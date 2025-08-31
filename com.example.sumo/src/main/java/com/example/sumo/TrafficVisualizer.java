package com.example.sumo;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TrafficVisualizer extends JPanel {

    private final List<Road> roads;
    private final List<Intersection> intersections;
    private final List<Vehicle> vehicles;

    // bounding box i botës (llogaritet një herë në konstruktor)
    private double minX, minY, maxX, maxY;
    private final int margin = 20;

    public TrafficVisualizer(List<Road> roads, List<Intersection> intersections,
                             List<Vehicle> vehicles) {
        this.roads = roads;
        this.intersections = intersections;
        this.vehicles = vehicles;

        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1000, 700));

        computeBounds();
    }

    private void computeBounds() {
        if (roads.isEmpty() && intersections.isEmpty()) {
            minX = minY = 0; maxX = maxY = 1;
            return;
        }
        minX = Double.POSITIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;

        for (Road r : roads) {
            minX = Math.min(minX, Math.min(r.getX1(), r.getX2()));
            minY = Math.min(minY, Math.min(r.getY1(), r.getY2()));
            maxX = Math.max(maxX, Math.max(r.getX1(), r.getX2()));
            maxY = Math.max(maxY, Math.max(r.getY1(), r.getY2()));
        }
        for (Intersection inter : intersections) {
            minX = Math.min(minX, inter.getX());
            minY = Math.min(minY, inter.getY());
            maxX = Math.max(maxX, inter.getX());
            maxY = Math.max(maxY, inter.getY());
        }
        if (minX == maxX) maxX = minX + 1;
        if (minY == maxY) maxY = minY + 1;
    }

    private Point worldToView(double x, double y) {
        int W = Math.max(1, getWidth()  - 2*margin);
        int H = Math.max(1, getHeight() - 2*margin);
        double sx = W / (maxX - minX);
        double sy = H / (maxY - minY);
        double s  = Math.min(sx, sy); // ruaj raportet

        // opsionale: invertim i boshtit Y nëse do orientim "grafik"
        double vx = margin + (x - minX) * s;
        double vy = margin + (y - minY) * s;

        return new Point((int)Math.round(vx), (int)Math.round(vy));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // rrugët
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(2f));
        for (Road r : roads) {
            Point a = worldToView(r.getX1(), r.getY1());
            Point b = worldToView(r.getX2(), r.getY2());
            g.drawLine(a.x, a.y, b.x, b.y);
        }

        // kryqëzimet
        g.setColor(Color.DARK_GRAY);
        for (Intersection inter : intersections) {
            Point p = worldToView(inter.getX(), inter.getY());
            g.fillOval(p.x - 3, p.y - 3, 6, 6);
        }

        // makinat (në edge-in aktual)
        g.setColor(new Color(30,144,255));
        for (Vehicle v : vehicles) {
            Road currentRoad = null;
            for (Road r : roads) {
                if (r.getId().equals(v.getCurrentEdge())) { currentRoad = r; break; }
            }
            if (currentRoad != null) {
                double t = Math.max(0, Math.min(1, v.getProgress()));
                double x = currentRoad.getX1() + t * (currentRoad.getX2() - currentRoad.getX1());
                double y = currentRoad.getY1() + t * (currentRoad.getY2() - currentRoad.getY1());
                Point p = worldToView(x, y);
                g.fillRect(p.x - 3, p.y - 3, 6, 6);
            }
        }

        // overlay i thjeshtë informativ
        g.setColor(new Color(0,0,0,140));
        String info = "Intersections: " + intersections.size() +
                      " | Roads: " + roads.size() +
                      " | Vehicles: " + vehicles.size();
        g.drawString(info, 10, getHeight() - 10);
    }
}
