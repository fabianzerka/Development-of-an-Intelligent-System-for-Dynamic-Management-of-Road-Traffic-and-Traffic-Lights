package com.example.sumo.gui;

import com.example.sumo.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Entry point for the Swing-based traffic simulation demo.
 * Wires readers, controller, visualizer, and a modern dark toolbar.
 */
public class TrafficSimulation {

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            // --- Dark UI defaults (UiKit) ---
            UiKit.applyDarkDefaults();

            String base   = "src/main/resources/sumo_output/";
            String net    = base + "osm.net.xml";
            String routes = base + "routes.rou.xml";

            // 1) Network
            SumoNetworkReader.readNetwork(net);
            java.util.List<Intersection> intersections = new ArrayList<>(SumoNetworkReader.getIntersections());
            java.util.List<Road> roads = new ArrayList<>(SumoNetworkReader.getRoads());
            Map<String, Road> roadById = SumoNetworkReader.getRoadById();

            // 2) Lights
            for (Intersection inter : intersections) {
                TrafficLight tl = new TrafficLight("TL_" + inter.getId(), inter);
                inter.addTrafficLight(tl);
            }

            // 3) Vehicles
            java.util.List<Vehicle> vehicles = SumoVehiclesReader.readVehicles(routes);
            for (Vehicle v : vehicles) v.setStartTime(0.0);

            // 4) Controller + panel
            TrafficController controller = new TrafficController(roadById);
            controller.setVehicleSpeed(25.0);
            TrafficVisualizer panel = new TrafficVisualizer(roads, intersections, vehicles, roadById, controller);
            // Optional: dark map
            // panel.setBackground(new Color(0x1E222A));

            // 5) Frame
            JFrame frame = new JFrame("Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            // ----- Modern top bar (UiKit) -----
            JComponent top = UiKit.topBar();
            JPanel left  = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
            left.setOpaque(false);
            right.setOpaque(false);
            top.add(left, BorderLayout.WEST);
            top.add(right, BorderLayout.EAST);

            // Play/Pause button
            JButton playPause = UiKit.button("PAUSE", UiKit.pauseIcon(16), UiKit.BtnKind.PRIMARY);
            left.add(playPause);

            // Speed slider + badge
            left.add(new JLabel("SIMULATION SPEED:") {{ setForeground(UiKit.FG_MUTED); }});
            JSlider speed = new JSlider(0, 80, 25);
            speed.setOpaque(false);
            JLabel speedBadge = UiKit.badge("1.0x", UiKit.ACCENT_DARK);
            left.add(speed);
            left.add(speedBadge);

            // Green spinner + badge
            left.add(new JLabel(UiKit.lightIcon(16)));
            JSpinner green = new JSpinner(new SpinnerNumberModel(8.0, 2.0, 30.0, 0.5));
            ((JComponent) ((JSpinner.DefaultEditor) green.getEditor()).getComponent(0)).setOpaque(true);
            left.add(green);
            JLabel greenBadge = UiKit.badge("GREEN: 8", new Color(0x3A3F4B));
            left.add(greenBadge);

            // Export button
            JButton exportCsv = UiKit.button("EXPORT DATA (CSV)", UiKit.downloadIcon(16), UiKit.BtnKind.GHOST);
            right.add(exportCsv);

            frame.add(top, BorderLayout.NORTH);

            // ----- Timer & wiring -----
            final int dtMs = 40;
            final javax.swing.Timer simTimer = new javax.swing.Timer(dtMs, e -> {
                controller.tick(dtMs / 1000.0, intersections, vehicles);
                panel.repaint();
            });
            simTimer.start();

            // Play / Pause toggle
            playPause.addActionListener(ae -> {
                if (simTimer.isRunning()) {
                    simTimer.stop();
                    playPause.setText("PLAY");
                    playPause.setIcon(UiKit.playIcon(16));
                } else {
                    simTimer.start();
                    playPause.setText("PAUSE");
                    playPause.setIcon(UiKit.pauseIcon(16));
                }
            });

            // Speed slider → controller speed + badge
            speed.addChangeListener(e -> {
                int v = speed.getValue();
                controller.setVehicleSpeed(v);
                double factor = Math.max(0.25, v / 25.0);
                speedBadge.setText(String.format("%.1fx", factor));
            });

            // Green duration → all lights + badge
            green.addChangeListener(e -> {
                double val = ((Number) green.getValue()).doubleValue();
                for (Intersection inter : intersections) {
                    if (!inter.getTrafficLights().isEmpty()) {
                        inter.getTrafficLights().get(0).setGreenDuration(val);
                    }
                }
                greenBadge.setText("GREEN: " + (int)Math.round(val));
            });

            // Export CSV
            exportCsv.addActionListener(ae -> {
                try {
                    Path out = Paths.get("results", "metrics.csv");
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
