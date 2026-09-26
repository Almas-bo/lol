package com.farmgame.core.entity;

import com.farmgame.core.GridPosition;

import java.util.Objects;

/**
 * Базовый класс для всего, что стоит на поле: роботы, поливалки, сборщики.
 *
 * <p>Учебный пример наследования и инкапсуляции: позиция хранится в {@code private}-поле,
 * а подклассы меняют её только через {@link #setPosition(GridPosition)}.
 */
public abstract class Entity {

    private final String id;
    private GridPosition position;

    protected Entity(String id, GridPosition position) {
        this.id = Objects.requireNonNull(id, "id");
        this.position = Objects.requireNonNull(position, "position");
    }

    public String id() {
        return id;
    }

    public GridPosition position() {
        return position;
    }

    protected void setPosition(GridPosition position) {
        this.position = Objects.requireNonNull(position, "position");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + id + " @ " + position + "]";
    }
}
