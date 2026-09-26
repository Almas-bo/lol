package com.farmgame.core.crop;

/**
 * Вид культуры и его базовые характеристики.
 *
 * <p>Enum — хороший первый пример инкапсуляции данных: каждая константа хранит
 * собственные параметры, и добавление новой культуры не требует менять остальной код.
 */
public enum CropType {
    CORN("Кукуруза", 12.0, 3),
    WHEAT("Пшеница", 8.0, 2),
    CARROT("Морковь", 6.0, 1),
    PUMPKIN("Тыква", 20.0, 5);

    private final String displayName;
    private final double growthSeconds;
    private final int baseYield;

    CropType(String displayName, double growthSeconds, int baseYield) {
        this.displayName = displayName;
        this.growthSeconds = growthSeconds;
        this.baseYield = baseYield;
    }

    /** Название для интерфейса. */
    public String displayName() {
        return displayName;
    }

    /** Сколько игровых секунд нужно политой культуре, чтобы созреть. */
    public double growthSeconds() {
        return growthSeconds;
    }

    /** Урожай с одной грядки без удобрений. */
    public int baseYield() {
        return baseYield;
    }
}
