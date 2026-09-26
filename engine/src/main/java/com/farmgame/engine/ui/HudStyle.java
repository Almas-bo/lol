package com.farmgame.engine.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.RoundRectangle2D;

/** Общий стиль панелей: полупрозрачный тёмный фон, скруглённые углы, шрифты. */
public final class HudStyle {

    public static final Font TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 17);
    public static final Font TEXT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    public static final Font MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);
    public static final Font SMALL = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
    public static final Font KEY = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    public static final Color TEXT_COLOR = new Color(0xF2, 0xF0, 0xE6);
    public static final Color MUTED = new Color(0xB8, 0xC4, 0xB0);
    public static final Color ACCENT = new Color(0xFF, 0xC8, 0x3D);
    public static final Color ERROR = new Color(0xFF, 0x8A, 0x7A);

    private static final Color BACKGROUND = new Color(22, 30, 24, 190);
    private static final Color BORDER = new Color(255, 255, 255, 50);

    private HudStyle() {
    }

    /** Фон панели на весь холст. */
    public static void background(Graphics2D g, int width, int height) {
        RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, width - 3, height - 3, 16, 16);
        g.setColor(BACKGROUND);
        g.fill(shape);
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(shape);
    }

    public static void title(Graphics2D g, String text, int x, int y) {
        g.setFont(TITLE);
        g.setColor(ACCENT);
        g.drawString(text, x, y);
    }
}
