package com.farmgame.engine.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;

/**
 * Кнопка на панели HUD. Хранит свой прямоугольник (в координатах панели от левого
 * верхнего угла) для проверки попадания мышью.
 */
public record Button(String id, int x, int y, int width, int height, boolean enabled) {

    /** Значок слева от текста. */
    public enum Icon { NONE, PLAY, STOP, RESET, PREV, NEXT, BULB, KEY }

    public boolean contains(int px, int py) {
        return enabled && px >= x && py >= y && px < x + width && py < y + height;
    }

    /**
     * Рисует кнопку и возвращает её описание.
     *
     * @param fill основной цвет кнопки
     */
    public static Button draw(Graphics2D g, String id, String text, Icon icon, int x, int y, int height,
                              Color fill, boolean enabled) {
        g.setFont(HudStyle.BUTTON);
        FontMetrics fm = g.getFontMetrics();
        int iconW = icon == Icon.NONE ? 0 : text.isEmpty() ? 10 : 18;
        int width = fm.stringWidth(text) + iconW + 24;

        Color base = enabled ? fill : new Color(90, 96, 92);
        g.setColor(base);
        g.fillRoundRect(x, y, width, height, 10, 10);
        g.setColor(new Color(255, 255, 255, enabled ? 60 : 25));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, width - 1, height - 1, 10, 10);

        Color fg = enabled ? Color.WHITE : new Color(170, 175, 170);
        int cy = y + height / 2;
        drawIcon(g, icon, x + 12, cy, fg);
        g.setColor(fg);
        g.drawString(text, x + 12 + iconW, cy + fm.getAscent() / 2 - 2);
        return new Button(id, x, y, width, height, enabled);
    }

    /** Ширина кнопки без отрисовки — для выравнивания по правому краю. */
    public static int measure(Graphics2D g, String text, Icon icon) {
        g.setFont(HudStyle.BUTTON);
        return g.getFontMetrics().stringWidth(text) + (icon == Icon.NONE ? 0 : text.isEmpty() ? 10 : 18) + 24;
    }

    private static void drawIcon(Graphics2D g, Icon icon, int x, int cy, Color color) {
        g.setColor(color);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (icon) {
            case PLAY -> g.fillPolygon(new Polygon(new int[]{x, x + 10, x}, new int[]{cy - 6, cy, cy + 6}, 3));
            case STOP -> g.fillRect(x, cy - 5, 10, 10);
            case RESET -> {
                g.drawArc(x, cy - 6, 12, 12, 60, 270);
                g.fillPolygon(new Polygon(new int[]{x + 8, x + 13, x + 13}, new int[]{cy - 7, cy - 8, cy - 3}, 3));
            }
            case PREV -> g.drawPolyline(new int[]{x + 8, x + 2, x + 8}, new int[]{cy - 6, cy, cy + 6}, 3);
            case NEXT -> g.drawPolyline(new int[]{x + 2, x + 8, x + 2}, new int[]{cy - 6, cy, cy + 6}, 3);
            case BULB -> {
                g.drawOval(x + 1, cy - 8, 10, 10);
                g.drawLine(x + 4, cy + 5, x + 8, cy + 5);
            }
            case KEY -> {
                g.drawOval(x, cy - 5, 7, 7);
                g.drawLine(x + 7, cy - 1, x + 13, cy - 1);
                g.drawLine(x + 11, cy - 1, x + 11, cy + 3);
            }
            case NONE -> {
            }
        }
    }
}
