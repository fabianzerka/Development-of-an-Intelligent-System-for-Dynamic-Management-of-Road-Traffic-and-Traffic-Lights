package com.example.sumo;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class SumoNetworkReader {

    private static final Map<String, Intersection> intersections = new HashMap<>();
    private static final Map<String, Road> roads = new HashMap<>();

    public static void readNetwork(String filePath) {
        intersections.clear();
        roads.clear();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filePath));
            doc.getDocumentElement().normalize();

            // *** FIX 1: SUMO përdor <junction>, jo <node>
            NodeList juncList = doc.getElementsByTagName("junction");
            for (int i = 0; i < juncList.getLength(); i++) {
                Element j = (Element) juncList.item(i);
                String id = j.getAttribute("id");
                double x = parseD(j.getAttribute("x"), 0);
                double y = parseD(j.getAttribute("y"), 0);
                Intersection inter = new Intersection(id);
                inter.setCoordinates(x, y);
                intersections.put(id, inter);
            }

            // edges
            NodeList edgeList = doc.getElementsByTagName("edge");
            for (int i = 0; i < edgeList.getLength(); i++) {
                Element e = (Element) edgeList.item(i);
                // hiq rrugët "internal"
                if ("internal".equals(e.getAttribute("function"))) continue;

                String id = e.getAttribute("id");
                String fromId = e.getAttribute("from");
                String toId   = e.getAttribute("to");

                Intersection from = intersections.get(fromId);
                Intersection to   = intersections.get(toId);
                if (from == null || to == null) continue;

                int x1 = (int)Math.round(from.getX());
                int y1 = (int)Math.round(from.getY());
                int x2 = (int)Math.round(to.getX());
                int y2 = (int)Math.round(to.getY());

                Road r = new Road(id, from, to, x1, y1, x2, y2);
                roads.put(id, r);
                from.addOutgoing(r);
                to.addIncoming(r);
            }

            System.out.println("[SUMO] Loaded network: junctions=" + intersections.size() + ", edges=" + roads.size());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static double parseD(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    public static Collection<Intersection> getIntersections() { return intersections.values(); }
    public static Collection<Road> getRoads() { return roads.values(); }
    public static Map<String, Road> getRoadById() { return Collections.unmodifiableMap(roads); }
}
