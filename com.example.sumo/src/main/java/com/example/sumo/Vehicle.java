package com.example.sumo;

import java.util.*;

public class Vehicle {

    private String id;
    private List<String> routeEdges;
    private int currentIndex = 0;
    private double progress = 0.0; // 0.0 = fillimi i rrugës, 1.0 = fundi

    private double speed = 0.01; // units per timer tick

    public Vehicle(String id, String[] routeEdges) {
        this.id = id;
        this.routeEdges = Arrays.asList(routeEdges);
    }

    // lëviz makinë
    public void move() {
        progress += speed;
        if(progress >= 1.0){
            progress = 0.0;
            if(currentIndex < routeEdges.size() - 1)
                currentIndex++;
        }
    }

    public String getCurrentEdge() {
        return routeEdges.get(currentIndex);
    }

    public double getProgress() { return progress; }

    public String getId() { return id; }
}
