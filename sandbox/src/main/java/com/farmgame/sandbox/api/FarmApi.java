package com.farmgame.sandbox.api;

import com.farmgame.core.crop.CropType;

/** Информация о ферме, доступная коду игрока. Только чтение. */
public interface FarmApi {

    /** Ширина поля (количество столбцов, ось X). */
    int getWidth();

    /** Высота поля (количество строк, ось Y). */
    int getHeight();

    /** Пуста ли грядка {@code (x, y)}. */
    boolean isEmpty(int x, int y);

    /** Созрел ли урожай на грядке {@code (x, y)}. */
    boolean isRipe(int x, int y);

    /** Сколько единиц культуры уже собрано на склад. */
    int getHarvested(CropType type);
}
