package com.farmgame.sandbox.runtime;

import java.util.concurrent.CompletableFuture;

/**
 * Запущенная программа игрока: её будущий результат и возможность остановить её.
 */
public final class RunningProgram {

    private final Thread worker;
    private final CompletableFuture<ExecutionResult> result;

    RunningProgram(Thread worker, CompletableFuture<ExecutionResult> result) {
        this.worker = worker;
        this.result = result;
    }

    /** Результат выполнения; future никогда не завершается исключением. */
    public CompletableFuture<ExecutionResult> result() {
        return result;
    }

    public boolean isRunning() {
        return !result.isDone();
    }

    /**
     * Просит программу остановиться: поток прерывается, и ближайший вызов API робота
     * завершит её со статусом {@link ExecutionResult.Status#STOPPED}.
     */
    public void stop() {
        worker.interrupt();
    }
}
