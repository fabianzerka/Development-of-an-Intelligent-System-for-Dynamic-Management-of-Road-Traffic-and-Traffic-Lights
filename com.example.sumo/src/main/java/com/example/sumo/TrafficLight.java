package com.example.sumo;

import java.util.ArrayList;
import java.util.List;

public class TrafficLight {

    public enum State { GREEN, RED }

    private final String id;
    private final Intersection inter;

    // Grupi 0 (horizontal) dhe grupi 1 (vertical) – auto-popullohen nga incoming
    private final List<Road> group0 = new ArrayList<>();
    private final List<Road> group1 = new ArrayList<>();

    private int phase = 0;           // 0 => group0 jeshile, 1 => group1 jeshile
    private double timer = 0.0;
    private double greenDur = 8.0;  // sekonda (mund të ndryshohet në runtime)

    public TrafficLight(String id, Intersection inter) {
        this.id = id;
        this.inter = inter;
        autoGroupIncoming(inter);
    }

    public String getId() { return id; }
    public Intersection getIntersection() { return inter; }

    /** Thirret çdo tick (dt në sekonda). */
    public void update(double dt) {
        if (group0.isEmpty() && group1.isEmpty()) return;
        timer += dt;
        if (timer >= greenDur) {
            timer = 0.0;
            phase = 1 - phase;
        }
    }

    /** Jeshile për këtë rrugë hyrëse? */
    public boolean isGreenFor(Road incoming) {
        if (incoming == null) return true;
        if (group0.isEmpty() && group1.isEmpty()) return true;
        return (phase == 0 ? group0 : group1).contains(incoming);
    }

    public boolean isRedFor(Road incoming) { return !isGreenFor(incoming); }

    /** Alias i përgjithshëm për vizualizim (jeshile nëse të paktën një hyrje e lejuar). */
    public State getState() {
        if (inter == null || inter.getIncoming().isEmpty()) return State.GREEN;
        for (Road r : inter.getIncoming()) if (isGreenFor(r)) return State.GREEN;
        return State.RED;
    }

    // ==== Ndihmëse për adaptimin ====
    public int groupOf(Road r) {
        if (r == null) return -1;
        if (group0.contains(r)) return 0;
        if (group1.contains(r)) return 1;
        return -1;
    }
    public int getPhase() { return phase; }
    public void forcePhase(int p) { if (p == 0 || p == 1) { phase = p; timer = 0; } }
    public void setGreenDuration(double seconds) { if (seconds > 0) this.greenDur = seconds; }

    // ==== Grupimi automatik N–S / E–W sipas këndit të rrugës ====
    private void autoGroupIncoming(Intersection inter) {
        if (inter == null) return;
        for (Road r : inter.getIncoming()) {
            double dx = r.getX2() - r.getX1();
            double dy = r.getY2() - r.getY1();
            if (Math.abs(dx) >= Math.abs(dy)) group0.add(r); else group1.add(r);
        }
    }
}
