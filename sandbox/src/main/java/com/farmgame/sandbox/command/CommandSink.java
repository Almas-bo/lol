package com.farmgame.sandbox.command;

/**
 * Куда отправляются команды робота.
 *
 * <p>Разные реализации позволяют запускать один и тот же код игрока
 * и в 3D-игре ({@link QueuedCommandSink}), и в тестах без графики ({@link DirectCommandSink}).
 */
public interface CommandSink {

    /**
     * Выполняет команду и ждёт её завершения.
     *
     * @return результат {@link RobotCommand#applyTo}
     * @throws com.farmgame.core.FarmRuleException если команда нарушает правила игры
     * @throws InterruptedException                 если выполнение программы остановлено
     */
    int execute(RobotCommand command) throws InterruptedException;
}
