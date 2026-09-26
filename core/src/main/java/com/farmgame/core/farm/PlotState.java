package com.farmgame.core.farm;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.Crop;
import com.farmgame.core.crop.GrowthStage;

/**
 * Неизменяемый снимок грядки. Безопасно передавать в другие потоки (движок, песочница).
 *
 * @param position       клетка
 * @param crop           посаженная культура или {@code null}, если грядка пуста
 * @param growthProgress рост от 0.0 до 1.0
 * @param watered        полита ли грядка
 * @param stage          стадия роста или {@code null}, если грядка пуста
 */
public record PlotState(GridPosition position, Crop crop, double growthProgress, boolean watered,
                        GrowthStage stage) {

    public boolean isEmpty() {
        return crop == null;
    }

    public boolean isRipe() {
        return stage == GrowthStage.RIPE;
    }
}
