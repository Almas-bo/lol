package com.farmgame.sandbox.command;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Очередь команд между потоком игрока и игровым циклом движка.
 *
 * <p>Поток игрока кладёт команду и блокируется. Игровой цикл забирает её через
 * {@link #poll()}, проигрывает анимацию и вызывает {@link PendingCommand#complete} —
 * только после этого код игрока продолжает выполнение.
 *
 * <p>Если программу игрока остановили (кнопка «Стоп», таймаут), ожидающая команда
 * помечается отменённой и движок её пропускает.
 */
public final class QueuedCommandSink implements CommandSink {

    private final BlockingQueue<PendingCommand> queue = new LinkedBlockingQueue<>();

    @Override
    public int execute(RobotCommand command) throws InterruptedException {
        PendingCommand pending = new PendingCommand(command);
        queue.put(pending);
        try {
            return pending.result.get();
        } catch (InterruptedException e) {
            pending.cancel();
            queue.remove(pending);
            throw e;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getCause());
        }
    }

    /**
     * Забирает следующую неотменённую команду без ожидания или возвращает {@code null}.
     * Вызывается движком.
     */
    public PendingCommand poll() {
        PendingCommand next;
        while ((next = queue.poll()) != null) {
            if (!next.isCancelled()) {
                return next;
            }
        }
        return null;
    }

    /** Отменяет все ожидающие команды (например, перед новым запуском программы). */
    public void clear() {
        PendingCommand next;
        while ((next = queue.poll()) != null) {
            next.cancel();
        }
    }

    /** Команда, ожидающая выполнения в игровом мире. */
    public static final class PendingCommand {

        private final RobotCommand command;
        private final CompletableFuture<Integer> result = new CompletableFuture<>();

        private PendingCommand(RobotCommand command) {
            this.command = command;
        }

        public RobotCommand command() {
            return command;
        }

        /** Сообщает потоку игрока об успешном выполнении. */
        public void complete(int value) {
            result.complete(value);
        }

        /** Передаёт ошибку (например, нарушение правил) обратно в код игрока. */
        public void fail(RuntimeException error) {
            result.completeExceptionally(error);
        }

        /** Программа игрока больше не ждёт эту команду. */
        public boolean isCancelled() {
            return result.isCancelled();
        }

        private void cancel() {
            result.cancel(false);
        }
    }
}
