package com.example.sumo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrafficVisualizer extends JPanel {

    private final List<Road> roads;
    private final List<Intersection> intersections;
    private final List<Vehicle> vehicles;
    private final Map<String, Road> roadById;
    private final TrafficController controller;

    // kufijtë e botës (nga rrjeti)
    private double minX, minY, maxX, maxY;

    // zoom & pan
    private double viewScale = 1.0;
    private double baseScale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double minScale, maxScale;
    private Point lastDrag = null;

    private static final int margin = 20;
    private static final int QUEUE_WIN = 40; // px pranë semaforit për numërim radhe

    public TrafficVisualizer(List<Road> roads,
                             List<Intersection> intersections,
                             List<Vehicle> vehicles,
                             Map<String,Road> roadById,
                             TrafficController controller) {
        this.roads = roads;
        this.intersections = intersections;
        this.vehicles = vehicles;
        this.roadById = roadById;
        this.controller = controller;

        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1000, 700));
        setDoubleBuffered(true);

        computeBounds();
        fitToScreen();
        initInteractions();
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                fitToScreen();
                repaint();
            }
        });
    }

    /* ===================  BOUNDS & FIT  =================== */

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

    private void fitToScreen() {
        int W = Math.max(1, getWidth());
        int H = Math.max(1, getHeight());
        double sx = (W - 2.0 * margin) / (maxX - minX);
        double sy = (H - 2.0 * margin) / (maxY - minY);
        baseScale = Math.max(1e-6, Math.min(sx, sy));
        viewScale = baseScale;

        double contentW = (maxX - minX) * viewScale;
        double contentH = (maxY - minY) * viewScale;
        offsetX = (W - contentW) / 2.0 - minX * viewScale;
        offsetY = (H - contentH) / 2.0 - minY * viewScale;

        minScale = baseScale * 0.2;
        maxScale = baseScale * 20.0;
    }

    /* ===================  INTERAKSIONET  =================== */

    private void initInteractions() {
        addMouseWheelListener(e -> {
            double factor = (e.getWheelRotation() < 0) ? 1.1 : 1.0 / 1.1;
            zoomAt(e.getPoint(), factor);
        });

        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) lastDrag = e.getPoint();
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (lastDrag != null && SwingUtilities.isLeftMouseButton(e)) {
                    Point now = e.getPoint();
                    offsetX += now.x - lastDrag.x;
                    offsetY += now.y - lastDrag.y;
                    lastDrag = now;
                    repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent e) { lastDrag = null; }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { fitToScreen(); repaint(); }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    private void zoomAt(Point pivotView, double factor) {
        double newScale = clamp(viewScale * factor, minScale, maxScale);
        if (Math.abs(newScale - viewScale) < 1e-12) return;

        double wx = (pivotView.x - offsetX) / viewScale;
        double wy = (pivotView.y - offsetY) / viewScale;

        viewScale = newScale;
        offsetX = pivotView.x - wx * viewScale;
        offsetY = pivotView.y - wy * viewScale;

        repaint();
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private Point worldToView(double x, double y) {
        int vx = (int)Math.round(offsetX + x * viewScale);
        int vy = (int)Math.round(offsetY + y * viewScale);
        return new Point(vx, vy);
    }

    /* ===================  VIZATIMI  =================== */

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // rrugët (poliline)
        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(2f));
        for (Road r : roads) {
            java.util.List<java.awt.geom.Point2D> pts = r.getShape();
            for (int i = 0; i < pts.size() - 1; i++) {
                Point a = worldToView(pts.get(i).getX(),   pts.get(i).getY());
                Point b = worldToView(pts.get(i+1).getX(), pts.get(i+1).getY());
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        // indikatorë semafori
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) {
                TrafficLight tl = inter.getTrafficLights().get(0);
                Point p = worldToView(inter.getX(), inter.getY());
                g.setColor(tl.getState() == TrafficLight.State.GREEN ? new Color(0,170,0) : new Color(200,40,40));
                g.fillOval(p.x - 4, p.y - 12, 8, 8);
            }
        }

        // harta e radhëve (edgeId -> count) për këtë frame
        Map<String, Integer> qMap = new HashMap<>();
        for (Vehicle v : vehicles) {
            Road cr = roadById.get(v.getCurrentEdge());
            if (cr == null) continue;
            double L = cr.getLength();
            double distToEnd = (1.0 - v.getProgressOn(cr)) * L;
            if (distToEnd <= QUEUE_WIN) qMap.merge(cr.getId(), 1, Integer::sum);
        }

        // shiritat e kuq të radhëve
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(220, 50, 50, 160));
        for (Intersection inter : intersections) {
            for (Road r : inter.getIncoming()) {
                int q = qMap.getOrDefault(r.getId(), 0);
                if (q <= 0) continue;

                double L = r.getLength();
                double len = Math.min(8 + q * 5.0, Math.max(20.0, 0.25 * L));
                java.awt.geom.Point2D pEnd = pointAlong(r, L);
                java.awt.geom.Point2D pBeg = pointAlong(r, Math.max(0, L - len));

                Point a = worldToView(pBeg.getX(), pBeg.getY());
                Point b = worldToView(pEnd.getX(), pEnd.getY());
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }
        g.setStroke(new BasicStroke(2f));

        // makinat
        g.setColor(new Color(30,144,255));
        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;
            double t = v.getProgressOn(r);
            java.awt.geom.Point2D wp = pointAlong(r, t * r.getLength());
            if (wp != null) {
                Point p = worldToView(wp.getX(), wp.getY());
                g.fillRect(p.x - 3, p.y - 3, 6, 6);
            }
        }

        // overlay
        g.setColor(new Color(0,0,0,140));
        String info = "Intersections: " + intersections.size() +
                      " | Roads: " + roads.size() +
                      " | Active: " + vehicles.size();
        if (controller != null) {
            info += " | Completed: " + controller.getCompletedCount() +
                    String.format(" | Avg wait(comp): %.1fs", controller.getAvgWaitCompleted());
        }
        g.drawString(info, 10, getHeight() - 10);
    }

    // pikë përgjatë polilines së rrugës në distancën s (nga fillimi)
    private java.awt.geom.Point2D pointAlong(Road r, double s) {
        java.util.List<java.awt.geom.Point2D> pts = r.getShape();
        double remain = s;
        for (int i = 0; i < pts.size() - 1; i++) {
            java.awt.geom.Point2D a = pts.get(i), b = pts.get(i+1);
            double seg = a.distance(b);
            if (remain <= seg) {
                double u = seg < 1e-9 ? 0 : remain / seg;
                double x = a.getX() + u * (b.getX() - a.getX());
                double y = a.getY() + u * (b.getY() - a.getY());
                return new java.awt.geom.Point2D.Double(x, y);
            }
            remain -= seg;
        }
        return pts.isEmpty() ? null : pts.get(pts.size()-1);
    }
}
