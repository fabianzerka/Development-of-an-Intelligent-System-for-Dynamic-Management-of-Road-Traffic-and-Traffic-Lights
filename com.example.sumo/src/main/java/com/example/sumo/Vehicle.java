package com.example.sumo;

import java.util.Arrays;
import java.util.List;

public class Vehicle {

    private final String id;
    private final List<String> routeEdges;
    private int edgeIndex = 0;     // cili edge në radhë
    private double sOnEdge = 0.0;  // pozicioni në edge (në të njëjtat njësi si koordinatat)
    private double speed = 12.0;   // “pixel/s” (rrite/ul për efekt vizual)

    public Vehicle(String id, String[] routeEdges) { this(id, Arrays.asList(routeEdges)); }
    public Vehicle(String id, List<String> routeEdges) { this.id = id; this.routeEdges = routeEdges; }

    public String getId() { return id; }
    public String getCurrentEdge() { return routeEdges.get(edgeIndex); }
    public int getEdgeIndex() { return edgeIndex; }
    public double getProgress() { return 0.0; } // NUK përdoret më, lëre për kompatibilitet
    public void setSpeed(double s) { if (s > 0) this.speed = s; }

    /** Lëviz me dt (sekonda). Funksionon në bazë të gjatësisë së rrugës. */
    public void advance(double dt, Road currentRoad) {
        if (currentRoad == null) return;
        sOnEdge += speed * dt;
        double L = currentRoad.getLength();
        while (sOnEdge >= L && edgeIndex < routeEdges.size()-1) {
            sOnEdge -= L;           // kalon te edge-i tjetër
            edgeIndex++;
            // L i ri do llogaritet nga kontrollori në thirrjen tjetër
            break;
        }
    }

    /** Kthe progres vizual 0..1 vetëm për vizatim (opsionale). */
    public double getProgressOn(Road r) {
        if (r == null) return 0;
        double L = r.getLength();
        return L <= 0 ? 0 : Math.max(0, Math.min(1, sOnEdge / L));
    }

    public boolean arrived() {
        return edgeIndex >= routeEdges.size()-1 && sOnEdge <= 0.001;
    }
}
