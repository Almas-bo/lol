package com.farmgame.sandbox.lesson;

/**
 * Результат проверки цели урока.
 *
 * @param passed  выполнена ли цель
 * @param message что именно не так (для игрока) или поздравление
 */
public record GoalResult(boolean passed, String message) {

    public static GoalResult pass(String message) {
        return new GoalResult(true, message);
    }

    public static GoalResult fail(String message) {
        return new GoalResult(false, message);
    }
}
