package com.farmgame.sandbox.runtime;

import com.farmgame.sandbox.api.FarmApi;
import com.farmgame.sandbox.api.FarmProgram;
import com.farmgame.sandbox.api.RobotApi;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Запускает {@link FarmProgram} в отдельном daemon-потоке с ограничением по времени,
 * чтобы бесконечный цикл в коде игрока не «подвесил» игру.
 *
 * <p>По истечении лимита поток игрока прерывается ({@link Thread#interrupt()}): любая
 * следующая команда роботу завершит программу. Цикл, который вообще не обращается к API,
 * прервать нельзя — поток остаётся daemon-потоком и не мешает закрытию игры. Полноценная
 * изоляция (отдельный процесс/JVM) — задача следующих этапов.
 */
public final class ProgramRunner {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private final Duration timeLimit;

    /** @param timeLimit максимальная длительность выполнения программы */
    public ProgramRunner(Duration timeLimit) {
        this.timeLimit = Objects.requireNonNull(timeLimit, "timeLimit");
    }

    /**
     * Асинхронно запускает программу.
     *
     * @return future, который завершится результатом выполнения (никогда — исключением)
     */
    public CompletableFuture<ExecutionResult> start(FarmProgram program, RobotApi robot, FarmApi farm) {
        CompletableFuture<ExecutionResult> result = new CompletableFuture<>();
        long startNanos = System.nanoTime();

        Thread worker = new Thread(() -> {
            try {
                program.run(robot, farm);
                result.complete(new ExecutionResult(ExecutionResult.Status.SUCCESS, "", since(startNanos)));
            } catch (RobotController.ProgramStoppedException e) {
                // Поток прерван по таймауту: future уже завершён через orTimeout(), это no-op.
                result.complete(new ExecutionResult(ExecutionResult.Status.TIMEOUT, e.getMessage(), since(startNanos)));
            } catch (Throwable t) {
                String message = t.getClass().getSimpleName() + ": " + t.getMessage();
                result.complete(new ExecutionResult(ExecutionResult.Status.ERROR, message, since(startNanos)));
            }
        }, "player-program-" + THREAD_COUNTER.incrementAndGet());
        worker.setDaemon(true);
        worker.start();

        result.orTimeout(timeLimit.toMillis(), TimeUnit.MILLISECONDS)
                .exceptionally(error -> {
                    worker.interrupt();
                    return null;
                });

        return result.handle((value, error) -> value != null ? value
                : new ExecutionResult(ExecutionResult.Status.TIMEOUT,
                "Превышен лимит времени " + timeLimit.toSeconds() + " с", since(startNanos)));
    }

    /** Синхронный вариант {@link #start}: блокирует вызывающий поток до завершения программы. */
    public ExecutionResult run(FarmProgram program, RobotApi robot, FarmApi farm) {
        try {
            return start(program, robot, farm).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionResult(ExecutionResult.Status.ERROR, "Ожидание прервано", Duration.ZERO);
        } catch (ExecutionException e) {
            return new ExecutionResult(ExecutionResult.Status.ERROR, String.valueOf(e.getCause()), Duration.ZERO);
        }
    }

    private static Duration since(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
