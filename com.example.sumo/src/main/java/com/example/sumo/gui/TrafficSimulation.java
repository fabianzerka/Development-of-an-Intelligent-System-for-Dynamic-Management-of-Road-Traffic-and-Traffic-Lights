package com.example.sumo.gui;

import com.example.sumo.*;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/** Launches the Swing traffic simulation. */
public class TrafficSimulation {

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> {
            // Keep the dark toolbar; map itself will be white
            UiKit.applyDarkDefaults();

            String base   = "src/main/resources/sumo_output/";
            String net    = base + "osm.net.xml";
//            String routes = base + "routes.rou.xml";
            String routes = base + "lowDensity_routes.rou.xml";
//            String routes = base + "routes_high.rou.xml";

            // Network
            SumoNetworkReader.readNetwork(net);
            java.util.List<Intersection> intersections = new ArrayList<>(SumoNetworkReader.getIntersections());
            java.util.List<Road> roads = new ArrayList<>(SumoNetworkReader.getRoads());
            Map<String, Road> roadById = SumoNetworkReader.getRoadById();

            // Lights (1 per intersection)
            for (Intersection inter : intersections) {
                TrafficLight tl = new TrafficLight("TL_" + inter.getId(), inter);
                inter.addTrafficLight(tl);
            }

            // Vehicles
            java.util.List<Vehicle> vehicles = SumoVehiclesReader.readVehicles(routes);
            for (Vehicle v : vehicles) v.setStartTime(0.0);

            // Controller + panel
            TrafficController controller = new TrafficController(roadById);
            controller.setVehicleSpeed(25.0);
            TrafficVisualizer panel = new TrafficVisualizer(roads, intersections, vehicles, roadById, controller);
            panel.setBackground(Color.WHITE); // white map

            JFrame frame = new JFrame("Traffic Simulation");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.add(panel, BorderLayout.CENTER);

            // ---- Top bar (no GREEN controls) ----
            JComponent top = UiKit.topBar();
            JPanel left  = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
            left.setOpaque(false); right.setOpaque(false);
            top.add(left, BorderLayout.WEST);
            top.add(right, BorderLayout.EAST);

            JButton playPause = UiKit.button("PAUSE", UiKit.pauseIcon(16), UiKit.BtnKind.PRIMARY);
            left.add(playPause);

            left.add(new JLabel("SIMULATION SPEED:"){{
                setForeground(UiKit.FG_MAIN);
            }});
            JSlider speed = new JSlider(0, 80, 25);
            speed.setOpaque(false);
            left.add(speed);
            JLabel speedBadge = UiKit.badge("1.0x", UiKit.ACCENT_DARK);
            left.add(speedBadge);

            JButton exportCsv = UiKit.button("EXPORT DATA (CSV)", UiKit.downloadIcon(16), UiKit.BtnKind.GHOST);
            right.add(exportCsv);

            frame.add(top, BorderLayout.NORTH);

            // ---- Timer & wiring ----
            final int dtMs = 40;
            final javax.swing.Timer simTimer = new javax.swing.Timer(dtMs, e -> {
                controller.tick(dtMs / 1000.0, intersections, vehicles);
                panel.repaint();
            });
            simTimer.start();

            // Play/Pause
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

            // Speed slider
            speed.addChangeListener(e -> {
                int v = speed.getValue();
                controller.setVehicleSpeed(v);
                double factor = Math.max(0.25, v / 25.0);
                speedBadge.setText(String.format("%.1fx", factor));
            });

            // Export CSV
            exportCsv.addActionListener(ae -> {
                try {
                    Path out = Paths.get("results", "FixedTimeLowDensity_metrics.csv");
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
