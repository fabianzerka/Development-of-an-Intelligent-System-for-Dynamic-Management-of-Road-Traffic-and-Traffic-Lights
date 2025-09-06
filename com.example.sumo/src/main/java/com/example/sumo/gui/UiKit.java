package com.example.sumo.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/** Small, dependency-free UI kit for a modern dark toolbar and buttons. */
public final class UiKit {

    private UiKit(){}

    // --- Palette ---
    public static final Color BG_DARK     = new Color(0x23272F);
    public static final Color BG_DARKER   = new Color(0x1B1F27);
    public static final Color FG_MAIN     = new Color(0xE6EAF2);
    public static final Color FG_MUTED    = new Color(0xA9B2C3);
    public static final Color ACCENT      = new Color(0x2BB3F3);   // cyan
    public static final Color ACCENT_DARK = new Color(0x1E8FC1);
    public static final Color DANGER      = new Color(0xE85D75);
    public static final Color OK          = new Color(0x3AC569);
    public static final Color SEPARATOR   = new Color(255,255,255,30);

    /** Apply a dark theme to Swing defaults (safe, no external LAF). */
    public static void applyDarkDefaults() {
        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("OptionPane.background", BG_DARK);
        UIManager.put("OptionPane.messageForeground", FG_MAIN);
        UIManager.put("Label.foreground", FG_MAIN);
        UIManager.put("Button.foreground", FG_MAIN);
        UIManager.put("Spinner.background", BG_DARKER);
        UIManager.put("Spinner.foreground", FG_MAIN);
        UIManager.put("TextField.background", BG_DARKER);
        UIManager.put("TextField.foreground", FG_MAIN);

        // ensure OptionPane buttons show text and have readable colors
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("Button.background", BG_DARKER);
        UIManager.put("Button.foreground", FG_MAIN);

        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
    }


    // ---------- Buttons ----------
    public enum BtnKind { PRIMARY, GHOST, DANGER }

