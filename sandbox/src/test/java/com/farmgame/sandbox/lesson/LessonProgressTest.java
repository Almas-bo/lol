package com.farmgame.sandbox.lesson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LessonProgressTest {

    @Test
    void progressSurvivesRestart(@TempDir Path dir) {
        Path file = dir.resolve("sub/progress.properties");
        LessonProgress progress = new LessonProgress(file);
        progress.markCompleted("first-command");
        progress.saveCode("sequence", "robot.moveTo(1, 1); // привет\nвторая строка");
        progress.setCurrentLesson("sequence");
        progress.save();

        LessonProgress reloaded = new LessonProgress(file);
        assertTrue(reloaded.isCompleted("first-command"));
        assertEquals("robot.moveTo(1, 1); // привет\nвторая строка", reloaded.savedCode("sequence").orElseThrow());
        assertEquals("sequence", reloaded.currentLesson());
    }

    @Test
    void lessonsUnlockInOrder(@TempDir Path dir) {
        List<Lesson> lessons = LessonCatalog.lessons();
        LessonProgress progress = new LessonProgress(dir.resolve("p.properties"));
        assertTrue(progress.isUnlocked(lessons, 0));
        assertFalse(progress.isUnlocked(lessons, 1));

        progress.markCompleted(lessons.get(0).id());
        assertTrue(progress.isUnlocked(lessons, 1));
        assertFalse(progress.isUnlocked(lessons, 2));
    }
}
