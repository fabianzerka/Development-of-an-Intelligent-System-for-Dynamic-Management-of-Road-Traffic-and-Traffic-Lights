package com.example.sumo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class HeadlessRunner {

    public static void main(String[] args) throws Exception {
        Config cfg = parseArgs(args);

        String base   = cfg.base;
        String net    = base + "/osm.net.xml";
        String routes = base + "/routes.rou.xml";

        // 1) Rrjeti
        SumoNetworkReader.readNetwork(net);
        List<Intersection> intersections = new ArrayList<>(SumoNetworkReader.getIntersections());
        Map<String, Road> roadById = SumoNetworkReader.getRoadById();

        // 2) Semaforët
        for (Intersection inter : intersections) {
            TrafficLight tl = new TrafficLight("TL_" + inter.getId(), inter);
            tl.setGreenDuration(cfg.green);
            inter.addTrafficLight(tl);
        }

        // 3) Mjetet
        List<Vehicle> vehicles = SumoVehiclesReader.readVehicles(routes);
        for (Vehicle v : vehicles) v.setStartTime(0.0);

        // 4) Simulimi
        TrafficController controller = new TrafficController(roadById);
        controller.setVehicleSpeed(cfg.speed);

        double t = 0.0, dt = cfg.dt;
        long steps = (long)Math.ceil(cfg.duration / dt);
        for (long i = 0; i < steps; i++) {
            controller.tick(dt, intersections, vehicles);
            t += dt;
            if (vehicles.isEmpty()) break; // të gjitha mbaruan
        }

        // 5) Export CSV + përmbledhje
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Path out = Paths.get("results", "metrics_" + stamp + ".csv");
        CsvExporter.exportVehicleMetrics(out, controller.getFinishedVehiclesSnapshot());

        int completed = controller.getCompletedCount();
        double avgWait = controller.getAvgWaitCompleted();
        double avgStops = controller.getFinishedVehiclesSnapshot().stream().mapToInt(Vehicle::getStops).average().orElse(0);
        double avgTravel = controller.getFinishedVehiclesSnapshot().stream().mapToDouble(Vehicle::getTravelTime).average().orElse(0);

        System.out.printf(Locale.US, "DONE. Completed=%d | AvgWait=%.2fs | AvgStops=%.2f | AvgTravel=%.2fs%n",
                completed, avgWait, avgStops, avgTravel);
        System.out.println("CSV: " + out.toAbsolutePath());
    }

    /* -------- args -------- */
    static class Config {
        String base = "src/main/resources/sumo_output"; // path i dossier-it me osm.net.xml & routes.rou.xml
        double duration = 120.0;  // sekonda
        double green = 8.0;       // sekonda
        double speed = 25.0;      // px/s (vizual)
        double dt = 0.04;         // sek
    }
    static Config parseArgs(String[] args) {
        Config c = new Config();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--base":     c.base = args[++i]; break;
                case "--duration": c.duration = Double.parseDouble(args[++i]); break;
                case "--green":    c.green = Double.parseDouble(args[++i]); break;
                case "--speed":    c.speed = Double.parseDouble(args[++i]); break;
                case "--dt":       c.dt = Double.parseDouble(args[++i]); break;
            }
        }
        return c;
    }
}
