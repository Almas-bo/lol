package com.farmgame.sandbox.api;

/**
 * Программа игрока. Игрок пишет класс, реализующий этот интерфейс:
 *
 * <pre>{@code
 * import com.farmgame.core.crop.*;
 * import com.farmgame.sandbox.api.*;
 *
 * public class MyFarm implements FarmProgram {
 *     @Override
 *     public void run(RobotApi robot, FarmApi farm) {
 *         robot.moveTo(2, 3);
 *         robot.plant(Crop.builder().setType(CropType.CORN).build());
 *         robot.water();
 *     }
 * }
 * }</pre>
 *
 * <p>У класса должен быть публичный конструктор без параметров.
 */
@FunctionalInterface
public interface FarmProgram {

    /**
     * Точка входа программы игрока.
     *
     * @param robot управление роботом
     * @param farm  информация о ферме (только чтение)
     */
    void run(RobotApi robot, FarmApi farm);
}
