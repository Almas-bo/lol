package com.farmgame.core.farm;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.Crop;
import com.farmgame.core.crop.GrowthStage;

/**
 * Грядка — одна клетка поля. Изменяемое состояние доступно только классу {@link Farm}
 * (методы package-private); наружу отдаётся неизменяемый {@link PlotState}.
 */
final class Plot {

    private final GridPosition position;
    private Crop crop;
    private double growthProgress;
    private boolean watered;

    Plot(GridPosition position) {
        this.position = position;
    }

    boolean isEmpty() {
        return crop == null;
    }

    boolean isRipe() {
        return crop != null && growthProgress >= 1.0;
    }

    void plant(Crop newCrop) {
        this.crop = newCrop;
        this.growthProgress = 0.0;
        this.watered = false;
    }

    void water() {
        this.watered = true;
    }

    /** Растёт только политая культура. */
    void grow(double seconds) {
        if (crop == null || !watered || growthProgress >= 1.0) {
            return;
        }
        growthProgress = Math.min(1.0, growthProgress + seconds / crop.type().growthSeconds());
    }

    Crop clear() {
        Crop removed = crop;
        crop = null;
        growthProgress = 0.0;
        watered = false;
        return removed;
    }

    PlotState snapshot() {
        GrowthStage stage = crop == null ? null : GrowthStage.fromProgress(growthProgress);
        return new PlotState(position, crop, growthProgress, watered, stage);
    }
}
