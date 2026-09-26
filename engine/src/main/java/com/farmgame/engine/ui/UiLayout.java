package com.farmgame.engine.ui;

/**
 * Раскладка интерфейса по размеру экрана. Все координаты — в пикселях jME:
 * начало в ЛЕВОМ НИЖНЕМ углу, (x, y) — левый нижний угол панели.
 *
 * <pre>
 * ┌──────────┬───────────┬─────────────┐
 * │  Урок    │  [ферма]  │             │
 * │          │           │  Редактор   │
 * ├──────────┤   3D      │    кода     │
 * │ Консоль  │           │             │
 * └──────────┴───────────┴─────────────┘
 * </pre>
 */
public record UiLayout(int screenWidth, int screenHeight,
                       Rect lesson, Rect console, Rect editor, Rect farm, float viewCenterNdc) {

    public static final int MARGIN = 12;

    /** Прямоугольник панели. */
    public record Rect(int x, int y, int width, int height) {
    }

    public static UiLayout forScreen(int w, int h) {
        int lessonW = clamp(Math.round(w * 0.29f), 330, 480);
        int editorW = clamp(Math.round(w * 0.40f), 440, 780);
        int consoleH = clamp(Math.round(h * 0.27f), 150, 250);
        int lessonH = h - consoleH - 3 * MARGIN;
        int editorH = h - 2 * MARGIN;

        int middleLeft = 2 * MARGIN + lessonW;
        int middleRight = w - 2 * MARGIN - editorW;
        int farmW = Math.min(260, Math.max(0, middleRight - middleLeft));
        int farmH = 150;
        int farmX = middleLeft + (middleRight - middleLeft - farmW) / 2;

        // Центр свободной области в NDC (-1..1): туда камера "смотрит", чтобы ферма не пряталась под панелями.
        float middleCenter = (middleLeft + middleRight) / 2f;
        float ndc = 2f * middleCenter / w - 1f;

        return new UiLayout(w, h,
                new Rect(MARGIN, 2 * MARGIN + consoleH, lessonW, lessonH),
                new Rect(MARGIN, MARGIN, lessonW, consoleH),
                new Rect(w - MARGIN - editorW, MARGIN, editorW, editorH),
                new Rect(farmX, h - MARGIN - farmH, farmW, farmH),
                ndc);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
