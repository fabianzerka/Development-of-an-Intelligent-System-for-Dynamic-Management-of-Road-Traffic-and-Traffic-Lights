package com.example.sumo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Swing component responsible for rendering the road network, vehicles and
 * traffic light states. The panel supports zooming and panning to explore the
 * simulation visually.
 */
public class TrafficVisualizer extends JPanel {

    private final List<Road> roads;
    private final List<Intersection> intersections;
    private final List<Vehicle> vehicles;
    private final Map<String, Road> roadById;
    private final TrafficController controller;

    // world bounds (computed from the network)
    private double minX, minY, maxX, maxY;

    // zoom & pan
    private double viewScale = 1.0;
    private double baseScale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double minScale, maxScale;
    private Point lastDrag = null;

    private static final int margin = 20;
    private static final int QUEUE_WIN = 40; // px near the stop line

    // ---- Dark theme palette for the map ----
    private static final Color MAP_BG        = new Color(0x1E222A);
    private static final Color ROAD_SHADOW   = new Color(0, 0, 0, 90);
    private static final Color ROAD_LINE     = new Color(140, 146, 160);
    private static final Color NODE_FILL     = new Color(35, 40, 50);
    private static final Color NODE_STROKE   = new Color(220, 225, 235, 110);
    private static final Color VEHICLE_FILL  = new Color(43, 179, 243);
    private static final Color QUEUE_BAR     = new Color(0, 209, 255, 180);
    private static final Color LIGHT_GREEN   = new Color(58, 197, 105);
    private static final Color LIGHT_RED     = new Color(232, 93, 117);
    private static final Color OVERLAY_TEXT  = new Color(230, 234, 242, 210);

    /** Creates a visualizer for the given network and controller. */
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

        setBackground(MAP_BG);                     // dark background
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

    /** Calculates world-space bounds based on roads and intersections. */
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

    /** Adjusts the view transform so that the entire network fits the panel. */
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

    /* ===================  INTERACTIONS  =================== */

    /** Wires up mouse listeners for zooming and panning interactions. */
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

    /** Zooms the view around the specified point. */
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

    /** Converts world coordinates to panel coordinates. */
    private Point worldToView(double x, double y) {
        int vx = (int)Math.round(offsetX + x * viewScale);
        int vy = (int)Math.round(offsetY + y * viewScale);
        return new Point(vx, vy);
    }

    /* ===================  DRAWING  =================== */

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ---- Roads (shadow + main stroke) ----
        for (Road r : roads) {
            java.util.List<java.awt.geom.Point2D> pts = r.getShape();
            if (pts.size() < 2) continue;

            // shadow pass
            g.setColor(ROAD_SHADOW);
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < pts.size() - 1; i++) {
                Point a = worldToView(pts.get(i).getX(),   pts.get(i).getY());
                Point b = worldToView(pts.get(i+1).getX(), pts.get(i+1).getY());
                g.drawLine(a.x, a.y, b.x, b.y);
            }
            // main pass
            g.setColor(ROAD_LINE);
            g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < pts.size() - 1; i++) {
                Point a = worldToView(pts.get(i).getX(),   pts.get(i).getY());
                Point b = worldToView(pts.get(i+1).getX(), pts.get(i+1).getY());
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        // ---- Intersections + small light indicator with glow ----
        for (Intersection inter : intersections) {
            Point p = worldToView(inter.getX(), inter.getY());

            // junction dot
            g.setColor(NODE_FILL);
            g.fillOval(p.x - 3, p.y - 3, 6, 6);
            g.setColor(NODE_STROKE);
            g.drawOval(p.x - 3, p.y - 3, 6, 6);

            // traffic light indicator (if any)
            if (!inter.getTrafficLights().isEmpty()) {
                boolean green = inter.getTrafficLights().get(0).getState() == TrafficLight.State.GREEN;
                Color c = green ? LIGHT_GREEN : LIGHT_RED;
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 90));   // glow
                g.fillOval(p.x - 7, p.y - 15, 14, 14);
                g.setColor(c);
                g.fillOval(p.x - 4, p.y - 12, 8, 8);
            }
        }

        // ---- Compute queue counts for this frame ----
        Map<String, Integer> qMap = new HashMap<>();
        for (Vehicle v : vehicles) {
            Road cr = roadById.get(v.getCurrentEdge());
            if (cr == null) continue;
            double L = cr.getLength();
            double distToEnd = (1.0 - v.getProgressOn(cr)) * L;
            if (distToEnd <= QUEUE_WIN) qMap.merge(cr.getId(), 1, Integer::sum);
        }

        // ---- Queue bars near stop lines (teal segments) ----
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(QUEUE_BAR);
        for (Intersection inter : intersections) {
            for (Road r : inter.getIncoming()) {
                int q = qMap.getOrDefault(r.getId(), 0);
                if (q <= 0) continue;

                double L = r.getLength();
                double len = Math.min(8 + q * 5.0, Math.max(24.0, 0.25 * L));
                java.awt.geom.Point2D pEnd = pointAlong(r, L);
                java.awt.geom.Point2D pBeg = pointAlong(r, Math.max(0, L - len));
                if (pEnd == null || pBeg == null) continue;

                Point a = worldToView(pBeg.getX(), pBeg.getY());
                Point b = worldToView(pEnd.getX(), pEnd.getY());
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }
        g.setStroke(new BasicStroke(2f));

        // ---- Vehicles (cyan with tiny shadow) ----
        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;
            double t = v.getProgressOn(r);
            java.awt.geom.Point2D wp = pointAlong(r, t * r.getLength());
            if (wp != null) {
                Point p = worldToView(wp.getX(), wp.getY());
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRect(p.x - 3 + 1, p.y - 3 + 1, 6, 6);
                g.setColor(VEHICLE_FILL);
                g.fillRect(p.x - 3, p.y - 3, 6, 6);
            }
        }

        // ---- Overlay (shadow + light text) ----
        String info = "Intersections: " + intersections.size() +
                      " | Roads: " + roads.size() +
                      " | Active: " + vehicles.size();
        if (controller != null) {
            info += " | Completed: " + controller.getCompletedCount() +
                    String.format(" | Avg wait(comp): %.1fs", controller.getAvgWaitCompleted());
        }
        int y = getHeight() - 10;
        g.setColor(new Color(0, 0, 0, 160));
        g.drawString(info, 11, y - 1);
        g.setColor(OVERLAY_TEXT);
        g.drawString(info, 10, y);

        g.dispose();
    }

    /** Returns a point along the polyline of road {@code r} at distance {@code s}. */
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
