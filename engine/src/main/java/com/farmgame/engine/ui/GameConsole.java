package com.farmgame.engine.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.logging.Logger;

/**
 * Игровая консоль: хранит последние сообщения программы игрока.
 * Потокобезопасна — пишет поток игрока, читает поток рендеринга.
 */
public final class GameConsole {

    private static final Logger LOG = Logger.getLogger(GameConsole.class.getName());

    private final int capacity;
    private final Deque<String> lines = new ArrayDeque<>();

    public GameConsole(int capacity) {
        this.capacity = capacity;
    }

    public synchronized void print(String message) {
        LOG.info(message);
        lines.addLast(message);
        while (lines.size() > capacity) {
            lines.removeFirst();
        }
    }

    public synchronized List<String> lines() {
        return List.copyOf(lines);
    }
}
