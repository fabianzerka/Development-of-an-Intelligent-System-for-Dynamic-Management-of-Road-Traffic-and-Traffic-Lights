package com.example.sumo;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class SumoNetworkReader {

    public static Map<String, Intersection> intersections = new HashMap<>();
    public static Map<String, Road> roads = new HashMap<>();

    public static void readNetwork(String filePath) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

            InputStream input;
            if (filePath.endsWith(".gz")) {
                input = new GZIPInputStream(new FileInputStream(new File(filePath)));
            } else {
                input = new FileInputStream(new File(filePath));
            }

            Document doc = dBuilder.parse(input);
            doc.getDocumentElement().normalize();

            // --- Load junctions as intersections ---
            NodeList junctions = doc.getElementsByTagName("junction");
            for (int i = 0; i < junctions.getLength(); i++) {
                Element j = (Element) junctions.item(i);
                Intersection inter = new Intersection(j.getAttribute("id"));

                // Optional: store coordinates if present
                if (j.hasAttribute("x") && j.hasAttribute("y")) {
                    inter.setX(Double.parseDouble(j.getAttribute("x")));
                    inter.setY(Double.parseDouble(j.getAttribute("y")));
                }

                intersections.put(inter.getId(), inter);
            }

            // --- Load edges as roads ---
            NodeList edges = doc.getElementsByTagName("edge");
            for (int i = 0; i < edges.getLength(); i++) {
                Element edge = (Element) edges.item(i);
                String id = edge.getAttribute("id");

                NodeList lanes = edge.getElementsByTagName("lane");
                double startX = 0, startY = 0, endX = 0, endY = 0;
                if (lanes.getLength() > 0) {
                    Element lane = (Element) lanes.item(0);
                    String[] points = lane.getAttribute("shape").split(" ");
                    String[] start = points[0].split(",");
                    String[] end = points[points.length - 1].split(",");
                    startX = Double.parseDouble(start[0]);
                    startY = Double.parseDouble(start[1]);
                    endX = Double.parseDouble(end[0]);
                    endY = Double.parseDouble(end[1]);
                }

                // Find nearest intersections
                Intersection from = findNearest(startX, startY);
                Intersection to = findNearest(endX, endY);

                Road r = new Road(id, from, to, (int) startX, (int) startY, (int) endX, (int) endY);
                roads.put(id, r);

                if (from != null) from.addOutgoingRoad(r);
                if (to != null) to.addIncomingRoad(r);
            }

            System.out.println("Loaded " + roads.size() + " roads and " + intersections.size() + " intersections.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Find nearest intersection by coordinates ---
    public static Intersection findNearest(double x, double y) {
        Intersection nearest = null;
        double minDist = Double.MAX_VALUE;

        for (Intersection inter : intersections.values()) {
            double dx = inter.getX() - x;
            double dy = inter.getY() - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < minDist) {
                minDist = dist;
                nearest = inter;
            }
        }

        return nearest;
    }

    public static Collection<Intersection> getIntersections() {
        return intersections.values();
    }

    public static Collection<Road> getRoads() {
        return roads.values();
    }
}
