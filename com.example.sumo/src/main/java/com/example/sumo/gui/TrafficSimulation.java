package com.example.sumo.gui;

import com.example.sumo.*;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;

public class TrafficSimulation {

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            String base   = "src/main/resources/sumo_output/";
            String net    = base + "osm.net.xml";
            String routes = base + "routes.rou.xml";

            // 1) Lexo rrjetin
            SumoNetworkReader.readNetwork(net);
            List<Intersection> intersections = new ArrayList<>(SumoNetworkReader.getIntersections());
            List<Road> roads = new ArrayList<>(SumoNetworkReader.getRoads());
            Map<String, Road> roadById = SumoNetworkReader.getRoadById();

            // 2) Krijo nga 1 semafor për çdo kryqëzim
            for (Intersection inter : intersections) {
                TrafficLight tl = new TrafficLight("TL_" + inter.getId(), inter);
                inter.addTrafficLight(tl);
            }

            // 3) Lexo mjetet
            List<Vehicle> vehicles = SumoVehiclesReader.readVehicles(routes);

            // Info në konsolë
            System.out.println("Intersections loaded = " + intersections.size());
            System.out.println("Roads loaded         = " + roads.size());
            System.out.println("Vehicles loaded      = " + vehicles.size());

            // 4) Controller + panel (me roadById për lookup O(1))
            TrafficController controller = new TrafficController(roadById);
            controller.setVehicleSpeed(25.0); // lëvizje më e dukshme

            TrafficVisualizer panel = new TrafficVisualizer(roads, intersections, vehicles, roadById);

            JFrame frame = new JFrame("Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // 5) Timer: përditëso semaforët + lëviz makinat + repaint
            final int dtMs = 40;
            new javax.swing.Timer(dtMs, e -> {
                controller.tick(dtMs / 1000.0, intersections, vehicles);
                panel.repaint();
            }).start();	
            
         // pas krijimit të controller dhe panelit
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

            // Slider për shpejtësinë (0..80 px/s)
            JLabel spLbl = new JLabel("Speed:");
            JSlider speed = new JSlider(0, 80, 25);
            speed.addChangeListener(e -> controller.setVehicleSpeed(speed.getValue()));
            controls.add(spLbl);
            controls.add(speed);

            // Spinner për green duration (2..30 s)
            JLabel gdLbl = new JLabel("  Green (s):");
            SpinnerNumberModel model = new SpinnerNumberModel(8.0, 2.0, 30.0, 0.5);
            JSpinner green = new JSpinner(model);
            green.addChangeListener(e -> {
                double val = ((Number)green.getValue()).doubleValue();
                for (Intersection inter : intersections) {
                    if (!inter.getTrafficLights().isEmpty()) {
                        inter.getTrafficLights().get(0).setGreenDuration(val);
                    }
                }
            });
            controls.add(gdLbl);
            controls.add(green);

            // shtoje panelin e kontrolleve në NORTH
            frame.add(controls, BorderLayout.NORTH);

        });
        
    }
}
