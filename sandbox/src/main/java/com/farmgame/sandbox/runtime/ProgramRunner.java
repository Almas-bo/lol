package com.farmgame.sandbox.runtime;

import com.farmgame.sandbox.api.FarmApi;
import com.farmgame.sandbox.api.FarmProgram;
import com.farmgame.sandbox.api.RobotApi;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Запускает {@link FarmProgram} в отдельном daemon-потоке с ограничением по времени,
 * чтобы бесконечный цикл в коде игрока не «подвесил» игру.
 *
 * <p>По истечении лимита (или по {@link RunningProgram#stop()}) поток игрока прерывается:
 * любая следующая команда роботу завершит программу. Цикл, который вообще не обращается к API,
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

    /** Асинхронно запускает программу. */
    public RunningProgram start(FarmProgram program, RobotApi robot, FarmApi farm) {
        CompletableFuture<ExecutionResult> result = new CompletableFuture<>();
        AtomicBoolean timedOut = new AtomicBoolean();
        long startNanos = System.nanoTime();
        String playerClass = program.getClass().getName();

        Thread worker = new Thread(() -> {
            try {
                program.run(robot, farm);
                result.complete(new ExecutionResult(ExecutionResult.Status.SUCCESS, "", since(startNanos)));
            } catch (RobotController.ProgramStoppedException e) {
                result.complete(timedOut.get()
                        ? new ExecutionResult(ExecutionResult.Status.TIMEOUT,
                        "Превышен лимит времени " + timeLimit.toSeconds() + " с", since(startNanos))
                        : new ExecutionResult(ExecutionResult.Status.STOPPED, e.getMessage(), since(startNanos)));
            } catch (Throwable t) {
                String message = t.getClass().getSimpleName()
                        + (t.getMessage() == null ? "" : ": " + t.getMessage());
                result.complete(new ExecutionResult(ExecutionResult.Status.ERROR, message,
                        playerLine(t, playerClass), since(startNanos)));
            }
        }, "player-program-" + THREAD_COUNTER.incrementAndGet());
        worker.setDaemon(true);
        worker.start();

        CompletableFuture.delayedExecutor(timeLimit.toMillis(), TimeUnit.MILLISECONDS).execute(() -> {
            if (!result.isDone()) {
                timedOut.set(true);
                worker.interrupt();
            }
        });
        return new RunningProgram(worker, result);
    }

    /** Синхронный вариант {@link #start}: блокирует вызывающий поток до завершения программы. */
    public ExecutionResult run(FarmProgram program, RobotApi robot, FarmApi farm) {
        try {
            return start(program, robot, farm).result().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionResult(ExecutionResult.Status.ERROR, "Ожидание прервано", Duration.ZERO);
        } catch (ExecutionException e) {
            return new ExecutionResult(ExecutionResult.Status.ERROR, String.valueOf(e.getCause()), Duration.ZERO);
        }
    }

    /** Ищет в стеке вызовов строку из кода игрока, чтобы подсветить её в редакторе. */
    private static int playerLine(Throwable t, String playerClass) {
        String outer = playerClass.contains("$") ? playerClass.substring(0, playerClass.indexOf('$')) : playerClass;
        for (StackTraceElement frame : t.getStackTrace()) {
            String cls = frame.getClassName();
            if ((cls.equals(outer) || cls.startsWith(outer + "$")) && frame.getLineNumber() > 0) {
                return frame.getLineNumber();
            }
        }
        return -1;
    }

    private static Duration since(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
