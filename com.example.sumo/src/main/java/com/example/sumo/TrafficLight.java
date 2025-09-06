package com.example.sumo;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Two-phase traffic light bound to a single Intersection.
 * Phase 0 serves group A of approaches (roughly E–W), phase 1 serves group B (N–S).
 * Adaptive timing: min-green, gap-out, soft max. Initial phase is randomized
 * per light to avoid global synchronization.
 */
public class TrafficLight {

    public enum State { GREEN, RED }

    private final String id;
    private final Intersection intersection;

    // Incoming edges split into two groups
    private final Set<String> groupA = new HashSet<>();
    private final Set<String> groupB = new HashSet<>();

    // 0 = A green, 1 = B green
    private int phase;

    // timers [s]
    private double greenElapsed = 0.0;
    private double minGreen     = 5.0;   // lower bound to avoid flicker
    private double maxGreen     = 18.0;  // soft cap to prevent starvation

    // small per-light jitter to desynchronize switching
    private final double jitter; // 0..2s

    public TrafficLight(String id, Intersection intersection) {
        this.id = id;
        this.intersection = intersection;
        autoGroupIncoming();

        Random rng = new Random(id.hashCode() ^ System.nanoTime());
        this.phase  = rng.nextBoolean() ? 0 : 1;      // random start
        this.jitter = 2.0 * rng.nextDouble();         // 0..2s
        this.greenElapsed = rng.nextDouble() * (minGreen + jitter); // stagger start
    }

    public String getId()         { return id; }
    public Intersection getNode() { return intersection; }
    public int getPhase()         { return phase; }

    /** Kept for compatibility: treat as min-green. */
    public void setGreenDuration(double seconds) {
        minGreen = Math.max(2.0, seconds);
        maxGreen = Math.max(minGreen + 6.0, minGreen * 2.5);
    }

    public void setMinGreen(double s) { setGreenDuration(s); }
    public void setMaxGreen(double s) { maxGreen = Math.max(minGreen + 1.0, s); }

    /** 0 for group A, 1 for group B. */
    public int groupOf(Road incoming) {
        return groupA.contains(incoming.getId()) ? 0 : 1;
    }

    /** Green for this incoming road right now? */
    public boolean isGreenFor(Road incoming) { return groupOf(incoming) == phase; }
    public boolean isRedFor  (Road incoming) { return !isGreenFor(incoming); }

    /** Single-dot state used by the visualizer (GREEN when phase==0). */
    public State getState() { return (phase == 0) ? State.GREEN : State.RED; }

    /**
     * Adaptive decision:
     *  - hold until minGreen+jitter
     *  - if current side has demand and other side is lighter, extend up to maxGreen
     *  - if opposite is heavier OR current has no demand, switch (once min met)
     *  - hard switch at maxGreen
     */
    public void updateAdaptive(double dt, int dA, int dB, double nowIgnored) {
        greenElapsed += dt;

        final double effMin = minGreen + jitter * 0.3; // small per-light offset
        int curr = (phase == 0 ? dA : dB);
        int oth  = (phase == 0 ? dB : dA);

        // always respect effective min-green
        if (greenElapsed < effMin) return;

        boolean mustSwitch = false;

        // gap-out: current side empty while opposite has cars
        if (curr == 0 && oth > 0) mustSwitch = true;

        // pressure-bias: if opposite queue is significantly larger, switch
        if (!mustSwitch && oth >= curr + 3) mustSwitch = true; // needs 3+ more vehicles

        // hard maximum
        if (!mustSwitch && greenElapsed >= maxGreen) mustSwitch = true;

        // otherwise, continue serving current queue (extend)
        if (mustSwitch) {
            phase = 1 - phase;
            greenElapsed = 0.0;
        }
    }

    /** Legacy fixed update (unused by controller). */
    public void update(double dt) {
        greenElapsed += dt;
        if (greenElapsed >= Math.max(2.0, minGreen)) {
            phase = 1 - phase;
            greenElapsed = 0.0;
        }
    }

    /* ---------- helpers ---------- */

    /** Split incoming edges into two orthogonal sets by approach vector. */
    private void autoGroupIncoming() {
        groupA.clear(); groupB.clear();
        for (Road in : intersection.getIncoming()) {
            double dx = intersection.getX() - in.getX1();
            double dy = intersection.getY() - in.getY1();
            if (Math.abs(dx) >= Math.abs(dy)) groupA.add(in.getId()); // approx E–W
            else                               groupB.add(in.getId()); // approx N–S
        }
        if (groupA.isEmpty() || groupB.isEmpty()) {
            groupA.clear(); groupB.clear();
            int i = 0;
            for (Road in : intersection.getIncoming()) {
                ((i++ & 1) == 0 ? groupA : groupB).add(in.getId());
            }
        }
    }
}
