package com.example.sumo;

import javax.swing.Timer;
import java.util.*;

public class TrafficController {

    private final int MIN_GREEN = 2;
    private final int MAX_GREEN = 6;
    private Map<TrafficLight,Integer> greenTimers = new HashMap<>();
    private double averageSpeed = 0.02; // units/sec

    public void updateTraffic(Map<TrafficLight,Integer> measuredFlows, List<Intersection> intersections){

        List<TrafficLight> allLights = new ArrayList<>(measuredFlows.keySet());
        allLights.sort((a,b)->measuredFlows.get(b)-measuredFlows.get(a));

        for(TrafficLight tl: allLights){
            int flow = measuredFlows.get(tl);
            int greenTime = Math.min(MAX_GREEN, Math.max(MIN_GREEN, flow/2));
            greenTimers.put(tl,greenTime);
            tl.setState(TrafficLight.State.RED);
        }

        // green wave
        for(List<TrafficLight> corridor: getMainCorridors(intersections)){
            for(int i=0;i<corridor.size();i++){
                TrafficLight tl = corridor.get(i);
                double distance = tl.getIntersection().getDistanceToPrevious(i>0? corridor.get(i-1):null);
                int delay = (int)(distance/averageSpeed*1000);

                Timer timer = new Timer(delay,e->tl.setState(TrafficLight.State.GREEN));
                timer.setRepeats(false);
                timer.start();
            }
        }
    }

    private List<List<TrafficLight>> getMainCorridors(List<Intersection> intersections){
        List<List<TrafficLight>> corridors = new ArrayList<>();
        for(Intersection inter: intersections){
            if(!inter.getIncoming().isEmpty() && !inter.getTrafficLights().isEmpty())
                corridors.add(inter.getTrafficLights());
        }
        return corridors;
    }

    public int getGreenTime(TrafficLight tl){ return greenTimers.getOrDefault(tl,MIN_GREEN); }
}
