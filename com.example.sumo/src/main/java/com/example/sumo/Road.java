package com.example.sumo;

import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.List;

public class Road {

    private final String id;
    private final Intersection from;
    private final Intersection to;
    private final int x1, y1, x2, y2;

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

    public int getX1(){ return x1; }
    public int getY1(){ return y1; }
    public int getX2(){ return x2; }
    public int getY2(){ return y2; }

    // alias-e të kërkuara nga kodi ekzistues
    public Intersection getEndIntersection() { return to; }

    public double getLength() {
        return Math.hypot(x2 - x1, y2 - y1);
    }

    // për vizualizim/projeksion
    public List<Point2D> getShape() {
        return Arrays.asList(new Point2D.Double(x1, y1), new Point2D.Double(x2, y2));
    }
}
