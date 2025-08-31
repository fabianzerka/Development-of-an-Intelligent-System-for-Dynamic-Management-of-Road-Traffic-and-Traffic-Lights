package com.example.sumo;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class SumoReader {

    /** Kthen Map<edgeId, count> bazuar në atributet e disponueshme. */
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
                // përdor çfarëdo atribute që ekzistojnë
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

    private static int parseI(String s, int def){
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
