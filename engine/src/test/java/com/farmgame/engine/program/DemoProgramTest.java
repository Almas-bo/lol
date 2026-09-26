package com.farmgame.engine.program;

import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;
import com.farmgame.engine.FarmGameApp;
import com.farmgame.sandbox.command.CommandSink;
import com.farmgame.sandbox.command.RobotCommand;
import com.farmgame.sandbox.compiler.CompilationResult;
import com.farmgame.sandbox.compiler.PlayerCodeCompiler;
import com.farmgame.sandbox.runtime.ExecutionResult;
import com.farmgame.sandbox.runtime.FarmView;
import com.farmgame.sandbox.runtime.ProgramRunner;
import com.farmgame.sandbox.runtime.RobotController;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверяет встроенную демо-программу без графики: команды применяются сразу, а pause() двигает время. */
class DemoProgramTest {

    @Test
    void demoProgramCompilesAndHarvestsCrops() throws Exception {
        String source;
        try (InputStream in = getClass().getResourceAsStream("/programs/DemoFarm.java")) {
            assertNotNull(in);
            source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        CompilationResult.Success compiled =
                assertInstanceOf(CompilationResult.Success.class, new PlayerCodeCompiler().compile(source));

        Farm farm = new Farm(FarmGameApp.FARM_WIDTH, FarmGameApp.FARM_HEIGHT);
        CommandSink simulatedTime = command -> {
            if (command instanceof RobotCommand.Pause pause) {
                farm.tick(pause.seconds());
            }
            return command.applyTo(farm);
        };
        RobotController robot = new RobotController(farm, simulatedTime, message -> { });

        ExecutionResult result = new ProgramRunner(Duration.ofSeconds(10))
                .run(compiled.program(), robot, new FarmView(farm));

        assertTrue(result.isSuccess(), result.message());
        assertTrue(farm.inventorySnapshot().getOrDefault(CropType.CORN, 0) > 0);
    }
}
