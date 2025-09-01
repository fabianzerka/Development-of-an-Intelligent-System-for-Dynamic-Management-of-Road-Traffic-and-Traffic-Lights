package com.example.sumo;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Utility for exporting per-vehicle metrics to a CSV file. Each row contains the
 * vehicle id along with start, finish, travel and waiting times.
 */
public class CsvExporter {

	/**
     * Writes the metrics of completed vehicles to the specified output file.
     * The parent directory is created automatically if missing.
     *
     * @param outCsv   destination CSV file
     * @param finished list of completed vehicles
     * @return the output path for convenience
     * @throws IOException if writing fails
     */
    public static Path exportVehicleMetrics(Path outCsv, List<Vehicle> finished) throws IOException {
        if (outCsv.getParent() != null) Files.createDirectories(outCsv.getParent());
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outCsv))) {
        	pw.println("id,start_time,finish_time,travel_time,waiting_time,stops_count");
        	for (Vehicle v : finished) {
        	    pw.printf(Locale.US, "%s,%.3f,%.3f,%.3f,%.3f,%d%n",
        	            v.getId(),
        	            v.getStartTime(),
        	            v.getFinishedAt(),
        	            v.getTravelTime(),
        	            v.getWaitingTime(),
        	            v.getStops());
        	}
        }
        return outCsv;
    }
}
