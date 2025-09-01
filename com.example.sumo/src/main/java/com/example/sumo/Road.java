package com.example.sumo;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a directed road segment connecting two {@link Intersection}s.
 * A road stores simple start/end coordinates for quick rendering as well as an
 * optional polyline shape when detailed geometry is available from SUMO. The
 * class provides helpers for accessing geometric properties such as length and
 * shape points.
 */
public class Road {

    private final String id;
    private final Intersection from;
    private final Intersection to;

    // Original straight-line coordinates used for simple rendering
    private final int x1, y1, x2, y2;

    // Optional precise polyline (from lane/@shape attribute)
    private List<Point2D> shape = null;
    private double polyLength = -1.0;
    
    /**
     * Constructs a new road segment.
     *
     * @param id   unique edge identifier
     * @param from starting intersection
     * @param to   ending intersection
     * @param x1   start x coordinate
     * @param y1   start y coordinate
     * @param x2   end x coordinate
     * @param y2   end y coordinate
     */
    public Road(String id, Intersection from, Intersection to, int x1, int y1, int x2, int y2) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
    }

    /** @return edge identifier */
    public String getId() { return id; }

    /** @return starting intersection */
    public Intersection getFrom() { return from; }

    /** @return ending intersection */
    public Intersection getTo() { return to; }

    /** Alias for {@link #getTo()} used by some callers. */
    public Intersection getEndIntersection() { return to; }

    /** @return start x coordinate */
    public int getX1(){ return x1; }

    /** @return start y coordinate */
    public int getY1(){ return y1; }

    /** @return end x coordinate */
    public int getX2(){ return x2; }

    /** @return end y coordinate */
    public int getY2(){ return y2; }

    // ==== Shape & Length ====
    /**
     * Sets a detailed polyline describing the road geometry. When provided the
     * road length is computed from this shape instead of the straight line.
     */
    public void setShape(List<Point2D> pts) {
        if (pts != null && pts.size() >= 2) {
            this.shape = new ArrayList<>(pts);
            this.polyLength = -1.0; // recompute lazy
        }
    }

    /**
     * Returns the polyline representing this road. If no detailed shape was
     * supplied, a simple two-point line based on the start and end coordinates is
     * returned.
     */
    public List<Point2D> getShape() {
        if (shape != null) return shape;
        return Arrays.asList(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
    }

    /**
     * Computes the geometric length of the road, taking the polyline into
     * account when available. The value is cached after first computation.
     */
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