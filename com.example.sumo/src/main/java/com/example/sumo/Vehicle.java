package com.example.sumo;

import java.util.Arrays;
import java.util.List;

public class Vehicle {

    private final String id;
    private final List<String> routeEdges;
    private int edgeIndex = 0;
    private double sOnEdge = 0.0;
    private double speed = 25.0;     // px/s (visual units)

    // metrics
    private double waitingTime = 0.0;
    private double startTime = 0.0;   // all start at t=0 in your setup
    private boolean finished = false;
    private double finishedAt = -1.0;

    public Vehicle(String id, String[] routeEdges) { this(id, Arrays.asList(routeEdges)); }
    public Vehicle(String id, List<String> routeEdges) {
        this.id = id;
        this.routeEdges = routeEdges;
    }

    public String getId() { return id; }
    public String getCurrentEdge() { return routeEdges.get(edgeIndex); }

    public void setSpeed(double s) { if (s > 0) this.speed = s; }
    public void addWaiting(double dt){ waitingTime += dt; }
    public double getWaitingTime(){ return waitingTime; }

    public void setStartTime(double t){ this.startTime = t; }
    public double getStartTime(){ return startTime; }

    public void markFinished(double simTime) { this.finished = true; this.finishedAt = simTime; }
    public boolean isFinished() { return finished; }
    public double getFinishedAt() { return finishedAt; }
    public double getTravelTime() { return finishedAt >= 0 ? (finishedAt - startTime) : 0.0; }

    /** Advance along current road with dt (seconds). */
    public void advance(double dt, Road currentRoad) {
        if (currentRoad == null) return;
        sOnEdge += speed * dt;
        double L = Math.max(1e-6, currentRoad.getLength());
        if (sOnEdge >= L && edgeIndex < routeEdges.size() - 1) {
            sOnEdge -= L;
            edgeIndex++;
        }
    }

    /** 0..1 progress along the given road (for drawing). */
    public double getProgressOn(Road r) {
        if (r == null) return 0.0;
        double L = Math.max(1e-6, r.getLength());
        double t = sOnEdge / L;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t;
    }

    /** Arrived at last edge end? */
    public boolean arrived() {
        return edgeIndex >= routeEdges.size() - 1 && sOnEdge >= Math.max(1.0, 0.99 * Math.max(1e-6, sOnEdge));
    }
    
 // --- pos. along current edge (for car-following) ---
    public double getSOnEdge() { return sOnEdge; }
    public void setSOnEdge(double s) { this.sOnEdge = Math.max(0, s); }
    // (opsionale) nëse të duhet shpejtësia e tanishme
    public double getSpeed() { return speed; }

}
