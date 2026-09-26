package com.farmgame.engine.program;

import com.farmgame.core.farm.Farm;
import com.farmgame.sandbox.command.QueuedCommandSink;
import com.farmgame.sandbox.compiler.CompilationResult;
import com.farmgame.sandbox.compiler.PlayerCodeCompiler;
import com.farmgame.sandbox.runtime.ExecutionResult;
import com.farmgame.sandbox.runtime.FarmView;
import com.farmgame.sandbox.runtime.ProgramRunner;
import com.farmgame.sandbox.runtime.RobotController;
import com.farmgame.sandbox.runtime.RunningProgram;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Один запуск кода игрока: компиляция (в фоновом потоке, чтобы не «замирала» картинка)
 * и выполнение против фермы через очередь команд движка.
 */
public final class ProgramSession {

    private static final Duration TIME_LIMIT = Duration.ofMinutes(5);

    /** Итог запуска: ошибка компиляции или результат выполнения. */
    public sealed interface Outcome {
        record CompileError(CompilationResult.Failure failure) implements Outcome {
        }

        record Finished(ExecutionResult result, List<String> said) implements Outcome {
        }
    }

    private final Farm farm;
    private final QueuedCommandSink sink;
    private final PlayerCodeCompiler compiler = new PlayerCodeCompiler();
    private volatile RunningProgram running;

    public ProgramSession(Farm farm, QueuedCommandSink sink) {
        this.farm = farm;
        this.sink = sink;
    }

    /**
     * Компилирует и запускает код.
     *
     * @param source  исходник игрока
     * @param console куда выводить {@code robot.say(...)}
     * @return итог; future завершается в фоновом потоке
     */
    public CompletableFuture<Outcome> run(String source, Consumer<String> console) {
        stop();
        return CompletableFuture.supplyAsync(() -> compiler.compile(source))
                .thenCompose(compiled -> switch (compiled) {
                    case CompilationResult.Failure failure ->
                            CompletableFuture.completedFuture(new Outcome.CompileError(failure));
                    case CompilationResult.Success success -> execute(success, console);
                });
    }

    private CompletableFuture<Outcome> execute(CompilationResult.Success success, Consumer<String> console) {
        List<String> said = new CopyOnWriteArrayList<>();
        RobotController robot = new RobotController(farm, sink, message -> {
            said.add(message);
            console.accept(message);
        });
        RunningProgram program = new ProgramRunner(TIME_LIMIT).start(success.program(), robot, new FarmView(farm));
        running = program;
        return program.result().thenApply(result -> new Outcome.Finished(result, List.copyOf(said)));
    }

    /** Останавливает выполняющуюся программу (если есть) и отменяет её команды. */
    public void stop() {
        RunningProgram current = running;
        if (current != null) {
            current.stop();
            running = null;
        }
        sink.clear();
    }

    public boolean isRunning() {
        RunningProgram current = running;
        return current != null && current.isRunning();
    }
}
