package com.farmgame.sandbox.runtime;

import com.farmgame.core.FarmRuleException;
import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;
import com.farmgame.core.farm.PlotState;
import com.farmgame.sandbox.api.FarmApi;
import com.farmgame.sandbox.api.RobotActionException;

import java.util.Objects;

/** Реализация {@link FarmApi}: доступ к ферме только для чтения. */
public final class FarmView implements FarmApi {

    private final Farm farm;

    public FarmView(Farm farm) {
        this.farm = Objects.requireNonNull(farm, "farm");
    }

    @Override
    public int getWidth() {
        return farm.width();
    }

    @Override
    public int getHeight() {
        return farm.height();
    }

    @Override
    public boolean isEmpty(int x, int y) {
        return plot(x, y).isEmpty();
    }

    @Override
    public boolean isRipe(int x, int y) {
        return plot(x, y).isRipe();
    }

    @Override
    public int getHarvested(CropType type) {
        return farm.inventorySnapshot().getOrDefault(type, 0);
    }

    private PlotState plot(int x, int y) {
        try {
            return farm.plotAt(new GridPosition(x, y));
        } catch (FarmRuleException e) {
            throw new RobotActionException(e.getMessage());
        }
    }
}
