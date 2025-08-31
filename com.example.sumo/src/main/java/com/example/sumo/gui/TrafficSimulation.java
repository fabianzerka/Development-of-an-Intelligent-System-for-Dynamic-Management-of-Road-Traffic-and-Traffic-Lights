package com.example.sumo.gui;

import com.example.sumo.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class TrafficSimulation {

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            String base = "src/main/resources/sumo_output/";
            String net = base + "osm.net.xml";
            String routes = base + "routes.rou.xml";
            String edgeData = base + "edgeData.xml";

            // 1) rrjeti
            SumoNetworkReader.readNetwork(net);
            java.util.List<Intersection> intersections = new ArrayList<>(SumoNetworkReader.getIntersections());
            java.util.List<Road> roads = new ArrayList<>(SumoNetworkReader.getRoads());

            // 2) semaforët (1 për kryqëzim)
            for (Intersection inter : intersections) {
                TrafficLight tl = new TrafficLight("TL_" + inter.getId(), inter);
                inter.addTrafficLight(tl);
            }

            // 3) mjetet
            java.util.List<Vehicle> vehicles = SumoVehiclesReader.readVehicles(routes);

            // 4) controller + vizualizimi
            TrafficController controller = new TrafficController();
            TrafficVisualizer panel = new TrafficVisualizer(roads, intersections, vehicles);

            JFrame frame = new JFrame("Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // 5) timer: përditëso semaforët + lëviz makinat + repaint
            final int dtMs = 40;
            new javax.swing.Timer(dtMs, e -> {
                // flows (nëse skedari mungon, thjesht bëhet hartë bosh)
                Map<String,Integer> flowsByEdge = SumoReader.readTrafficFlows(edgeData);

                // kthe në Map<TrafficLight, Integer>
                Map<TrafficLight,Integer> measured = new HashMap<>();
                for (Intersection inter : intersections) {
                    for (TrafficLight tl : inter.getTrafficLights()) {
                        int f = 0;
                        // shembull: përdor flow të rrugës së parë hyrëse (nëse ka)
                        if (!inter.getIncoming().isEmpty()) {
                            String edgeId = inter.getIncoming().get(0).getId();
                            f = flowsByEdge.getOrDefault(edgeId, 1); // 1 default që të jetë jeshile
                        } else {
                            f = 1;
                        }
                        measured.put(tl, f);
                    }
                }

                controller.updateTraffic(measured, intersections);
                controller.stepVehicles(vehicles);
                panel.repaint();
            }).start();
        });
    }
}
