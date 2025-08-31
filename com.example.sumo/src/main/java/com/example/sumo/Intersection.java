package com.example.sumo;

import java.util.*;

public class Intersection {

    private String id;
    private double x = 0; // X coordinate
    private double y = 0; // Y coordinate

    private List<Road> incoming = new ArrayList<>();
    private List<Road> outgoing = new ArrayList<>();
    private List<TrafficLight> trafficLights = new ArrayList<>();

    public Intersection(String id) {
        this.id = id;
    }

    public String getId() { return id; }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double getX() { return x; }
    public double getY() { return y; }

    public void addIncomingRoad(Road r) { incoming.add(r); }
    public void addOutgoingRoad(Road r) { outgoing.add(r); }

    public List<Road> getIncoming() { return incoming; }
    public List<Road> getOutgoing() { return outgoing; }

    public void addTrafficLight(TrafficLight tl) {
        trafficLights.add(tl);
    }

    public List<TrafficLight> getTrafficLights() {
        return trafficLights;
    }

    // për green wave: distanca midis semaforëve të rrugës kryesore
    public double getDistanceToPrevious(TrafficLight prev) {
        if(prev == null) return 0;
        // shembull: vendosim një distance fikse, mund ta bëjmë sipas koordinatave të rrugës
        return 100.0;
    }
}
