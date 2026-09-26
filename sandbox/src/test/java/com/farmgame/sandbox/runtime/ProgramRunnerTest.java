package com.farmgame.sandbox.runtime;

import com.farmgame.core.farm.Farm;
import com.farmgame.sandbox.api.FarmProgram;
import com.farmgame.sandbox.command.DirectCommandSink;
import com.farmgame.sandbox.command.QueuedCommandSink;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgramRunnerTest {

    private final Farm farm = new Farm(3, 3);

    @Test
    void ruleViolationBecomesPlayerFacingError() {
        RobotController robot = new RobotController(farm,
                new DirectCommandSink(farm), s -> { });
        FarmProgram program = (r, f) -> r.moveTo(10, 10);

        ExecutionResult result = new ProgramRunner(Duration.ofSeconds(2)).run(program, robot, new FarmView(farm));

        assertEquals(ExecutionResult.Status.ERROR, result.status());
        assertTrue(result.message().startsWith("RobotActionException"), result.message());
    }

    @Test
    void stuckProgramIsStoppedByTimeout() {
        // Движок не обрабатывает очередь, поэтому робот "едет" вечно.
        QueuedCommandSink neverProcessed = new QueuedCommandSink();
        RobotController robot = new RobotController(farm, neverProcessed, s -> { });
        FarmProgram program = (r, f) -> r.moveTo(1, 1);

        ExecutionResult result = new ProgramRunner(Duration.ofMillis(200)).run(program, robot, new FarmView(farm));

        assertEquals(ExecutionResult.Status.TIMEOUT, result.status());
    }

    @Test
    void queuedCommandCompletesWhenEngineProcessesIt() throws Exception {
        QueuedCommandSink sink = new QueuedCommandSink();
        RobotController robot = new RobotController(farm, sink, s -> { });
        var future = new ProgramRunner(Duration.ofSeconds(5))
                .start((r, f) -> r.moveTo(2, 2), robot, new FarmView(farm));

        // Имитация игрового цикла движка.
        QueuedCommandSink.PendingCommand pending;
        while ((pending = sink.poll()) == null) {
            Thread.onSpinWait();
        }
        pending.complete(pending.command().applyTo(farm));

        assertTrue(future.get().isSuccess());
        assertEquals(2, farm.robotPosition().x());
    }
}
