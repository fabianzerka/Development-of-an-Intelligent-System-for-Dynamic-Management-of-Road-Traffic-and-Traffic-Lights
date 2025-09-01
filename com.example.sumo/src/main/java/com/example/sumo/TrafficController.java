package com.example.sumo;

import java.util.List;
import java.util.Map;

public class TrafficController {
    private final Map<String, Road> roadById;

    // parametra të thjeshtë “vizualë”
    private double vehicleSpeed = 25.0;   // piksele/sek
    private final double STOP_DIST   = 18.0; // px para kryqëzimit
    private final double QUEUE_WIN   = 40.0; // px nga fundi i rrugës për të numëruar “në radhë”

    public TrafficController(Map<String, Road> roadById) {
        this.roadById = roadById;
    }

    public void setVehicleSpeed(double s) { if (s > 0) vehicleSpeed = s; }

    /** Tick i vetëm: përditëson semaforët, adaptimin dhe lëviz mjetet. */
    public void tick(double dt, List<Intersection> intersections, List<Vehicle> vehicles) {
        // 1) përditëso timer-in e semaforëve
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) {
                inter.getTrafficLights().get(0).update(dt);
            }
        }

        // 2) ADAPTIVE: mat radhët sipas afërsisë me kryqëzimin dhe përshtat fazën/greenDur
        for (Intersection inter : intersections) {
            if (inter.getTrafficLights().isEmpty()) continue;
            TrafficLight tl = inter.getTrafficLights().get(0);

            int[] q = new int[]{0, 0}; // q[0] dhe q[1] për grupet
            for (Road r : inter.getIncoming()) {
                int gi = tl.groupOf(r);
                if (gi < 0) continue;
                double L = r.getLength();
                for (Vehicle v : vehicles) {
                    if (!r.getId().equals(v.getCurrentEdge())) continue;
                    double t = v.getProgressOn(r); // 0..1
                    double distToEnd = (1.0 - t) * L;
                    if (distToEnd <= QUEUE_WIN) q[gi]++;
                }
            }
            // zgjidh phased fitues nëse diferenca është domethënëse
            int win = (q[0] >= q[1]) ? 0 : 1;
            if (Math.abs(q[0] - q[1]) >= 2 && tl.getPhase() != win) {
                tl.forcePhase(win); // kalim i menjëhershëm dhe reset i timer-it
            }
            // skalimi i greenDur me madhësinë e radhës (deri 30s)
            double newGreen = 6.0 + 1.5 * Math.max(q[0], q[1]);
            if (newGreen > 30.0) newGreen = 30.0;
            tl.setGreenDuration(newGreen);
        }

        // 3) lëviz mjetet; nëse e kuqe dhe afër, akumulo pritje
        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;
            v.setSpeed(vehicleSpeed);

            if (mustStopAtRed(v, r)) {
                v.addWaiting(dt);
                continue;
            }
            v.advance(dt, r);
        }
    }

    private boolean mustStopAtRed(Vehicle v, Road r) {
        Intersection end = r.getEndIntersection();
        if (end == null || end.getTrafficLights().isEmpty()) return false;
        TrafficLight tl = end.getTrafficLights().get(0);

        double L = r.getLength();
        double t = v.getProgressOn(r);
        double distToEnd = (1.0 - t) * L;

        return tl.isRedFor(r) && distToEnd < STOP_DIST;
    }
}
