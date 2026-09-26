package com.farmgame.sandbox.runtime;

import java.time.Duration;

/**
 * Итог выполнения программы игрока.
 *
 * @param status  чем закончилось выполнение
 * @param message пояснение для игрока (текст ошибки) или пустая строка
 * @param elapsed сколько длилось выполнение
 */
public record ExecutionResult(Status status, String message, Duration elapsed) {

    public enum Status {
        /** Программа завершилась без ошибок. */
        SUCCESS,
        /** Код игрока бросил исключение. */
        ERROR,
        /** Программа превысила лимит времени и была остановлена. */
        TIMEOUT
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
