package com.farmgame.core;

/**
 * Неизменяемая позиция клетки на сетке фермы.
 *
 * <p>Ось {@code x} идёт «вправо», ось {@code y} — «вглубь» поля.
 * Движок сам решает, как отобразить эти координаты в 3D.
 *
 * @param x номер столбца, начиная с 0
 * @param y номер строки, начиная с 0
 */
public record GridPosition(int x, int y) {

    /** Начало координат — левый ближний угол фермы. */
    public static final GridPosition ORIGIN = new GridPosition(0, 0);

    /** Манхэттенское расстояние (количество шагов по сетке) до другой клетки. */
    public int distanceTo(GridPosition other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
