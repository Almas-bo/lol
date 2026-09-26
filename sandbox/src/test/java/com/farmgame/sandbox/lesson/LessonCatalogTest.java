package com.farmgame.sandbox.lesson;

import com.farmgame.core.farm.Farm;
import com.farmgame.sandbox.command.CommandSink;
import com.farmgame.sandbox.command.RobotCommand;
import com.farmgame.sandbox.compiler.CompilationResult;
import com.farmgame.sandbox.compiler.PlayerCodeCompiler;
import com.farmgame.sandbox.runtime.ExecutionResult;
import com.farmgame.sandbox.runtime.FarmView;
import com.farmgame.sandbox.runtime.ProgramRunner;
import com.farmgame.sandbox.runtime.RobotController;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Гарантирует, что курс проходим: эталонное решение каждого урока компилируется в песочнице
 * и проходит проверку, а стартовый код компилируется, но задание ещё не выполняет.
 */
class LessonCatalogTest {

    private static final PlayerCodeCompiler COMPILER = new PlayerCodeCompiler();

    /** Запускает код урока без графики: pause() двигает игровое время. */
    static GoalResult runLesson(Lesson lesson, String source) {
        CompilationResult.Success compiled = assertInstanceOf(CompilationResult.Success.class,
                COMPILER.compile(source), () -> lesson.id() + ": код не компилируется\n" + source);

        Farm farm = new Farm(8, 6);
        lesson.prepare(farm);
        CommandSink simulatedTime = command -> {
            if (command instanceof RobotCommand.Pause pause) {
                farm.tick(pause.seconds());
            }
            return command.applyTo(farm);
        };
        List<String> said = new ArrayList<>();
        RobotController robot = new RobotController(farm, simulatedTime, said::add);
        ExecutionResult result = new ProgramRunner(Duration.ofSeconds(10))
                .run(compiled.program(), robot, new FarmView(farm));
        if (!result.isSuccess()) {
            return GoalResult.fail("выполнение: " + result.status() + " " + result.message());
        }
        return lesson.goal().check(new LessonContext(farm, said, source));
    }

    @TestFactory
    Stream<DynamicTest> solutionsPassTheirGoals() {
        return LessonCatalog.lessons().stream().map(lesson -> DynamicTest.dynamicTest(lesson.id(), () -> {
            GoalResult result = runLesson(lesson, lesson.solution());
            assertTrue(result.passed(), lesson.id() + ": " + result.message());
        }));
    }

    @TestFactory
    Stream<DynamicTest> starterCodeCompilesButIsNotASolution() {
        return LessonCatalog.lessons().stream()
                .filter(lesson -> !lesson.id().equals("free-mode"))
                .map(lesson -> DynamicTest.dynamicTest(lesson.id(), () -> {
                    GoalResult result = runLesson(lesson, lesson.starterCode());
                    assertFalse(result.passed(), lesson.id() + ": стартовый код уже решает задачу");
                }));
    }

    @Test
    void lessonIdsAreUnique() {
        List<Lesson> lessons = LessonCatalog.lessons();
        assertEquals(lessons.size(), new HashSet<>(lessons.stream().map(Lesson::id).toList()).size());
    }

    @Test
    void commentedOutCodeDoesNotCountAsUsingAConstruct() {
        Lesson forLesson = LessonCatalog.lessons().get(4);
        String cheat = LessonCatalog.program("""
                // for (int x = 0; x < 8; x++)
                robot.moveTo(0, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water();
                robot.moveTo(1, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water();
                robot.moveTo(2, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water();
                robot.moveTo(3, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water();
                robot.moveTo(4, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water();
                robot.moveTo(5, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water();
                robot.moveTo(6, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water();
                robot.moveTo(7, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water();
                """);
        GoalResult result = runLesson(forLesson, cheat);
        assertFalse(result.passed());
        assertTrue(result.message().contains("for"), result.message());
    }

    @Test
    void combinedGoalReportsMainMessageOnSuccess() {
        LessonGoal goal = ctx -> GoalResult.pass("главное");
        LessonGoal combined = goal.and(ctx -> GoalResult.pass("второстепенное"));
        assertEquals("главное", combined.check(null).message());
        assertEquals("нет", goal.and(ctx -> GoalResult.fail("нет")).check(null).message());
    }
}
