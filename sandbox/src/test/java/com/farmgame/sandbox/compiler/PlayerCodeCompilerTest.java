package com.farmgame.sandbox.compiler;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;
import com.farmgame.sandbox.command.DirectCommandSink;
import com.farmgame.sandbox.runtime.ExecutionResult;
import com.farmgame.sandbox.runtime.FarmView;
import com.farmgame.sandbox.runtime.ProgramRunner;
import com.farmgame.sandbox.runtime.RobotController;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerCodeCompilerTest {

    private final PlayerCodeCompiler compiler = new PlayerCodeCompiler();

    @Test
    void compilesAndRunsPlayerProgram() {
        String source = """
                import com.farmgame.core.crop.*;
                import com.farmgame.sandbox.api.*;
                import java.util.List;

                public class MyFarm implements FarmProgram {
                    @Override
                    public void run(RobotApi robot, FarmApi farm) {
                        List<Integer> columns = List.of(1, 2);
                        columns.forEach(x -> {
                            robot.moveTo(x, 1);
                            robot.plant(Crop.builder().setType(CropType.CORN).build());
                            robot.water();
                        });
                        robot.say("Посажено: " + columns.size());
                    }
                }
                """;

        CompilationResult result = compiler.compile(source);
        CompilationResult.Success success = assertInstanceOf(CompilationResult.Success.class, result);

        Farm farm = new Farm(5, 5);
        List<String> console = new ArrayList<>();
        RobotController robot = new RobotController(farm, new DirectCommandSink(farm), console::add);
        ExecutionResult execution = new ProgramRunner(Duration.ofSeconds(5))
                .run(success.program(), robot, new FarmView(farm));

        assertTrue(execution.isSuccess(), execution.message());
        assertEquals(new GridPosition(2, 1), farm.robotPosition());
        assertEquals(CropType.CORN, farm.plotAt(new GridPosition(1, 1)).crop().type());
        assertEquals(List.of("Посажено: 2"), console);
    }

    @Test
    void reportsCompilationErrorsWithLineNumbers() {
        String source = """
                import com.farmgame.sandbox.api.*;

                public class Broken implements FarmProgram {
                    public void run(RobotApi robot, FarmApi farm) {
                        robot.fly();
                    }
                }
                """;

        CompilationResult.Failure failure = assertInstanceOf(CompilationResult.Failure.class, compiler.compile(source));
        assertEquals(5, failure.diagnostics().getFirst().line());
    }

    @Test
    void forbidsAccessToGameInternals() {
        String source = """
                import com.farmgame.sandbox.api.*;

                public class Cheater implements FarmProgram {
                    private final Object farm = new com.farmgame.core.farm.Farm(1, 1);
                    public void run(RobotApi robot, FarmApi farm) { }
                }
                """;

        CompilationResult.Failure failure = assertInstanceOf(CompilationResult.Failure.class, compiler.compile(source));
        assertTrue(failure.summary().contains("недоступен в песочнице"), failure.summary());
    }

    @Test
    void sandboxAllowListIsStrict() {
        assertTrue(SandboxClassLoader.isAllowed("java.util.ArrayList"));
        assertTrue(SandboxClassLoader.isAllowed(CropType.class.getName()));
        assertTrue(!SandboxClassLoader.isAllowed("java.io.File"));
        assertTrue(!SandboxClassLoader.isAllowed("java.lang.reflect.Method"));
        assertTrue(!SandboxClassLoader.isAllowed("java.lang.System"));
    }

    @Test
    void compilerErrorsGetFriendlyHints() {
        CompilationResult.Failure failure = assertInstanceOf(CompilationResult.Failure.class, compiler.compile("""
                import com.farmgame.sandbox.api.*;
                public class Oops implements FarmProgram {
                    public void run(RobotApi robot, FarmApi farm) {
                        robot.moveTo(1, 1)
                    }
                }
                """));
        String hint = CompilerHints.hintFor(failure.diagnostics().getFirst().message());
        assertTrue(hint.contains("точка с запятой"), hint);
    }
}
