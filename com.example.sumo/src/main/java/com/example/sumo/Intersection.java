package com.example.sumo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Intersection {

    private final String id;
    private double x; // koordinata nga osm.net.xml
    private double y;

    private final List<Road> incoming = new ArrayList<>();
    private final List<Road> outgoing = new ArrayList<>();
    private final List<TrafficLight> trafficLights = new ArrayList<>();

    public Intersection(String id) { this.id = id; }

    public String getId() { return id; }

    public void setCoordinates(double x, double y) { this.x = x; this.y = y; }
    public double getX() { return x; }
    public double getY() { return y; }

    public void addIncoming(Road r) { if (r != null) incoming.add(r); }
    public void addOutgoing(Road r) { if (r != null) outgoing.add(r); }

    public List<Road> getIncoming() { return Collections.unmodifiableList(incoming); }
    public List<Road> getOutgoing() { return Collections.unmodifiableList(outgoing); }

    public void addTrafficLight(TrafficLight tl) {
        if (tl != null) trafficLights.add(tl);
    }

    public List<TrafficLight> getTrafficLights() { return Collections.unmodifiableList(trafficLights); }

    // ndihmës (për algoritme green-wave në të ardhmen)
    public double getDistanceToPrevious(TrafficLight prev) { return 100.0; }
}
