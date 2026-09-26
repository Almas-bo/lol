package com.farmgame.sandbox.lesson;

import com.farmgame.core.farm.Farm;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Один урок курса.
 *
 * <p>Текст {@code theory} состоит из абзацев, разделённых пустой строкой; строки,
 * начинающиеся с двух пробелов, отображаются как код.
 *
 * @param id          стабильный идентификатор (для сохранения прогресса)
 * @param title       заголовок
 * @param topic       изучаемая тема программирования
 * @param theory      объяснение темы с примерами
 * @param task        что нужно сделать
 * @param hint        подсказка, если игрок застрял
 * @param starterCode код, который видит игрок в начале урока
 * @param solution    эталонное решение (используется в тестах и как «последняя подсказка»)
 * @param setup       подготовка фермы перед каждым запуском (ферма уже сброшена)
 * @param goal        проверка результата
 */
public record Lesson(String id, String title, String topic, String theory, String task, String hint,
                     String starterCode, String solution, Consumer<Farm> setup, LessonGoal goal) {

    public Lesson {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(setup, "setup");
        Objects.requireNonNull(goal, "goal");
    }

    /** Готовит ферму к запуску: сбрасывает её и применяет {@link #setup()}. */
    public void prepare(Farm farm) {
        farm.reset();
        setup.accept(farm);
        farm.resetRobot();
    }
}
