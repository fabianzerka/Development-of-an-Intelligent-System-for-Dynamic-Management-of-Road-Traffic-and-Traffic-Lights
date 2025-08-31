package com.example.sumo;

import java.util.*;

public class TrafficController {
    private final Map<String,Road> roadById;
    private double vehicleSpeed = 0.02; // rrite për lëvizje më të dukshme

    public TrafficController(Map<String,Road> roadById) {
        this.roadById = roadById;
    }

    public void setVehicleSpeed(double s) { if (s > 0) vehicleSpeed = s; }

    /** Vendos GREEN/RED (aktualisht lëmë jeshile default). */
    public void updateTraffic(List<Intersection> intersections){
        for (Intersection inter : intersections) {
            if (!inter.getTrafficLights().isEmpty()) {
                inter.getTrafficLights().get(0).setGreen(); // thjesht jeshile tani
            }
        }
    }

    /** Lëviz mjetet, por ndal kur ka semafor të kuq shumë afër. */
    public void stepVehicles(List<Vehicle> vehicles){
        for (Vehicle v : vehicles) {
            v.setSpeed(vehicleSpeed);
            if (!shouldStop(v)) v.step();
        }
    }

    private boolean shouldStop(Vehicle v) {
        Road r = roadById.get(v.getCurrentEdge());
        if (r == null) return false;
        // gjej semaforin në fundin e rrugës
        Intersection end = r.getEndIntersection();
        if (end == null || end.getTrafficLights().isEmpty()) return false;
        TrafficLight tl = end.getTrafficLights().get(0);

        // distanca (në njësi ekrani) deri në fund të rrugës
        double distToEnd = (1.0 - v.getProgress()) * r.getLength();
        double stopDist = 12.0; // piksele ~ 12px para fund-it

        return tl.getState() == TrafficLight.State.RED && distToEnd < stopDist;
    }
}
