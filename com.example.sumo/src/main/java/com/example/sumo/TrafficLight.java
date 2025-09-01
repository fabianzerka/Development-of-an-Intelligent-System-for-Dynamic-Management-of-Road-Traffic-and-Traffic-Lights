package com.example.sumo;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple two-phase traffic light assigned to an {@link Intersection}. Incoming
 * roads are automatically split into two orthogonal groups (north–south and
 * east–west) and the light cycles between them with a fixed green duration.
 */
public class TrafficLight {

    /** High level state used by the visualizer. */
    public enum State { GREEN, RED }

    private final String id;
    private final Intersection inter;

    // Group 0 = horizontal, group 1 = vertical; populated from incoming roads
    private final List<Road> group0 = new ArrayList<>();
    private final List<Road> group1 = new ArrayList<>();

    private int phase = 0;           // 0 => group0 green, 1 => group1 green
    private double timer = 0.0;
    private double greenDur = 8.0;  // seconds
    
    /**
     * Creates a traffic light for the given intersection and automatically
     * populates road groups based on their orientation.
     *
     * @param id    light identifier
     * @param inter target intersection
     */
    public TrafficLight(String id, Intersection inter) {
        this.id = id;
        this.inter = inter;
        autoGroupIncoming(inter);
    }

    /** @return traffic light identifier */
    public String getId() { return id; }

    /** @return associated intersection */
    public Intersection getIntersection() { return inter; }

    /**
     * Updates the current phase timer. Should be called once per simulation
     * tick.
     *
     * @param dt elapsed time in seconds
     */
    public void update(double dt) {
        if (group0.isEmpty() && group1.isEmpty()) return;
        timer += dt;
        if (timer >= greenDur) {
            timer = 0.0;
            phase = 1 - phase;
        }
    }

    /**
     * Checks if the given road has a green signal.
     *
     * @param incoming road approaching the intersection
     * @return {@code true} if vehicles from that road may proceed
     */
    public boolean isGreenFor(Road incoming) {
        if (incoming == null) return true;
        if (group0.isEmpty() && group1.isEmpty()) return true;
        return (phase == 0 ? group0 : group1).contains(incoming);
    }

    /** @return {@code true} if the road faces a red signal */
    public boolean isRedFor(Road incoming) { return !isGreenFor(incoming); }

    /**
     * Convenience state for the visualizer – reports green if any incoming road
     * currently has a green light.
     */
    public State getState() {
        if (inter == null || inter.getIncoming().isEmpty()) return State.GREEN;
        for (Road r : inter.getIncoming()) if (isGreenFor(r)) return State.GREEN;
        return State.RED;
    }

    // ==== Helpers for adaptation ====

    /** @return group index (0/1) for the given road or -1 if not controlled */
    public int groupOf(Road r) {
        if (r == null) return -1;
        if (group0.contains(r)) return 0;
        if (group1.contains(r)) return 1;
        return -1;
    }    

    /** @return current phase (0 or 1) */
    public int getPhase() { return phase; }

    /** Forces the traffic light into a specific phase and resets the timer. */
    public void forcePhase(int p) { if (p == 0 || p == 1) { phase = p; timer = 0; } }

    /** Sets the duration of the green phase for each group. */
    public void setGreenDuration(double seconds) { if (seconds > 0) this.greenDur = seconds; }

    // ==== Automatic grouping N–S / E–W based on road angle ====

    /**
     * Splits incoming roads into two groups based on their dominant axis. Roads
     * with a larger horizontal component go to group 0; others go to group 1.
     */
    private void autoGroupIncoming(Intersection inter) {
        if (inter == null) return;
        for (Road r : inter.getIncoming()) {
            double dx = r.getX2() - r.getX1();
            double dy = r.getY2() - r.getY1();
            if (Math.abs(dx) >= Math.abs(dy)) group0.add(r); else group1.add(r);
        }
    }
}