    // Functional painter + wrapper to a real Icon
    private interface PaintLambda {
        void paint(Component c, Graphics2D g, int x, int y, int sz);
    }
    private static Icon makeIcon(int sz, PaintLambda painter) {
        return new Icon() {
            public int getIconWidth()  { return sz; }
            public int getIconHeight() { return sz; }
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                painter.paint(c, g2, x, y, sz);
                g2.dispose();
            }
        };
    }

    /** Rounded button with subtle hover; use with one of the icon() helpers. */
    public static JButton button(String text, Icon icon, BtnKind kind) {
        JButton b = new JButton(text, icon) {
            boolean hover=false, press=false;
            @Override protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                Color base;
                switch (kind) {
                    case PRIMARY: base = ACCENT; break;
                    case DANGER:  base = DANGER; break;
                    default:      base = new Color(255,255,255,20);
                }
                Color fill = base;
                if (kind==BtnKind.GHOST) {
                    fill = hover ? new Color(255,255,255,30) : new Color(255,255,255,18);
                } else {
                    fill = press ? shade(base, -0.12) : (hover ? shade(base, -0.06) : base);
                }
                g.setColor(fill);
                g.fillRoundRect(0,0,w,h,16,16);
                if (kind==BtnKind.GHOST) {
                    g.setColor(new Color(255,255,255,40));
                    g.drawRoundRect(0,0,w-1,h-1,16,16);
                }
                super.paintComponent(g0); // text+icon layout
                g.dispose();
            }
            @Override public void updateUI() {
                super.updateUI();
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setFocusable(false);
            }
            {
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(6,10,6,12));
                setForeground(kind==BtnKind.GHOST ? FG_MAIN : Color.WHITE);
                setFont(getFont().deriveFont(Font.BOLD));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new java.awt.event.MouseAdapter(){
                    public void mouseEntered(java.awt.event.MouseEvent e){ hover=true; repaint();}
                    public void mouseExited (java.awt.event.MouseEvent e){ hover=false; repaint();}
                    public void mousePressed(java.awt.event.MouseEvent e){ press=true; repaint();}
                    public void mouseReleased(java.awt.event.MouseEvent e){ press=false; repaint();}
                });
                setIconTextGap(8);
            }
        };
        return b;
    }

    private static Color shade(Color c, double f) {
        int r=(int)Math.max(0,Math.min(255,c.getRed()  *(1+f)));
        int g=(int)Math.max(0,Math.min(255,c.getGreen()*(1+f)));
        int b=(int)Math.max(0,Math.min(255,c.getBlue() *(1+f)));
        return new Color(r,g,b,c.getAlpha());
    }

    // ---------- Icons (vector, sharp on any DPI) ----------
    public static Icon playIcon(int sz) {
        return makeIcon(sz, (c, g, x, y, s) -> {
            g.setColor(Color.WHITE);
            Polygon tri = new Polygon(
                new int[]{x + 3, x + 3, x + s - 3},
                new int[]{y + 3, y + s - 3, y + s / 2}, 3);
            g.fillPolygon(tri);
        });
    }
    public static Icon pauseIcon(int sz) {
        return makeIcon(sz, (c, g, x, y, s) -> {
            g.setColor(Color.WHITE);
            int w = Math.max(2, s / 4);
            int gap = Math.max(2, w / 2);
            g.fillRoundRect(x + 3,         y + 3, w, s - 6, 3, 3);
            g.fillRoundRect(x + 3 + w+gap, y + 3, w, s - 6, 3, 3);
        });
    }
    public static Icon resetIcon(int sz) {
        return makeIcon(sz, (c, g, x, y, s) -> {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int r  = s/2 - 3;
            int cx = x + s/2, cy = y + s/2;
            g.drawArc(cx - r, cy - r, 2 * r, 2 * r, 45, 270);
            Path2D arrow = new Path2D.Double();
            arrow.moveTo(cx + r - 1, cy - 2);
            arrow.lineTo(cx + r - 6, cy - 7);
            arrow.lineTo(cx + r - 8, cy + 1);
            arrow.closePath();
            g.fill(arrow);
        });
    }
    public static Icon downloadIcon(int sz) {
        return makeIcon(sz, (c, g, x, y, s) -> {
            g.setColor(Color.WHITE);
            int cx = x + s/2;
            int top = y + 3, bot = y + s - 5;
            g.fillRect(cx - 1, top, 2, bot - top - 5);
            Polygon arr = new Polygon(new int[]{cx - 6, cx, cx + 6},
                                      new int[]{bot - 7, bot - 1, bot - 7}, 3);
            g.fillPolygon(arr);
            g.fillRoundRect(x + 3, y + s - 5, s - 6, 2, 2, 2);
        });
    }
    public static Icon lightIcon(int sz) {
        return makeIcon(sz, (c, g, x, y, s) -> {
            g.setColor(OK);
            g.fillOval(x + 2, y + 2, s - 4, s - 4);
            g.setColor(new Color(0, 0, 0, 40));
            g.drawOval(x + 2, y + 2, s - 4, s - 4);
        });
    }

    // --- Toolbar container ---
    public static JComponent topBar() {
        JPanel p = new JPanel(new BorderLayout()){
            @Override protected void paintComponent(Graphics g0){
                super.paintComponent(g0);
                Graphics2D g=(Graphics2D)g0;
                g.setPaint(new GradientPaint(0,0,BG_DARKER,0,getHeight(),BG_DARK));
                g.fillRect(0,0,getWidth(),getHeight());
                g.setColor(SEPARATOR);
                g.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);
            }
        };
        p.setOpaque(true);
        p.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        return p;
    }

    /** Lightweight label used for tiny badges like "5x" or "8". */
    public static JLabel badge(String text, Color bg){
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setOpaque(true);
        l.setBackground(bg);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(new Color(255,255,255,40)),
                BorderFactory.createEmptyBorder(2,8,2,8)));
        return l;
    }
    private static class RoundedBorder extends javax.swing.border.AbstractBorder{
        private final Color c; RoundedBorder(Color c){this.c=c;}
        @Override public void paintBorder(Component c0, Graphics g, int x, int y, int w, int h){
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(c); ((Graphics2D)g).drawRoundRect(x,y,w-1,h-1,12,12);
        }
    }
}
