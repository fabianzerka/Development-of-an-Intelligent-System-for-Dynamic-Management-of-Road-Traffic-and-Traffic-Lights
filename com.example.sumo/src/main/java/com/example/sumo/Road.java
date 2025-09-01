package com.example.sumo;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Road {

    private final String id;
    private final Intersection from;
    private final Intersection to;

    // Vjetër (kompatibilitet për vizatim të thjeshtë)
    private final int x1, y1, x2, y2;

    // RE: Poliline e saktë (nëse lexohet nga lane/@shape)
    private List<Point2D> shape = null;
    private double polyLength = -1.0;

    public Road(String id, Intersection from, Intersection to, int x1, int y1, int x2, int y2) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
    }

    public String getId() { return id; }
    public Intersection getFrom() { return from; }
    public Intersection getTo() { return to; }
    public Intersection getEndIntersection() { return to; }

    public int getX1(){ return x1; }
    public int getY1(){ return y1; }
    public int getX2(){ return x2; }
    public int getY2(){ return y2; }

    // ==== Shape & Length ====
    public void setShape(List<Point2D> pts) {
        if (pts != null && pts.size() >= 2) {
            this.shape = new ArrayList<>(pts);
            this.polyLength = -1.0; // recompute lazy
        }
    }

    public List<Point2D> getShape() {
        if (shape != null) return shape;
        return Arrays.asList(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
    }

    public double getLength() {
        if (shape == null) return Math.hypot(x2 - x1, y2 - y1);
        if (polyLength >= 0) return polyLength;
        double sum = 0.0;
        for (int i = 0; i < shape.size() - 1; i++) {
            sum += shape.get(i).distance(shape.get(i + 1));
        }
        polyLength = Math.max(sum, 1e-6);
        return polyLength;
    }
}
