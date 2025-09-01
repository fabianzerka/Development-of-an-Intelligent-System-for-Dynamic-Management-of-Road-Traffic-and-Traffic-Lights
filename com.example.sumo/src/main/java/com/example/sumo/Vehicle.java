package com.example.sumo;

import java.util.Arrays;
import java.util.List;

public class Vehicle {

    private final String id;
    private final List<String> routeEdges;
    private int edgeIndex = 0;     // cili edge është aktiv
    private double sOnEdge = 0.0;  // pozicioni (në piksele të hartës) përgjatë edge-it
    private double speed = 20.0;   // piksele / sek
    private final double depart;
    private boolean finished = false;
    private double finishedAt = -1;
    
    // metrikë
    private double waitingTime = 0.0;

    public Vehicle(String id, List<String> routeEdges, double depart) {
        this.id = id; this.routeEdges = routeEdges; this.depart = Math.max(0, depart);
    }

    // Konstruktori ekzistues të thërrasë këtë:
    public Vehicle(String id, List<String> routeEdges) { this(id, routeEdges, 0.0); }
    public Vehicle(String id, String[] routeEdges) { this(id, Arrays.asList(routeEdges), 0.0); }

	public double getDepart() { return depart; }
	public boolean isActive(double simTime) { return simTime >= depart; }

    public String getId() { return id; }
    public String getCurrentEdge() { return routeEdges.get(edgeIndex); }

    public void setSpeed(double s) { if (s > 0) this.speed = s; }

    /** Lëviz me dt në rrugën aktuale; kalon në tjetrën kur mbaron. */
    public void advance(double dt, Road currentRoad) {
        if (currentRoad == null) return;
        sOnEdge += speed * dt;
        double L = Math.max(1e-6, currentRoad.getLength());
        if (sOnEdge >= L && edgeIndex < routeEdges.size() - 1) {
            sOnEdge -= L;
            edgeIndex++;
        }
    }

    /** Progres 0..1 për vizatim në rrugën e dhënë. */
    public double getProgressOn(Road r) {
        if (r == null) return 0.0;
        double L = Math.max(1e-6, r.getLength());
        double t = sOnEdge / L;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t;
    }

    public boolean arrived() {
        return edgeIndex >= routeEdges.size() - 1 && sOnEdge <= 1.0;
    }

    // ==== metrikë ====
    public void addWaiting(double dt) { waitingTime += dt; }
    public double getWaitingTime() { return waitingTime; }
    public void markFinished(double simTime) { this.finished = true; this.finishedAt = simTime; }
    public boolean isFinished() { return finished; }
    public double getFinishedAt() { return finishedAt; }
}
