package com.example.sumo;

import java.util.*;

public class TrafficController {
    private final Map<String, Road> roadById;

    private double simTime = 0.0;
    private double vehicleSpeed = 25.0;

    private final List<Vehicle> finishedVehicles = new ArrayList<>();
    private int completed = 0;
    private double totalWaitCompleted = 0.0;

    // stop distance near intersection (only red stops)
    private final double STOP_DIST_MIN = 4.0;
    private final double STOP_DIST_MAX = 18.0;

    public TrafficController(Map<String, Road> roadById) { this.roadById = roadById; }
    public void setVehicleSpeed(double s) { if (s > 0) vehicleSpeed = s; }

    public double getSimTime(){ return simTime; }
    public int getCompletedCount(){ return completed; }
    public double getAvgWaitCompleted(){ return completed == 0 ? 0.0 : totalWaitCompleted / completed; }
    public List<Vehicle> getFinishedVehiclesSnapshot(){ return Collections.unmodifiableList(finishedVehicles); }

    public void tick(double dt, List<Intersection> intersections, List<Vehicle> vehicles) {
        simTime += dt;

        // 1) cycle signals
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) {
                inter.getTrafficLights().get(0).update(dt);
            }
        }

        // 2) move vehicles; remove those finished
        List<Vehicle> toRemove = new ArrayList<>();
        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;
            v.setSpeed(vehicleSpeed);

            if (mustStopAtRed(v, r)) {
                v.addWaiting(dt);
                continue;
            }
            v.advance(dt, r);

            if (!v.isFinished() && v.arrived()) {
                v.markFinished(simTime);
                completed++;
                totalWaitCompleted += v.getWaitingTime();
                finishedVehicles.add(v);
                toRemove.add(v);
            }
        }
        if (!toRemove.isEmpty()) vehicles.removeAll(toRemove);
    }

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
