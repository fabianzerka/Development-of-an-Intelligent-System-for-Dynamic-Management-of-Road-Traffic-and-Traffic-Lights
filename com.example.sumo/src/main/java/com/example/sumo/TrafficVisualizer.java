package com.example.sumo;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class TrafficVisualizer extends JPanel {

    private List<Road> roads;
    private List<Intersection> intersections;
    private List<Vehicle> vehicles;
    private TrafficController controller;

    public TrafficVisualizer(List<Road> roads, List<Intersection> intersections,
                             List<Vehicle> vehicles, TrafficController controller) {
        this.roads = roads;
        this.intersections = intersections;
        this.vehicles = vehicles;
        this.controller = controller;
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
    	System.out.println("Drawing panel. Roads=" + roads.size() + ", Vehicles=" + vehicles.size());

        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));

        // vizualizo rrugët
        g2.setColor(Color.LIGHT_GRAY);
        for(Road r: roads){
            g2.drawLine(r.getX1(), r.getY1(), r.getX2(), r.getY2());
        }

        // vizualizo kryqëzimet dhe semaforët
        for(Intersection inter: intersections){
            int size = 10;
            int cx = inter.getIncoming().isEmpty()?0:inter.getIncoming().get(0).getX2();
            int cy = inter.getIncoming().isEmpty()?0:inter.getIncoming().get(0).getY2();

            // pika kryqëzimi
            g2.setColor(Color.BLACK);
            g2.fillOval(cx-size/2, cy-size/2, size, size);

            // semaforët
            int offset = -15;
            for(TrafficLight tl: inter.getTrafficLights()){
                Color color = tl.getState() == TrafficLight.State.GREEN ? Color.GREEN : Color.RED;
                g2.setColor(color);
                g2.fillOval(cx+offset, cy-15, 10, 10);
                offset += 15;
            }
        }

        // vizualizo makinat lëvizëse
        g2.setColor(Color.BLUE);
        for(Vehicle v: vehicles){
            Road currentRoad = null;
            for(Road r: roads){
                if(r.getId().equals(v.getCurrentEdge())){
                    currentRoad = r;
                    break;
                }
            }
            if(currentRoad != null){
                double t = v.getProgress();
                int x = (int)(currentRoad.getX1() + t*(currentRoad.getX2()-currentRoad.getX1()));
                int y = (int)(currentRoad.getY1() + t*(currentRoad.getY2()-currentRoad.getY1()));
                g2.fillRect(x-3, y-3, 6, 6);
            }
        }
        System.out.println("Drawing panel. Roads=" + roads.size() + ", Vehicles=" + vehicles.size());

    }
}
