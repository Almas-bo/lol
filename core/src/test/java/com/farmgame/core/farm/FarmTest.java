package com.farmgame.core.farm;

import com.farmgame.core.FarmRuleException;
import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.Crop;
import com.farmgame.core.crop.CropType;
import com.farmgame.core.crop.GrowthStage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmTest {

    private final Farm farm = new Farm(4, 3);

    @Test
    void robotCannotLeaveTheFarm() {
        assertThrows(FarmRuleException.class, () -> farm.moveRobot(new GridPosition(4, 0)));
        assertEquals(GridPosition.ORIGIN, farm.robotPosition());
    }

    @Test
    void robotCountsSteps() {
        farm.moveRobot(new GridPosition(2, 1));
        farm.moveRobot(new GridPosition(0, 0));

        assertEquals(6, farm.robotSteps());
    }

    @Test
    void cropGrowsOnlyWhenWatered() {
        farm.plantAtRobot(Crop.of(CropType.CARROT));
        farm.tick(100);
        assertEquals(GrowthStage.SEED, farm.plotAt(GridPosition.ORIGIN).stage());

        farm.waterAtRobot();
        farm.tick(CropType.CARROT.growthSeconds());
        assertTrue(farm.plotAt(GridPosition.ORIGIN).isRipe());
    }

    @Test
    void harvestMovesYieldToInventory() {
        farm.plantAtRobot(Crop.builder().setType(CropType.CORN).setFertilized(true).build());
        assertEquals(0, farm.harvestAtRobot(), "незрелую культуру собрать нельзя");

        farm.waterAtRobot();
        farm.tick(CropType.CORN.growthSeconds());

        assertEquals(CropType.CORN.baseYield() * 2, farm.harvestAtRobot());
        assertEquals(CropType.CORN.baseYield() * 2, farm.inventorySnapshot().get(CropType.CORN));
        assertTrue(farm.plotAt(GridPosition.ORIGIN).isEmpty());
    }

    @Test
    void cannotPlantTwice() {
        farm.plantAtRobot(Crop.of(CropType.WHEAT));
        assertThrows(FarmRuleException.class, () -> farm.plantAtRobot(Crop.of(CropType.CORN)));
    }

    @Test
    void cannotWaterEmptyPlot() {
        assertThrows(FarmRuleException.class, farm::waterAtRobot);
        assertFalse(farm.plotAt(GridPosition.ORIGIN).watered());
    }
}
