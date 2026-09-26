package com.farmgame.sandbox.command;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.Crop;
import com.farmgame.core.farm.Farm;

/**
 * Одна команда роботу (паттерн <b>Command</b>). Запечатанный интерфейс перечисляет
 * все возможные команды, что позволяет движку обрабатывать их через {@code switch}
 * с проверкой полноты на этапе компиляции.
 */
public sealed interface RobotCommand {

    /**
     * Применяет команду к логике фермы.
     *
     * @return числовой результат (например, объём урожая) или 0
     */
    int applyTo(Farm farm);

    /** Переместиться в клетку. */
    record MoveTo(GridPosition target) implements RobotCommand {
        @Override
        public int applyTo(Farm farm) {
            farm.moveRobot(target);
            return 0;
        }
    }

    /** Посадить культуру под роботом. */
    record Plant(Crop crop) implements RobotCommand {
        @Override
        public int applyTo(Farm farm) {
            farm.plantAtRobot(crop);
            return 0;
        }
    }

    /** Полить грядку под роботом. */
    record Water() implements RobotCommand {
        @Override
        public int applyTo(Farm farm) {
            farm.waterAtRobot();
            return 0;
        }
    }

    /** Собрать урожай под роботом. */
    record Harvest() implements RobotCommand {
        @Override
        public int applyTo(Farm farm) {
            return farm.harvestAtRobot();
        }
    }

    /**
     * Подождать указанное игровое время (например, пока растут культуры).
     * В {@link DirectCommandSink} время не идёт — команда завершается сразу.
     */
    record Pause(double seconds) implements RobotCommand {
        public Pause {
            if (seconds < 0 || Double.isNaN(seconds)) {
                throw new IllegalArgumentException("Время ожидания должно быть неотрицательным: " + seconds);
            }
        }

        @Override
        public int applyTo(Farm farm) {
            return 0;
        }
    }
}
