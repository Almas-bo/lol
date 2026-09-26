package com.farmgame.core.resource;

import com.farmgame.core.crop.CropType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** Склад фермы: сколько урожая каждого вида собрано. */
public class Inventory {

    private final Map<CropType, Integer> harvested = new EnumMap<>(CropType.class);

    /** Добавляет урожай на склад. */
    public void add(CropType type, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Количество не может быть отрицательным: " + amount);
        }
        harvested.merge(type, amount, Integer::sum);
    }

    /** Очищает склад. */
    public void clear() {
        harvested.clear();
    }

    /** Сколько единиц данной культуры на складе. */
    public int count(CropType type) {
        return harvested.getOrDefault(type, 0);
    }

    /** Неизменяемый снимок содержимого склада. */
    public Map<CropType, Integer> snapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(harvested));
    }
}
