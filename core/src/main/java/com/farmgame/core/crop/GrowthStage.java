package com.farmgame.core.crop;

/** Стадия роста культуры на грядке. */
public enum GrowthStage {
    SEED,
    SPROUT,
    GROWING,
    RIPE;

    /**
     * Определяет стадию по доле роста.
     *
     * @param progress значение от 0.0 (только посажено) до 1.0 (созрело)
     */
    public static GrowthStage fromProgress(double progress) {
        if (progress >= 1.0) {
            return RIPE;
        }
        if (progress >= 0.5) {
            return GROWING;
        }
        if (progress >= 0.15) {
            return SPROUT;
        }
        return SEED;
    }
}
