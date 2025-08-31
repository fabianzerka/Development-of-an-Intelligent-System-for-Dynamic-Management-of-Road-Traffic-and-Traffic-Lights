package com.example.sumo.gui;

import com.example.sumo.*; 

import javax.swing.*;
import javax.swing.Timer; 
import java.util.*;

public class TrafficSimulation {

    public static void main(String[] args){
        SumoNetworkReader.readNetwork("src/main/resources/sumo_output/osm.net.xml");
        List<Intersection> intersections = new ArrayList<>(SumoNetworkReader.getIntersections());
        List<Road> roads = new ArrayList<>(SumoNetworkReader.getRoads());
        

        // krijo semaforë
        for(Intersection inter: intersections){
            if(!inter.getIncoming().isEmpty()){
                inter.addTrafficLight(new TrafficLight("TL_"+inter.getId(), inter));
            }
        }
        
        System.out.println("=== Intersections and Traffic Lights ===");
        for(Intersection inter : intersections) {
            System.out.println("Intersection: " + inter.getId() + " incoming=" + inter.getIncoming().size() + " outgoing=" + inter.getOutgoing().size());
            for(TrafficLight tl : inter.getTrafficLights()) {
                System.out.println("  TrafficLight id=" + tl.getId());
            }
        }

        
        List<Vehicle> vehicles = SumoVehiclesReader.readVehicles("src/main/resources/sumo_output/routes.rou.xml");
        TrafficController controller = new TrafficController();

        TrafficVisualizer panel = new TrafficVisualizer(roads, intersections, vehicles, controller);
        JFrame frame = new JFrame("SUMO Traffic Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,600);
        frame.add(panel);
        frame.setVisible(true);

        // Timer për lëvizje dhe green wave
        Timer timer = new Timer(50, e->{
            for(Vehicle v: vehicles) v.move();

            // lexon fluksin nga edgeData.xml
            Map<String,Integer> flows = SumoReader.readTrafficFlows("src/main/resources/sumo_output/edgeData.xml");
            Map<TrafficLight,Integer> measured = new HashMap<>();
            for(Intersection inter:intersections){
                for(TrafficLight tl: inter.getTrafficLights()){
                    if(!inter.getIncoming().isEmpty()){
                        String edgeId = inter.getIncoming().get(0).getId();
                        measured.put(tl, flows.getOrDefault(edgeId,0));
                    }
                }
            }

            controller.updateTraffic(measured, intersections);
            panel.repaint();
        });
        timer.start();
    }
}
