package com.example.sumo;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class TrafficLight {

    public enum State { GREEN, RED }

    private final String id;
    private final Intersection intersection;

    // Grupi i ardhjeve: A ~ E–W, B ~ N–S (përafërsisht)
    private final Set<String> groupA = new HashSet<>();
    private final Set<String> groupB = new HashSet<>();

    // 0 = A green, 1 = B green
    private int phase;

    // ---- Fixed-time params (sekonda) ----
    private double greenA = 18.0;   // kohë jeshile për A
    private double greenB = 18.0;   // kohë jeshile për B
    private double allRed = 1.0;    // dritë e kuqe për të gjithë në tranzicion

    // timerat
    private double greenElapsed = 0.0;
    private boolean inAllRed = false;
    private double allRedElapsed = 0.0;

    // desinkronizim i lehtë mes nyjeve
    private final double jitter; // 0..2s

    public TrafficLight(String id, Intersection intersection) {
        this.id = id;
        this.intersection = intersection;
        autoGroupIncoming();

        Random rng = new Random(id.hashCode() ^ System.nanoTime());
        this.phase  = rng.nextBoolean() ? 0 : 1;        // start rastësor
        this.jitter = 2.0 * rng.nextDouble();           // 0..2s
        this.greenElapsed = rng.nextDouble() * (5.0 + jitter * 0.3);
    }

    public String getId()         { return id; }
    public Intersection getNode() { return intersection; }
    public int getPhase()         { return phase; }

    // ---- Setters opsionale për të rregulluar kohët në runtime ----
    public void setPlan(double greenA, double greenB, double allRed) {
        this.greenA = Math.max(3.0, greenA);
        this.greenB = Math.max(3.0, greenB);
        this.allRed = Math.max(0.5, allRed);
    }
    public void setGreenA(double s){ setPlan(s, this.greenB, this.allRed); }
    public void setGreenB(double s){ setPlan(this.greenA, s, this.allRed); }
    public void setAllRed(double s){ setPlan(this.greenA, this.greenB, s); }

    /** 0 për grupin A, 1 për grupin B. */
    public int groupOf(Road incoming) {
        return groupA.contains(incoming.getId()) ? 0 : 1;
    }

    /** Green për këtë hyrje tani? (gjatë all-red gjithmonë është RED) */
    public boolean isGreenFor(Road incoming) { 
        if (inAllRed) return false;
        return groupOf(incoming) == phase; 
    }
    public boolean isRedFor(Road incoming) { return !isGreenFor(incoming); }

    /** Shteti për dot-in në vizualizim (thjesht jeshil nëse faza 0, ndryshe kuq). */
    public State getState() { return (!inAllRed && phase == 0) ? State.GREEN : State.RED; }

    /** Loop i thjeshtë fixed-time: GREEN(phase) -> ALL_RED -> switch phase -> GREEN ... */
    public void updateFixed(double dt) {
        if (inAllRed) {
            allRedElapsed += dt;
            if (allRedElapsed >= allRed) {
                inAllRed = false;
                allRedElapsed = 0.0;
                greenElapsed = 0.0; // nis jeshilen e fazës tjetër
            }
            return;
        }

        greenElapsed += dt;
        double currGreen = (phase == 0 ? greenA : greenB);

        if (greenElapsed >= currGreen) {
            // kalim në ALL_RED pastaj ndërrim faze
            inAllRed = true;
            allRedElapsed = 0.0;
            phase = 1 - phase;
        }
    }

    /* ---------- ruajmë metodën adaptive për kompatibilitet, por mos e përdor ---------- */
    public void updateAdaptive(double dt, int dA, int dB) {
        // i zbrazët qëllimisht në fixed-time
        updateFixed(dt);
    }

    /* ---------- helpers ---------- */
    private void autoGroupIncoming() {
        groupA.clear(); groupB.clear();
        for (Road in : intersection.getIncoming()) {
            double dx = intersection.getX() - in.getX1();
            double dy = intersection.getY() - in.getY1();
            if (Math.abs(dx) >= Math.abs(dy)) groupA.add(in.getId()); // E–W
            else                               groupB.add(in.getId()); // N–S
        }
        if (groupA.isEmpty() || groupB.isEmpty()) {
            groupA.clear(); groupB.clear();
            int i = 0;
            for (Road in : intersection.getIncoming()) {
                ((i++ & 1) == 0 ? groupA : groupB).add(in.getId());
            }
        }
    }
}
