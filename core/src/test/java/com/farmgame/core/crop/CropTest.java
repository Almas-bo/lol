package com.farmgame.core.crop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CropTest {

    @Test
    void builderCreatesCropWithDefaults() {
        Crop crop = Crop.builder().setType(CropType.CORN).build();

        assertEquals(CropType.CORN, crop.type());
        assertEquals(CropType.CORN.displayName(), crop.name());
        assertEquals(CropType.CORN.baseYield(), crop.expectedYield());
    }

    @Test
    void fertilizerDoublesYield() {
        Crop crop = Crop.builder().setType(CropType.WHEAT).setName("Поле №1").setFertilized(true).build();

        assertTrue(crop.fertilized());
        assertEquals(CropType.WHEAT.baseYield() * 2, crop.expectedYield());
    }

    @Test
    void buildWithoutTypeFails() {
        assertThrows(IllegalStateException.class, () -> Crop.builder().setName("Без типа").build());
    }
}
