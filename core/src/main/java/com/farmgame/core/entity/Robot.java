package com.farmgame.core.entity;

import com.farmgame.core.GridPosition;

/**
 * Фермерский робот. Сам по себе умеет только перемещаться;
 * работу с грядками выполняет {@link com.farmgame.core.farm.Farm}, проверяя правила.
 */
public class Robot extends Entity {

    private int stepsTaken;

    public Robot(String id, GridPosition start) {
        super(id, start);
    }

    /**
     * Перемещает робота. Проверка границ поля — ответственность фермы.
     *
     * @param target новая клетка
     */
    public void moveTo(GridPosition target) {
        stepsTaken += position().distanceTo(target);
        setPosition(target);
    }

    /** Ставит робота в клетку без учёта пройденного пути и обнуляет счётчик шагов (новый запуск). */
    public void placeAt(GridPosition start) {
        setPosition(start);
        stepsTaken = 0;
    }

    /** Общее число пройденных клеток — пригодится для статистики и заданий. */
    public int stepsTaken() {
        return stepsTaken;
    }
}
