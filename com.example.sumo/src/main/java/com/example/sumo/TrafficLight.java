package com.example.sumo;

public class TrafficLight {

    public enum State { RED, GREEN }

    private String id;
    private State state;
    private Intersection intersection; // lidhja me kryqëzimin

    public TrafficLight(String id, Intersection intersection) {
        this.id = id;
        this.state = State.RED;
        this.intersection = intersection;
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public String getId() {
        return id;
    }

    public Intersection getIntersection() {
        return intersection;
    }
}
