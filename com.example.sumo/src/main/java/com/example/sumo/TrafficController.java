package com.example.sumo;

import java.util.*;

/**
 * Advances the simulation:
 *  - adapts every signal from measured stop-line queues (local control),
 *  - moves vehicles (red-light compliance, simple car-following),
 *  - tracks throughput and waiting metrics.
 */
public class TrafficController {

    private final Map<String, Road> roadById;

    // time & speed
    private double simTime = 0.0;
    private double vehicleSpeed = 25.0; // px/s (your units)

    // metrics
    private final List<Vehicle> finishedVehicles = new ArrayList<>();
    private int completed = 0;
    private double totalWaitCompleted = 0.0;

    // behavior parameters (pixels)
    private final double STOP_DIST_MIN = 4.0;
    private final double STOP_DIST_MAX = 18.0;
    private final double MIN_GAP       = 12.0; // same-edge spacing
    private final double QUEUE_WIN     = 40.0; // stop-line window to count demand

    public TrafficController(Map<String, Road> roadById) { this.roadById = roadById; }

    public void setVehicleSpeed(double s) { if (s > 0) vehicleSpeed = s; }
    public double getSimTime(){ return simTime; }

    public int getCompletedCount(){ return completed; }
    public double getAvgWaitCompleted(){ return completed == 0 ? 0.0 : totalWaitCompleted / completed; }
    public List<Vehicle> getFinishedVehiclesSnapshot(){ return Collections.unmodifiableList(finishedVehicles); }

    /** Reset metrics and clock (optional Reset button). */
    public void reset() {
        simTime = 0.0;
        completed = 0;
        totalWaitCompleted = 0.0;
        finishedVehicles.clear();
    }

    /* ================= main step ================= */

    public void tick(double dt, List<Intersection> intersections, List<Vehicle> vehicles) {
        simTime += dt;

        // 0) measure queued demand near the stop line for each intersection (A,B)
        Map<Intersection,int[]> demand = computeDemand(vehicles);

        // 1) adapt all signals locally
        for (Intersection inter : intersections) {
            if (inter.getTrafficLights().isEmpty()) continue;
            TrafficLight tl = inter.getTrafficLights().get(0);
            int[] d = demand.getOrDefault(inter, new int[2]);
            tl.updateAdaptive(dt, d[0], d[1], simTime);
        }

        // 2) move vehicles (stop on red near stop-line; then advance)
        Map<Vehicle, Double> prevPos = new IdentityHashMap<>();
        for (Vehicle v : vehicles) prevPos.put(v, v.getSOnEdge());

        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;
            v.setSpeed(vehicleSpeed);

            if (mustStopAtRed(v, r)) { v.addWaiting(dt); continue; }
            v.advance(dt, r);
        }

        // 3) simple car-following per edge: enforce MIN_GAP
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

        // 4) count stops and finalize finished vehicles
        for (Vehicle v : vehicles) {
            double prev = prevPos.getOrDefault(v, 0.0);
            boolean advanced = v.getSOnEdge() > prev + 1e-6;
            v.markAdvanced(advanced);
        }

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

    /* ============ helpers ============ */

    /** Demand per intersection: [dA, dB] vehicles within QUEUE_WIN of stop line. */
    private Map<Intersection,int[]> computeDemand(List<Vehicle> vehicles) {
        Map<Intersection,int[]> dem = new HashMap<>();
        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;

            Intersection end = r.getEndIntersection();
            if (end == null || end.getTrafficLights().isEmpty()) continue;
            TrafficLight tl = end.getTrafficLights().get(0);

            double L = r.getLength();
            double distToEnd = (1.0 - v.getProgressOn(r)) * L;
            if (distToEnd > QUEUE_WIN) continue;

            int grp = tl.groupOf(r); // 0 or 1
            dem.computeIfAbsent(end, k -> new int[2])[grp]++;
        }
        return dem;
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
