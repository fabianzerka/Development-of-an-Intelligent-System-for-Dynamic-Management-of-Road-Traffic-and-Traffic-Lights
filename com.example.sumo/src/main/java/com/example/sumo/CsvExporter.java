package com.example.sumo;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class CsvExporter {

    public static Path exportVehicleMetrics(Path outCsv, List<Vehicle> finished) throws IOException {
        if (outCsv.getParent() != null) Files.createDirectories(outCsv.getParent());
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outCsv))) {
            pw.println("id,start_time,finish_time,travel_time,waiting_time");
            for (Vehicle v : finished) {
                pw.printf("%s,%.3f,%.3f,%.3f,%.3f%n",
                        v.getId(),
                        v.getStartTime(),
                        v.getFinishedAt(),
                        v.getTravelTime(),
                        v.getWaitingTime());
            }
        }
        return outCsv;
    }
}
