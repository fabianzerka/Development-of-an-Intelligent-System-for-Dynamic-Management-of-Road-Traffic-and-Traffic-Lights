package com.example.sumo;

public class Road {

    private String id;
    private Intersection from;
    private Intersection to;
    private int x1, y1, x2, y2;

    public Road(String id, Intersection from, Intersection to, int x1,int y1,int x2,int y2){
        this.id = id;
        this.from = from;
        this.to = to;
        this.x1 = x1; this.y1 = y1;
        this.x2 = x2; this.y2 = y2;
    }

    public String getId() { return id; }
    public Intersection getFrom() { return from; }
    public Intersection getTo() { return to; }

    public int getX1(){return x1;}
    public int getY1(){return y1;}
    public int getX2(){return x2;}
    public int getY2(){return y2;}
}
