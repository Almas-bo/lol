package com.farmgame.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GraphicsQualityTest {

    @Test
    void parsesQualityArgument() {
        assertEquals(GraphicsQuality.LOW, GraphicsQuality.fromArgs(new String[]{"--fullscreen", "--quality=low"}));
        assertEquals(GraphicsQuality.MEDIUM, GraphicsQuality.fromArgs(new String[]{"--quality=Medium"}));
    }

    @Test
    void defaultsToHighOnMissingOrUnknownValue() {
        assertEquals(GraphicsQuality.HIGH, GraphicsQuality.fromArgs(new String[0]));
        assertEquals(GraphicsQuality.HIGH, GraphicsQuality.fromArgs(new String[]{"--quality=ultra"}));
    }

    @Test
    void lowQualityDisablesExpensiveEffects() {
        assertFalse(GraphicsQuality.LOW.shadows());
        assertFalse(GraphicsQuality.LOW.postEffects());
    }
}
