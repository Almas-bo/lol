package com.farmgame.sandbox.api;

/**
 * Робот не смог выполнить команду игрока. Сообщение объясняет причину
 * и предназначено для показа игроку.
 */
public class RobotActionException extends RuntimeException {

    public RobotActionException(String message) {
        super(message);
    }
}
