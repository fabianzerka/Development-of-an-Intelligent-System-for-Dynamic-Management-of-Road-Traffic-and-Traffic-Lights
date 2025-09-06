package com.example.sumo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Renders roads, vehicles and small red/green dots at intersections. */
public class TrafficVisualizer extends JPanel {

    private final List<Road> roads;
    private final List<Intersection> intersections;
    private final List<Vehicle> vehicles;
    private final Map<String, Road> roadById;
    private final TrafficController controller;

    // world bounds
    private double minX, minY, maxX, maxY;

    // zoom & pan
    private double viewScale = 1.0, baseScale = 1.0, offsetX = 0.0, offsetY = 0.0;
    private double minScale, maxScale;
    private Point lastDrag = null;

    private static final int margin = 20;
    private static final int QUEUE_WIN = 40;

    public TrafficVisualizer(List<Road> roads, List<Intersection> intersections,
                             List<Vehicle> vehicles, Map<String,Road> roadById,
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
            @Override public void componentResized(ComponentEvent e) { fitToScreen(); repaint(); }
        });
    }

    /* ---------- bounds & view ---------- */

    private void computeBounds() {
        if (roads.isEmpty() && intersections.isEmpty()) { minX=minY=0; maxX=maxY=1; return; }
        minX=Double.POSITIVE_INFINITY; minY=Double.POSITIVE_INFINITY;
        maxX=Double.NEGATIVE_INFINITY; maxY=Double.NEGATIVE_INFINITY;
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
        if (minX==maxX) maxX=minX+1;
        if (minY==maxY) maxY=minY+1;
    }

    private void fitToScreen() {
        int W = Math.max(1, getWidth()), H = Math.max(1, getHeight());
        double sx = (W - 2.0*margin) / (maxX - minX);
        double sy = (H - 2.0*margin) / (maxY - minY);
        baseScale = Math.max(1e-6, Math.min(sx, sy));
        viewScale = baseScale;

        double contentW = (maxX - minX) * viewScale;
        double contentH = (maxY - minY) * viewScale;
        offsetX = (W - contentW) / 2.0 - minX * viewScale;
        offsetY = (H - contentH) / 2.0 - minY * viewScale;

        minScale = baseScale * 0.2;
        maxScale = baseScale * 20.0;
    }

    private void initInteractions() {
        addMouseWheelListener(e -> zoomAt(e.getPoint(), e.getWheelRotation() < 0 ? 1.1 : 1.0/1.1));
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed (MouseEvent e){ if (SwingUtilities.isLeftMouseButton(e)) lastDrag = e.getPoint(); }
            @Override public void mouseDragged (MouseEvent e){ if (lastDrag!=null && SwingUtilities.isLeftMouseButton(e)){ Point n=e.getPoint(); offsetX+=n.x-lastDrag.x; offsetY+=n.y-lastDrag.y; lastDrag=n; repaint(); } }
            @Override public void mouseReleased(MouseEvent e){ lastDrag=null; }
            @Override public void mouseClicked (MouseEvent e){ if (e.getClickCount()==2){ fitToScreen(); repaint(); } }
        };
        addMouseListener(ma); addMouseMotionListener(ma);
    }

    private void zoomAt(Point pv, double factor) {
        double ns = Math.max(minScale, Math.min(maxScale, viewScale * factor));
        if (Math.abs(ns - viewScale) < 1e-12) return;
        double wx = (pv.x - offsetX) / viewScale;
        double wy = (pv.y - offsetY) / viewScale;
        viewScale = ns;
        offsetX = pv.x - wx * viewScale;
        offsetY = pv.y - wy * viewScale;
        repaint();
    }

    private Point worldToView(double x, double y) {
        return new Point((int)Math.round(offsetX + x * viewScale),
                         (int)Math.round(offsetY + y * viewScale));
    }

    /* ---------- drawing ---------- */

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // roads
        g.setColor(new Color(210, 213, 219));
        g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Road r : roads) {
            java.util.List<java.awt.geom.Point2D> pts = r.getShape();
            for (int i = 0; i < pts.size() - 1; i++) {
                Point a = worldToView(pts.get(i).getX(), pts.get(i).getY());
                Point b = worldToView(pts.get(i+1).getX(), pts.get(i+1).getY());
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        // traffic light indicator at each intersection (single small dot)
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) {
                TrafficLight tl = inter.getTrafficLights().get(0);
                Point p = worldToView(inter.getX(), inter.getY());
                g.setColor(tl.getState() == TrafficLight.State.GREEN ? new Color(0,170,0) : new Color(200,40,40));
                g.fillOval(p.x - 4, p.y - 12, 8, 8);
                g.setColor(new Color(80,80,80));
                g.fillOval(p.x - 3, p.y - 3, 6, 6); // node dot
            }
        }

        // queue bars (light teal) near stop-lines
        Map<String,Integer> qMap = new HashMap<>();
        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;
            double L = r.getLength();
            double distToEnd = (1.0 - v.getProgressOn(r)) * L;
            if (distToEnd <= QUEUE_WIN) qMap.merge(r.getId(), 1, Integer::sum);
        }
        g.setColor(new Color(0, 160, 200, 170));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Intersection inter : intersections) {
            for (Road r : inter.getIncoming()) {
                int q = qMap.getOrDefault(r.getId(), 0);
                if (q <= 0) continue;
                double L = r.getLength();
                double len = Math.min(8 + q * 5.0, Math.max(24.0, 0.25 * L));
                java.awt.geom.Point2D pEnd = pointAlong(r, L);
                java.awt.geom.Point2D pBeg = pointAlong(r, Math.max(0, L - len));
                Point a = worldToView(pBeg.getX(), pBeg.getY());
                Point b = worldToView(pEnd.getX(), pEnd.getY());
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }
        g.setStroke(new BasicStroke(2f));

        // vehicles
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
        g.setColor(new Color(0,0,0,160));
        String info = "Intersections: " + intersections.size() +
                      " | Roads: " + roads.size() +
                      " | Active: " + vehicles.size();
        if (controller != null) {
            info += " | Completed: " + controller.getCompletedCount() +
                    String.format(" | Avg wait(comp): %.1fs", controller.getAvgWaitCompleted());
        }
        g.drawString(info, 10, getHeight() - 10);
        g.dispose();
    }

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
