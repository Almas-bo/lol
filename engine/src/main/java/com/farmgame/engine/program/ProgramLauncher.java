package com.farmgame.engine.program;

import com.farmgame.core.farm.Farm;
import com.farmgame.engine.ui.GameConsole;
import com.farmgame.sandbox.command.QueuedCommandSink;
import com.farmgame.sandbox.compiler.CompilationResult;
import com.farmgame.sandbox.compiler.PlayerCodeCompiler;
import com.farmgame.sandbox.runtime.ExecutionResult;
import com.farmgame.sandbox.runtime.FarmView;
import com.farmgame.sandbox.runtime.ProgramRunner;
import com.farmgame.sandbox.runtime.RobotController;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Компилирует исходник игрока и запускает его против фермы.
 *
 * <p>Сейчас запускается демо-программа из ресурсов {@code programs/DemoFarm.java};
 * в будущем сюда придёт текст из внутриигрового редактора кода.
 */
public final class ProgramLauncher {

    private static final String DEMO_PROGRAM = "/programs/DemoFarm.java";
    private static final Duration TIME_LIMIT = Duration.ofMinutes(30);

    private final Farm farm;
    private final QueuedCommandSink sink;
    private final GameConsole console;

    public ProgramLauncher(Farm farm, QueuedCommandSink sink, GameConsole console) {
        this.farm = farm;
        this.sink = sink;
        this.console = console;
    }

    /** Запускает встроенную демо-программу. */
    public void launchDemo() {
        launch(readResource(DEMO_PROGRAM));
    }

    /** Компилирует и запускает произвольный исходный код игрока. Не блокирует вызывающий поток. */
    public void launch(String source) {
        console.print("Компиляция программы игрока...");
        switch (new PlayerCodeCompiler().compile(source)) {
            case CompilationResult.Failure failure -> console.print("Ошибка компиляции:\n" + failure.summary());
            case CompilationResult.Success success -> {
                console.print("Программа запущена");
                RobotController robot = new RobotController(farm, sink, console::print);
                new ProgramRunner(TIME_LIMIT)
                        .start(success.program(), robot, new FarmView(farm))
                        .thenAccept(result -> console.print(describe(result)));
            }
        }
    }

    private static String describe(ExecutionResult result) {
        return switch (result.status()) {
            case SUCCESS -> "Программа завершена успешно";
            case ERROR -> "Ошибка выполнения: " + result.message();
            case TIMEOUT -> "Программа остановлена: " + result.message();
        };
    }

    private static String readResource(String path) {
        try (InputStream in = ProgramLauncher.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Ресурс не найден: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
