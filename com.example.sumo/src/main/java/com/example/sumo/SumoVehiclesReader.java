package com.example.sumo;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

public class SumoVehiclesReader {

    public static List<Vehicle> readVehicles(String filePath){
        List<Vehicle> vehicles = new ArrayList<>();
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(new File(filePath));
            doc.getDocumentElement().normalize();

            NodeList vNodes = doc.getElementsByTagName("vehicle");
            for (int i = 0; i < vNodes.getLength(); i++) {
                Element vEl = (Element) vNodes.item(i);
                String vid = vEl.getAttribute("id");

                NodeList routeNodes = vEl.getElementsByTagName("route");
                if (routeNodes.getLength() == 0) continue;
                Element rEl = (Element) routeNodes.item(0);

                String edges = rEl.getAttribute("edges");
                if (edges == null || edges.trim().isEmpty()) continue;

                String[] edgeIds = edges.trim().split("\\s+");

                // Use an existing Vehicle constructor (no depart)
                vehicles.add(new Vehicle(vid, edgeIds));
                // If your Vehicle only has (String, List<String>), use:
                // vehicles.add(new Vehicle(vid, Arrays.asList(edgeIds)));
            }
            System.out.println("Loaded vehicles: " + vehicles.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vehicles;
    }
}
