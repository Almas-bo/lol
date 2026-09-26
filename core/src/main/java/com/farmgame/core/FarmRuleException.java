package com.farmgame.core;

/**
 * Нарушение игровых правил: выход за границы поля, посадка на занятую грядку и т.п.
 *
 * <p>Это «ожидаемая» ошибка игрока, а не баг движка, поэтому исключение непроверяемое
 * и содержит понятное сообщение, которое можно показать в игровой консоли.
 */
public class FarmRuleException extends RuntimeException {

    public FarmRuleException(String message) {
        super(message);
    }
}
