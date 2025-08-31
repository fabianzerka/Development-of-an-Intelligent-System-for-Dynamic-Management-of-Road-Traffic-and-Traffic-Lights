package com.example.sumo;

public class TrafficLight {

    public enum State { RED, GREEN }

    private final String id;
    private final Intersection intersection;
    private State state = State.RED;

    public TrafficLight(String id, Intersection intersection) {
        this.id = id;
        this.intersection = intersection;
    }

    // kontroll i thjeshtë; mund të zgjerohet me faza/drejtim
    public void setState(State s) { this.state = s; }
    public void setGreen() { this.state = State.GREEN; }
    public void setRed() { this.state = State.RED; }
    public State getState() { return state; }

    public String getId() { return id; }
    public Intersection getIntersection() { return intersection; }

    // version minimal për kompatibilitet me thirrje të mëparshme
    public boolean isGreenFor(Road r) { return state == State.GREEN; }
    public void update(double dtSeconds) { /* no-op në këtë MWP */ }
}
