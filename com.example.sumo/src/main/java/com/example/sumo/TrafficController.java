package com.example.sumo;

import java.util.*;

public class TrafficController {

    // Koha vizuale për lëvizjen e mjeteve (në Vehicle.step())
    private double vehicleSpeed = 0.01;

    /** Vendos GREEN/RED për semaforët sipas flows. */
    public void updateTraffic(Map<TrafficLight,Integer> measuredFlows, List<Intersection> intersections){
        // nëse kemi 1 semafor për kryqëzim, le të jetë GREEN kur flow > 0
        for (Intersection inter : intersections) {
            List<TrafficLight> tls = inter.getTrafficLights();
            if (tls.isEmpty()) continue;

            if (tls.size() == 1) {
                TrafficLight tl = tls.get(0);
                int f = measuredFlows.getOrDefault(tl, 1); // default 1 që të kalojnë mjetet
                if (f > 0) tl.setGreen(); else tl.setRed();
            } else {
                // zgjedh GREEN për atë me flow më të madh
                TrafficLight best = null; int bestF = Integer.MIN_VALUE;
                for (TrafficLight tl : tls) {
                    int f = measuredFlows.getOrDefault(tl, 0);
                    if (f > bestF) { bestF = f; best = tl; }
                }
                for (TrafficLight tl : tls) {
                    if (tl == best) tl.setGreen(); else tl.setRed();
                }
            }
        }
    }

    /** Thjesht rrit progresin e çdo mjeti. */
    public void stepVehicles(List<Vehicle> vehicles){
        for (Vehicle v : vehicles) {
            v.setSpeed(vehicleSpeed);
            v.step();
        }
    }
}
