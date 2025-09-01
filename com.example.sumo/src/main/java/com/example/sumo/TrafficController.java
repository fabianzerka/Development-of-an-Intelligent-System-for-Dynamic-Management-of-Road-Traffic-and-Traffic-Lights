package com.example.sumo;

import java.util.*;

/**
 * Core simulation engine that advances vehicles, cycles traffic lights and
 * gathers statistics. The controller operates in discrete time steps provided
 * via {@link #tick(double, List, List)}.
 */
public class TrafficController {
    private final Map<String, Road> roadById;

    private double simTime = 0.0;
    private double vehicleSpeed = 25.0;

    private final List<Vehicle> finishedVehicles = new ArrayList<>();
    private int completed = 0;
    private double totalWaitCompleted = 0.0;

    // stop distance near intersection (only red stops)
    private static final double STOP_DIST_MIN = 4.0;
    private static final double STOP_DIST_MAX = 18.0;

    // car-following params (pixels)
    private static final double MIN_GAP  = 12.0;
    private static final double QUEUE_WIN = 40.0;

    /** Constructs a controller bound to the provided road map. */
    public TrafficController(Map<String, Road> roadById) { this.roadById = roadById; }

    /** Sets the desired cruising speed for vehicles (visual units per second). */
    public void setVehicleSpeed(double s) { if (s > 0) vehicleSpeed = s; }

    /** @return current simulation time in seconds */
    public double getSimTime(){ return simTime; }

    /** @return number of vehicles that have finished their routes */
    public int getCompletedCount(){ return completed; }

    /** @return average waiting time of completed vehicles */
    public double getAvgWaitCompleted(){ return completed == 0 ? 0.0 : totalWaitCompleted / completed; }

    /** Snapshot of finished vehicles for reporting or exporting. */
    public List<Vehicle> getFinishedVehiclesSnapshot(){ return Collections.unmodifiableList(finishedVehicles); }
    
    /**
     * Advances the entire simulation by {@code dt} seconds.
     *
     * @param dt            time step in seconds
     * @param intersections all intersections in the network
     * @param vehicles      active vehicles in the simulation
     */
    public void tick(double dt, List<Intersection> intersections, List<Vehicle> vehicles) {
        simTime += dt;

        // 1) cycle signals
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) inter.getTrafficLights().get(0).update(dt);
        }

        // 2) move (save previous s for car-following)
        Map<Vehicle, Double> prevPos = new HashMap<>();
        for (Vehicle v : vehicles) prevPos.put(v, v.getSOnEdge());

        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;
            v.setSpeed(vehicleSpeed);

            if (mustStopAtRed(v, r)) { v.addWaiting(dt); continue; }
            v.advance(dt, r);
        }

        // 3) car-following per edge
        Map<String, List<Vehicle>> perEdge = new HashMap<>();
        for (Vehicle v : vehicles) perEdge.computeIfAbsent(v.getCurrentEdge(), k -> new ArrayList<>()).add(v);

        for (Map.Entry<String, List<Vehicle>> e : perEdge.entrySet()) {
            Road r = roadById.get(e.getKey());
            if (r == null) continue;
            double L = r.getLength();

            List<Vehicle> list = e.getValue();
            list.sort(Comparator.comparingDouble(Vehicle::getSOnEdge));

            for (int i = 1; i < list.size(); i++) {
                Vehicle lead = list.get(i-1);
                Vehicle foll = list.get(i);
                double desired = foll.getSOnEdge();
                double front   = lead.getSOnEdge();

                if (desired - front < MIN_GAP) {
                    double newPos = Math.min(front + MIN_GAP, L - 1e-3);
                    if (newPos <= prevPos.getOrDefault(foll, 0.0) + 1e-3) {
                        foll.addWaiting(dt);
                    }
                    foll.setSOnEdge(newPos);
                }
            }
        }

        // 4) remove finished + throughput
        List<Vehicle> toRemove = new ArrayList<>();
        for (Vehicle v : vehicles) {
            if (!v.isFinished() && v.arrived()) {
                v.markFinished(simTime);
                completed++;
                totalWaitCompleted += v.getWaitingTime();
                finishedVehicles.add(v);
                toRemove.add(v);
            }
        }
        
     // 3.5) shëno lëvizjen reale për llogaritjen e ndalesave
        for (Vehicle v : vehicles) {
            double prev = prevPos.getOrDefault(v, 0.0);
            boolean advanced = v.getSOnEdge() > prev + 1e-6;
            v.markAdvanced(advanced);
        }

        if (!toRemove.isEmpty()) vehicles.removeAll(toRemove);
    }
    
    /**
     * Determines whether a vehicle must stop before entering an intersection
     * because of a red signal on the current road.
     */
    private boolean mustStopAtRed(Vehicle v, Road r) {
        Intersection end = r.getEndIntersection();
        if (end == null || end.getTrafficLights().isEmpty()) return false;
        TrafficLight tl = end.getTrafficLights().get(0);

        double L = r.getLength();
        double t = v.getProgressOn(r);
        double distToEnd = (1.0 - t) * L;

        double stopDist = Math.max(STOP_DIST_MIN, Math.min(STOP_DIST_MAX, 0.15 * L));
        return tl.isRedFor(r) && distToEnd < stopDist;
    }
}
