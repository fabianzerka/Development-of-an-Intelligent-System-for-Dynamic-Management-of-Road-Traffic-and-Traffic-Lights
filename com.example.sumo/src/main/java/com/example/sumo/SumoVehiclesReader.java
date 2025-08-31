package com.example.sumo;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.util.*;

public class SumoVehiclesReader {

    public static List<Vehicle> readVehicles(String filePath){
        List<Vehicle> vehicles = new ArrayList<>();
        try{
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new File(filePath));
            doc.getDocumentElement().normalize();

            NodeList vehicleList = doc.getElementsByTagName("vehicle");
            for(int i=0;i<vehicleList.getLength();i++){
                Element veh = (Element) vehicleList.item(i);
                String id = veh.getAttribute("id");
                String[] route = veh.getAttribute("route").split(" ");
                vehicles.add(new Vehicle(id, route));
            }

            System.out.println("=== Loaded Vehicles ===");
            for(Vehicle v : vehicles) {
                System.out.println(v.getId() + " route=" + String.join(",", v.getId()));
            }
            System.out.println("Total vehicles: " + vehicles.size());

            
        }catch(Exception e){ e.printStackTrace(); }
        return vehicles;
    }
}
