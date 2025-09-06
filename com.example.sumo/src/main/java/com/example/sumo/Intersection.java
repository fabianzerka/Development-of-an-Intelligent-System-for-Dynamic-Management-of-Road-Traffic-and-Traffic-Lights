package com.example.sumo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of a junction within the road network. Each intersection keeps
 * track of incoming and outgoing roads as well as attached traffic lights.
 */
public class Intersection {

    private final String id;
    private double x; // koordinata nga osm.net.xml
    private double y;

    private final List<Road> incoming = new ArrayList<>();
    private final List<Road> outgoing = new ArrayList<>();
    private final List<TrafficLight> trafficLights = new ArrayList<>();

    /** Creates an intersection with the specified identifier. */
    public Intersection(String id) { this.id = id; }

    /** @return intersection identifier */
    public String getId() { return id; }

    /** Sets the world coordinates of the intersection. */
    public void setCoordinates(double x, double y) { this.x = x; this.y = y; }
    
    /** @return x coordinate */
    public double getX() { return x; }
    
    /** @return y coordinate */
    public double getY() { return y; }

    /** Adds an incoming road to the intersection. */
    public void addIncoming(Road r) { if (r != null) incoming.add(r); }

    /** Adds an outgoing road from the intersection. */
    public void addOutgoing(Road r) { if (r != null) outgoing.add(r); }

    /** @return unmodifiable view of incoming roads */
    public List<Road> getIncoming() { return Collections.unmodifiableList(incoming); }

    /** @return unmodifiable view of outgoing roads */
    public List<Road> getOutgoing() { return Collections.unmodifiableList(outgoing); }

    /** Attaches a traffic light to the intersection. */
    public void addTrafficLight(TrafficLight tl) {
        if (tl != null) trafficLights.add(tl);
    }

    /** @return traffic lights controlling this intersection */
    public List<TrafficLight> getTrafficLights() { return Collections.unmodifiableList(trafficLights); }
}
