package com.example.sumo;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.geom.Point2D;
import java.io.File;
import java.util.*;

/**
 * Utility responsible for loading a SUMO network definition ({@code osm.net.xml})
 * into in-memory {@link Intersection} and {@link Road} objects. The reader
 * parses junctions and edges, extracting polyline geometry where available.
 */
public class SumoNetworkReader {

    private static final Map<String, Intersection> intersections = new HashMap<>();
    private static final Map<String, Road> roads = new HashMap<>();

    /**
     * Reads a SUMO network file and populates the internal collections of
     * intersections and roads. Existing data is cleared before loading.
     *
     * @param filePath path to the {@code osm.net.xml} network file
     */
    public static void readNetwork(String filePath) {
        intersections.clear();
        roads.clear();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filePath));
            doc.getDocumentElement().normalize();

            // ---- junctions ----
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

            // ---- edges ----
            NodeList edgeList = doc.getElementsByTagName("edge");
            for (int i = 0; i < edgeList.getLength(); i++) {
                Element e = (Element) edgeList.item(i);
                if ("internal".equals(e.getAttribute("function"))) continue;

                String id = e.getAttribute("id");
                Intersection from = intersections.get(e.getAttribute("from"));
                Intersection to   = intersections.get(e.getAttribute("to"));
                if (from == null || to == null) continue;

                int x1 = (int)Math.round(from.getX());
                int y1 = (int)Math.round(from.getY());
                int x2 = (int)Math.round(to.getX());
                int y2 = (int)Math.round(to.getY());

                Road r = new Road(id, from, to, x1, y1, x2, y2);

                // take shape of first lane if available
                NodeList lanes = e.getElementsByTagName("lane");
                if (lanes.getLength() > 0) {
                    Element lane0 = (Element) lanes.item(0);
                    String shapeStr = lane0.getAttribute("shape"); // "x1,y1 x2,y2 ..."
                    List<Point2D> poly = parseShape(shapeStr);
                    if (poly != null && poly.size() >= 2) r.setShape(poly);
                }

                roads.put(id, r);
                from.addOutgoing(r);
                to.addIncoming(r);
            }

            System.out.println("[SUMO] Loaded network: junctions=" + intersections.size() +
                               ", edges=" + roads.size());

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Parses a string as double, returning {@code def} when parsing fails. */
    private static double parseD(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }


    /**
     * Converts a SUMO lane {@code shape} attribute into a list of points.
     *
     * @param shapeAttr value of the {@code shape} attribute
     * @return list of points or {@code null} if parsing fails
     */
    private static List<Point2D> parseShape(String shapeAttr) {
        if (shapeAttr == null || shapeAttr.isEmpty()) return null;
        String[] toks = shapeAttr.trim().split("\\s+");
        List<Point2D> pts = new ArrayList<>(toks.length);
        for (String t : toks) {
            String[] xy = t.split(",");
            if (xy.length != 2) continue;
            try {
                double x = Double.parseDouble(xy[0]);
                double y = Double.parseDouble(xy[1]);
                pts.add(new Point2D.Double(x, y));
            } catch (NumberFormatException ignore) {}
        }
        return pts;
    }

    /** @return all intersections in the loaded network */
    public static Collection<Intersection> getIntersections() { return intersections.values(); }

    /** @return all roads in the loaded network */
    public static Collection<Road> getRoads() { return roads.values(); }

    /** @return unmodifiable mapping from road id to road instance */
    public static Map<String, Road> getRoadById() { return Collections.unmodifiableMap(roads); }
}
