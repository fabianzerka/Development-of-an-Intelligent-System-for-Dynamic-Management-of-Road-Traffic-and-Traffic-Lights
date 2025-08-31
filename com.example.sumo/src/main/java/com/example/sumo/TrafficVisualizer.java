package com.example.sumo;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class TrafficVisualizer extends JPanel {

    private final List<Road> roads;
    private final List<Intersection> intersections;
    private final List<Vehicle> vehicles;
    private final Map<String, Road> roadById;

    // bounding box i botës (llogaritet një herë në konstruktor)
    private double minX, minY, maxX, maxY;
    private final int margin = 20;

    public TrafficVisualizer(List<Road> roads,
            List<Intersection> intersections,
            List<Vehicle> vehicles,
            Map<String,Road> roadById) {
		this.roads = roads;
		this.intersections = intersections;
		this.vehicles = vehicles;
		this.roadById = roadById;  // O(1) gjetja e rrugës
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

        // kryqëzimet + semaforët
        for (Intersection inter : intersections) {
            Point p = worldToView(inter.getX(), inter.getY());
            // pika e kryqëzimit
            g.setColor(Color.DARK_GRAY);
            g.fillOval(p.x - 3, p.y - 3, 6, 6);

            // semafori (nëse ekziston i pari)
            if (!inter.getTrafficLights().isEmpty()) {
                TrafficLight tl = inter.getTrafficLights().get(0);
                g.setColor(tl.getState() == TrafficLight.State.GREEN ? new Color(0,170,0) : new Color(200,40,40));
                g.fillOval(p.x - 6, p.y - 10, 8, 8);
            }
        }

        // makinat (gjej rrugën me O(1))
        g.setColor(new Color(30,144,255));
        for (Vehicle v : vehicles) {
            Road currentRoad = roadById.get(v.getCurrentEdge());
            if (currentRoad != null) {
                double t = v.getProgressOn(currentRoad); // 0..1
                double x = currentRoad.getX1() + t * (currentRoad.getX2() - currentRoad.getX1());
                double y = currentRoad.getY1() + t * (currentRoad.getY2() - currentRoad.getY1());
                Point p = worldToView(x, y);
                g.fillRect(p.x - 3, p.y - 3, 6, 6);
            }
        }

        // overlay informues
        g.setColor(new Color(0,0,0,140));
        String info = "Intersections: " + intersections.size() +
                      " | Roads: " + roads.size() +
                      " | Vehicles: " + vehicles.size();
        g.drawString(info, 10, getHeight() - 10);
    }
}
