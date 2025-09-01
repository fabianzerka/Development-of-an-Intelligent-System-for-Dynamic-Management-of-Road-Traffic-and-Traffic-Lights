package com.example.sumo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrafficController {
    private final Map<String, Road> roadById;

    // parametra të thjeshtë “vizualë”
    private double vehicleSpeed = 25.0;   // piksele/sek
    private final double STOP_DIST   = 18.0; // px para kryqëzimit
    private final double QUEUE_WIN   = 40.0; // px nga fundi i rrugës për të numëruar “në radhë”

	private double simTime = 0.0;
	private int completed = 0;
	private double totalWaitCompleted = 0.0;
    
    public TrafficController(Map<String, Road> roadById) {
        this.roadById = roadById;
    }

    public void setVehicleSpeed(double s) { if (s > 0) vehicleSpeed = s; }

    /** Tick i vetëm: përditëson semaforët, adaptimin dhe lëviz mjetet. */
    public void tick(double dt, List<Intersection> intersections, List<Vehicle> vehicles) {
    	simTime += dt;
        // 1) përditëso timer-in e semaforëve
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) {
                inter.getTrafficLights().get(0).update(dt);
            }
        }

        // 2) ADAPTIVE: mat radhët sipas afërsisë me kryqëzimin dhe përshtat fazën/greenDur
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) {
                inter.getTrafficLights().get(0).update(dt);
            }
        }

        // 3) lëviz mjetet; nëse e kuqe dhe afër, akumulo pritje
        List<Vehicle> toRemove = new ArrayList<>();
        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;

            v.setSpeed(vehicleSpeed);
            if (mustStopAtRed(v, r)) { v.addWaiting(dt); continue; }

            v.advance(dt, r);

            if (v.arrived() && !v.isFinished()) {
                v.markFinished(simTime);
                completed++;
                totalWaitCompleted += v.getWaitingTime();
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

        // Prag dinamik: mos i ndal shumë larg në edge të shkurtra
        double stopDistPx = Math.max(4.0, Math.min(18.0, 0.15 * L)); // 4..18 px, ose 15% e gjatësisë
        return tl.isRedFor(r) && distToEnd < stopDistPx;
    }
    
    public int getCompletedCount() { return completed; }
    public double getAvgWaitCompleted() { return completed == 0 ? 0.0 : totalWaitCompleted / completed; }

}
