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
    private static final double STOP_DIST_MIN = 4.0;
    private static final double STOP_DIST_MAX = 18.0;

    // car-following params (pixels)
    private static final double MIN_GAP  = 12.0;
    private static final double QUEUE_WIN = 40.0;

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
