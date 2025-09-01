package com.example.sumo;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Utility class for reading aggregated traffic flow counts from SUMO generated
 * {@code edgeData.xml} files. The reader inspects available attributes on each
 * {@code <edge>} element and returns a map keyed by edge id containing the
 * parsed count. The class exposes only static helpers and cannot be
 * instantiated.
 */
public class SumoReader {

    /**
     * Parses a SUMO traffic flow file and returns a mapping from edge id to the
     * observed vehicle count. The method is tolerant to different attribute
     * names that SUMO may generate depending on the configured output options.
     *
     * @param filePath path to a SUMO {@code edgeData.xml} file
     * @return map of edge ids to vehicle counts (missing edges default to 0)
     */
    public static Map<String,Integer> readTrafficFlows(String filePath){
        Map<String,Integer> flows = new HashMap<>();
        try{
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(filePath));
            doc.getDocumentElement().normalize();

            NodeList edgeNodes = doc.getElementsByTagName("edge");
            for (int i = 0; i < edgeNodes.getLength(); i++) {
                Element e = (Element) edgeNodes.item(i);
                String id = e.getAttribute("id");
                int val = 0;
                // use whichever attribute SUMO provided
                if (e.hasAttribute("nVehContrib")) val = parseI(e.getAttribute("nVehContrib"), 0);
                else if (e.hasAttribute("nSamples")) val = parseI(e.getAttribute("nSamples"), 0);
                else if (e.hasAttribute("entered")) val = parseI(e.getAttribute("entered"), 0);
                else if (e.hasAttribute("vehNr")) val = parseI(e.getAttribute("vehNr"), 0);
                flows.put(id, val);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return flows;
    }

    /**
     * Safely parses an integer, returning the provided default value when the
     * input is malformed.
     *
     * @param s   string value to parse
     * @param def default value in case of parsing errors
     * @return parsed integer or the default value
     */
    private static int parseI(String s, int def){
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
