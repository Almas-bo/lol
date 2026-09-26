package com.farmgame.sandbox.lesson;

import com.farmgame.core.farm.Farm;

import java.util.List;

/**
 * Всё, что нужно для проверки урока после выполнения программы.
 *
 * @param farm   состояние фермы после программы
 * @param said   сообщения, которые программа вывела через {@code robot.say(...)}
 * @param source исходный код игрока
 */
public record LessonContext(Farm farm, List<String> said, String source) {

    public LessonContext {
        said = List.copyOf(said);
    }
}
