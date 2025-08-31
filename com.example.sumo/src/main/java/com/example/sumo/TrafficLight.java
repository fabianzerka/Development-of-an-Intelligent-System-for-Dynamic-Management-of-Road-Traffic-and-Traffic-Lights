package com.example.sumo;

import java.util.ArrayList;
import java.util.List;

public class TrafficLight {

    public enum State { GREEN, RED }

    private final String id;
    private final Intersection inter;

    // Dy grupe fazash: horizontale (group0) vs vertikale (group1)
    private final List<Road> group0 = new ArrayList<>();
    private final List<Road> group1 = new ArrayList<>();

    private int phase = 0;          // 0 => group0 jeshile, 1 => group1 jeshile
    private double timer = 0.0;
    private double greenDur = 10.0; // sekonda; rregullo sipas dëshirës

    public TrafficLight(String id, Intersection inter) {
        this.id = id;
        this.inter = inter;
        autoGroupIncoming(inter); // grupim automatik N–S / E–W sipas këndit
    }

    public String getId() { return id; }
    public Intersection getIntersection() { return inter; }

    /** Thirret çdo tick (dt në sekonda) për të ndërruar fazat. */
    public void update(double dt) {
        if (group0.isEmpty() && group1.isEmpty()) return; // s’ka hyrje për t’u kontrolluar
        timer += dt;
        if (timer >= greenDur) {
            timer = 0.0;
            phase = 1 - phase;
        }
    }

    /** Jeshile për këtë rrugë hyrëse? */
    public boolean isGreenFor(Road incoming) {
        if (incoming == null) return true;
        if (group0.isEmpty() && group1.isEmpty()) return true; // pa kufizime
        return (phase == 0 ? group0 : group1).contains(incoming);
    }

    /** E kuqe për këtë rrugë hyrëse? */
    public boolean isRedFor(Road incoming) {
        return !isGreenFor(incoming);
    }

    /**
     * Alias i përgjithshëm për panele që duan vetëm një ngjyrë “totale”.
     * Kthen GREEN nëse të paktën një nga rrugët hyrëse të këtij kryqëzimi është jeshile në fazën aktuale,
     * përndryshe RED.
     */
    public State getState() {
        if (inter == null) return State.GREEN;
        if (inter.getIncoming().isEmpty()) return State.GREEN; // s’ka hyrje → mos e blloko
        for (Road r : inter.getIncoming()) {
            if (isGreenFor(r)) return State.GREEN;
        }
        return State.RED;
    }

    // --- Grupim automatik: horizontale në group0, vertikale në group1 ---
    private void autoGroupIncoming(Intersection inter) {
        if (inter == null) return;
        for (Road r : inter.getIncoming()) {
            double dx = r.getX2() - r.getX1();
            double dy = r.getY2() - r.getY1();
            if (Math.abs(dx) >= Math.abs(dy)) group0.add(r); else group1.add(r);
        }
        // nëse vetëm një grup u mbush, funksionon si “jeshile gjithmonë” për atë drejtim
    }

    // Opsionale: nëse do të ndryshosh kohëzgjatjen nga jashtë
    public void setGreenDuration(double seconds) {
        if (seconds > 0) this.greenDur = seconds;
    }
}
