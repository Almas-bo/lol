package com.farmgame.engine.scene;

import com.farmgame.core.GridPosition;
import com.jme3.math.Vector3f;

/**
 * Перевод координат сетки фермы в мировые координаты jME.
 *
 * <p>Клетка {@code (x, y)} отображается в точку {@code (x * CELL_SIZE, 0, y * CELL_SIZE)}:
 * ось Y логики фермы соответствует оси Z в 3D, так как Y в jME направлена вверх.
 */
public final class FarmCoordinates {

    /** Размер одной клетки в мировых единицах. */
    public static final float CELL_SIZE = 2f;

    private final int width;
    private final int height;

    public FarmCoordinates(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** Центр клетки на уровне земли. */
    public Vector3f toWorld(GridPosition p) {
        return new Vector3f(p.x() * CELL_SIZE, 0f, p.y() * CELL_SIZE);
    }

    /** Геометрический центр всего поля. */
    public Vector3f center() {
        return new Vector3f((width - 1) * CELL_SIZE / 2f, 0f, (height - 1) * CELL_SIZE / 2f);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
