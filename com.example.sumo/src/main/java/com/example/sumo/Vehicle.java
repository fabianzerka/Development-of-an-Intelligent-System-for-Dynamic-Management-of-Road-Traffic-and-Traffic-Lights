package com.example.sumo;

import java.util.Arrays;
import java.util.List;

public class Vehicle {

    private final String id;
    private final List<String> routeEdges;
    private int currentIndex = 0;
    private double progress = 0.0; // 0..1 përgjatë edge-it aktual
    private double speed = 0.01;   // "units" për tick (vizuale)

    public Vehicle(String id, String[] routeEdges) {
        this(id, Arrays.asList(routeEdges));
    }

    public Vehicle(String id, List<String> routeEdges) {
        this.id = id;
        this.routeEdges = routeEdges;
    }

    public String getId() { return id; }
    public String getCurrentEdge() { return routeEdges.get(currentIndex); }
    public double getProgress() { return progress; }

    public void setSpeed(double s) { if (s > 0) this.speed = s; }

    // thirret në çdo tick të timer-it
    public void step() {
        progress += speed;
        if (progress >= 1.0) {
            progress = 0.0;
            if (currentIndex < routeEdges.size() - 1) currentIndex++;
        }
    }

    public boolean arrived() {
        return currentIndex >= routeEdges.size() - 1 && progress >= 0.999;
    }
}
