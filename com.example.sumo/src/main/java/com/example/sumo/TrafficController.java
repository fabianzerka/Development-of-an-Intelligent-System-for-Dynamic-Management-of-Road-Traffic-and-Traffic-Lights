package com.example.sumo;

import java.util.List;
import java.util.Map;

public class TrafficController {
    private final Map<String,Road> roadById;
    private double vehicleSpeed = 20.0; // pixel/s (rrite nëse do lëvizje më të shpejtë)

    public TrafficController(Map<String,Road> roadById) { this.roadById = roadById; }
    public void setVehicleSpeed(double s) { if (s > 0) vehicleSpeed = s; }

    /** Përditëson semaforët dhe lëviz mjetet (dt në sekonda). */
    public void tick(double dt, List<Intersection> intersections, List<Vehicle> vehicles) {
        // 1) semaforët
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) {
                inter.getTrafficLights().get(0).update(dt);
            }
        }

        // 2) mjetet
        for (Vehicle v : vehicles) {
            Road r = roadById.get(v.getCurrentEdge());
            if (r == null) continue;
            v.setSpeed(vehicleSpeed);

            // ndalim para të kuqes (në fund të rrugës)
            if (mustStopAtRed(v, r)) continue;

            v.advance(dt, r);
        }
    }

    private boolean mustStopAtRed(Vehicle v, Road r) {
        Intersection end = r.getEndIntersection();
        if (end == null || end.getTrafficLights().isEmpty()) return false;
        TrafficLight tl = end.getTrafficLights().get(0);

        // distanca deri te fundi i rrugës
        double L = r.getLength();
        double s = v.getProgressOn(r) * L;
        double distToEnd = L - s;

        double stopDist = 18.0; // ~18px para fund-it
        return tl.isRedFor(r) && distToEnd < stopDist;
    }
}
