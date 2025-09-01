package com.example.sumo.gui;

import com.example.sumo.*;

import javax.swing.*;
import javax.swing.Timer;

import java.awt.*;
import java.nio.file.Path;
import java.util.*;

public class TrafficSimulation {

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            String base   = "src/main/resources/sumo_output/";
            String net    = base + "osm.net.xml";
            String routes = base + "routes.rou.xml";

            // network
            SumoNetworkReader.readNetwork(net);
            java.util.List<Intersection> intersections = new ArrayList<>(SumoNetworkReader.getIntersections());
            java.util.List<Road> roads = new ArrayList<>(SumoNetworkReader.getRoads());
            Map<String, Road> roadById = SumoNetworkReader.getRoadById();

            // lights (1 per intersection)
            for (Intersection inter : intersections) {
                TrafficLight tl = new TrafficLight("TL_" + inter.getId(), inter);
                inter.addTrafficLight(tl);
            }

            // vehicles
            java.util.List<Vehicle> vehicles = SumoVehiclesReader.readVehicles(routes);
            // (optional) set start times 0
            for (Vehicle v : vehicles) v.setStartTime(0.0);

            // controller + panel
            TrafficController controller = new TrafficController(roadById);
            controller.setVehicleSpeed(25.0);

            TrafficVisualizer panel = new TrafficVisualizer(roads, intersections, vehicles, roadById, controller);

            JFrame frame = new JFrame("Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            // --- Controls bar ---
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controls.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

            JButton playPause = new JButton("Pause");
            JSlider speed = new JSlider(0, 80, 25);
            JLabel speedLbl = new JLabel(" Speed:");
            JLabel greenLbl = new JLabel("  Green (s):");
            JSpinner green = new JSpinner(new SpinnerNumberModel(8.0, 2.0, 30.0, 0.5));
            JButton exportCsv = new JButton("Export CSV");

            controls.add(playPause);
            controls.add(speedLbl);
            controls.add(speed);
            controls.add(greenLbl);
            controls.add(green);
            controls.add(exportCsv);

            frame.add(controls, BorderLayout.NORTH);

            // --- Timer & wiring ---
            final int dtMs = 40;
            final Timer[] holder = new Timer[1];
            holder[0] = new javax.swing.Timer(dtMs, e -> {
                controller.tick(dtMs / 1000.0, intersections, vehicles);
                panel.repaint();
            });
            holder[0].start();

            // play/pause
            playPause.addActionListener(ae -> {
                if (holder[0].isRunning()) { holder[0].stop(); playPause.setText("Play"); }
                else { holder[0].start(); playPause.setText("Pause"); }
            });

            // speed slider
            speed.addChangeListener(e -> controller.setVehicleSpeed(speed.getValue()));

            // green duration spinner
            green.addChangeListener(e -> {
                double val = ((Number)green.getValue()).doubleValue();
                for (Intersection inter : intersections) {
                    if (!inter.getTrafficLights().isEmpty()) {
                        inter.getTrafficLights().get(0).setGreenDuration(val);
                    }
                }
            });

            // export CSV
            exportCsv.addActionListener(ae -> {
                try {
                    Path out = Path.of("results", "metrics.csv");
                    CsvExporter.exportVehicleMetrics(out, controller.getFinishedVehiclesSnapshot());
                    JOptionPane.showMessageDialog(frame,
                            "Metrics exported to:\n" + out.toAbsolutePath(),
                            "Export CSV", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame,
                            "Export failed: " + ex.getMessage(),
                            "Export CSV", JOptionPane.ERROR_MESSAGE);
                }
            });

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
