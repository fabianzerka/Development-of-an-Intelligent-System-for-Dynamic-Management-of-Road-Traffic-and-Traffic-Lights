package com.example.sumo;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class SumoReader {

    /**
     * Lexon fluksin e makinave për çdo edge nga edgeData.xml
     * Kthen Map<edgeId, numri i makinave>
     */
    public static Map<String,Integer> readTrafficFlows(String filePath){
        Map<String,Integer> flows = new HashMap<>();
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(filePath));
            doc.getDocumentElement().normalize();

            NodeList edges = doc.getElementsByTagName("edge");
            for(int i=0;i<edges.getLength();i++){
                Element edge = (Element) edges.item(i);
                String id = edge.getAttribute("id");
                int vehCount = 0;
                if(edge.hasAttribute("vehNr")){
                    vehCount = Integer.parseInt(edge.getAttribute("vehNr"));
                }
                flows.put(id, vehCount);
            }

        }catch(Exception e){
            e.printStackTrace();
        }
        return flows;
    }
}
