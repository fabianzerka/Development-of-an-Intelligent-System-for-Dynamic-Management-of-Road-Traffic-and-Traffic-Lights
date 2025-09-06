package com.example.sumo;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a vehicle travelling along a predefined sequence of road ids.
 * The class tracks progress along the current edge as well as basic metrics
 * such as travel and waiting times.
 */
public class Vehicle {

    private final String id;
    private final List<String> routeEdges;
    private int edgeIndex = 0;
    private double sOnEdge = 0.0;
    private double speed = 25.0;     // px/s (visual units)

    // metrics
    private double waitingTime = 0.0;
    private double startTime = 0.0;   // all start at t=0 in your setup
    private boolean finished = false;
    private double finishedAt = -1.0;
    private boolean lastAdvanced = true; // a lëvizi në tick-un e kaluar?
    private int stops = 0;               // sa herë kaloi në "stop"

    /** Convenience constructor accepting an array of edge ids. */
    public Vehicle(String id, String[] routeEdges) { this(id, Arrays.asList(routeEdges)); }

    /**
     * Creates a vehicle with the provided route.
     *
     * @param id         vehicle identifier
     * @param routeEdges sequence of edge ids representing the route
     */
    public Vehicle(String id, List<String> routeEdges) {
        this.id = id;
        this.routeEdges = routeEdges;
    }

    /** @return vehicle identifier */
    public String getId() { return id; }

    /** @return current edge id the vehicle occupies */
    public String getCurrentEdge() { return edgeIndex < routeEdges.size() ? routeEdges.get(edgeIndex) : null; }
    
    /** Sets the current speed in visual units per second. */
    public void setSpeed(double s) { if (s > 0) this.speed = s; }

    /** Increments waiting time by {@code dt} seconds. */
    public void addWaiting(double dt){ waitingTime += dt; }

    /** @return accumulated waiting time */
    public double getWaitingTime(){ return waitingTime; }

    /** Sets the simulation start time for this vehicle. */
    public void setStartTime(double t){ this.startTime = t; }

    /** @return start time of the vehicle */
    public double getStartTime(){ return startTime; }

    /** Marks the vehicle as finished at the given simulation time. */
    public void markFinished(double simTime) { this.finished = true; this.finishedAt = simTime; }

    /** @return whether the vehicle has completed its route */
    public boolean isFinished() { return finished; }

    /** @return simulation time when the vehicle finished */
    public double getFinishedAt() { return finishedAt; }

    /** @return total travel time (finish minus start) */
    public double getTravelTime() { return finishedAt >= 0 ? (finishedAt - startTime) : 0.0; }

    /** Advance along current road by {@code dt} seconds. */
    public void advance(double dt, Road currentRoad) {
        if (currentRoad == null) return;
        sOnEdge += speed * dt;
        double L = Math.max(1e-6, currentRoad.getLength());
        if (sOnEdge >= L && edgeIndex < routeEdges.size() - 1) {
            sOnEdge -= L;
            edgeIndex++;
        }
    }

    /** 0..1 progress along the given road (used for drawing). */
    public double getProgressOn(Road r) {
        if (r == null) return 0.0;
        double L = Math.max(1e-6, r.getLength());
        double t = sOnEdge / L;
        if (t < 0) t = 0;
        if (t > 1) t = 1;
        return t;
    }

    /** @return whether the vehicle has reached the end of its last edge */
    public boolean arrived() {
        return edgeIndex >= routeEdges.size() - 1 && sOnEdge >= Math.max(1.0, 0.99 * Math.max(1e-6, sOnEdge));
    }

    // --- position along current edge (for car-following) ---
    /** @return distance travelled along current edge */
    public double getSOnEdge() { return sOnEdge; }

    /** Sets distance travelled along current edge. */
    public void setSOnEdge(double s) { this.sOnEdge = Math.max(0, s); }

    /** @return current speed */
    public double getSpeed() { return speed; }

    /**
     * Updates the vehicle's stop counter for the current simulation tick.
     * Call this after all movement logic has been applied and {@code sOnEdge} is finalized.
     *
     * @param advanced {@code true} if the vehicle moved forward this tick,
     *                 {@code false} if it remained stationary.
     *
     * Logic:
     * - If the vehicle was moving last tick and is now stopped, count it as a new stop.
     * - Remaining stopped across multiple ticks does not increase the count.
     *
     **/
    public void markAdvanced(boolean advanced) {
        if (!advanced && lastAdvanced) stops++;  // moving -> stopped
        lastAdvanced = advanced;
    }
    
    /**
     * Returns the number of <em>stop events</em> observed for this vehicle so far.
     * A stop event is a transition from Moving → Stopped between two consecutive
     * ticks; continuous waiting over multiple ticks counts as a single stop.
     *
     * @return number of Moving→Stopped transitions detected for this vehicle
     */
    public int getStops() { return stops; }
}
