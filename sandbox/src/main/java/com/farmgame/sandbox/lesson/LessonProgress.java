package com.farmgame.sandbox.lesson;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Прогресс игрока: пройденные уроки и код, написанный в каждом уроке.
 * Хранится в файле properties (по умолчанию {@code ~/.javafarm/progress.properties}).
 *
 * <p>Ошибки чтения/записи не прерывают игру — прогресс просто не сохраняется.
 */
public final class LessonProgress {

    private static final Logger LOG = Logger.getLogger(LessonProgress.class.getName());
    private static final String COMPLETED = "completed";
    private static final String CODE_PREFIX = "code.";
    private static final String CURRENT = "current";

    private final Path file;
    private final Properties data = new Properties();

    public LessonProgress(Path file) {
        this.file = file;
        load();
    }

    /** Файл прогресса в домашней папке пользователя. */
    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".javafarm", "progress.properties");
    }

    public boolean isCompleted(String lessonId) {
        return completed().contains(lessonId);
    }

    public void markCompleted(String lessonId) {
        Set<String> ids = completed();
        ids.add(lessonId);
        data.setProperty(COMPLETED, String.join(",", ids));
    }

    /** Урок открыт, если это первый урок или предыдущий пройден. */
    public boolean isUnlocked(List<Lesson> lessons, int index) {
        return index == 0 || isCompleted(lessons.get(index - 1).id());
    }

    public Optional<String> savedCode(String lessonId) {
        return Optional.ofNullable(data.getProperty(CODE_PREFIX + lessonId));
    }

    public void saveCode(String lessonId, String code) {
        data.setProperty(CODE_PREFIX + lessonId, code);
    }

    public String currentLesson() {
        return data.getProperty(CURRENT, "");
    }

    public void setCurrentLesson(String lessonId) {
        data.setProperty(CURRENT, lessonId);
    }

    private Set<String> completed() {
        String raw = data.getProperty(COMPLETED, "");
        Set<String> ids = new LinkedHashSet<>(Arrays.asList(raw.split(",")));
        ids.remove("");
        return ids;
    }

    private void load() {
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            data.load(in);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Не удалось прочитать прогресс: " + file, e);
        }
    }

    /** Записывает прогресс на диск. */
    public void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                data.store(out, "Java Farm progress");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Не удалось сохранить прогресс: " + file, e);
        }
    }
}
