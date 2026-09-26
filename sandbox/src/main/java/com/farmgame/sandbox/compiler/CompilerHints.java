package com.farmgame.sandbox.compiler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Переводит частые сообщения компилятора javac в понятные новичку подсказки на русском.
 * Оригинальное сообщение не заменяется, а дополняется — так игрок постепенно учится
 * читать настоящие ошибки компилятора.
 */
public final class CompilerHints {

    private static final Map<String, String> HINTS = new LinkedHashMap<>();

    static {
        HINTS.put("';' expected", "Пропущена точка с запятой «;» в конце команды.");
        HINTS.put("cannot find symbol", "Имя не найдено: проверьте опечатки и регистр букв (moveTo, а не moveto).");
        HINTS.put("reached end of file while parsing", "Не хватает закрывающей фигурной скобки «}».");
        HINTS.put("class, interface, enum, or record expected", "Лишняя «}» или код оказался вне класса.");
        HINTS.put("illegal start of expression", "Синтаксическая ошибка: проверьте скобки и точки с запятой выше.");
        HINTS.put("not a statement", "Это не команда: возможно, пропущены скобки () у вызова метода.");
        HINTS.put("incompatible types", "Несовместимые типы: значение не подходит для этой переменной или параметра.");
        HINTS.put("')' expected", "Не хватает закрывающей круглой скобки «)».");
        HINTS.put("unclosed string literal", "Строка не закрыта: добавьте вторую кавычку «\"».");
        HINTS.put("might not have been initialized", "Переменной не присвоено значение перед использованием.");
        HINTS.put("is already defined", "Такое имя уже объявлено — выберите другое.");
        HINTS.put("cannot be applied to given types", "Неверные аргументы метода: проверьте их количество и типы.");
        HINTS.put("missing return statement", "Метод должен вернуть значение: добавьте return.");
        HINTS.put("unreachable statement", "Этот код никогда не выполнится (он после return/break).");
        HINTS.put("<identifier> expected", "Ожидалось имя: возможно, код написан вне метода.");
        HINTS.put("does not override abstract method", "Класс должен содержать метод run(RobotApi robot, FarmApi farm).");
    }

    private CompilerHints() {
    }

    /** Подсказка для сообщения компилятора или пустая строка, если подсказки нет. */
    public static String hintFor(String compilerMessage) {
        for (Map.Entry<String, String> e : HINTS.entrySet()) {
            if (compilerMessage.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return "";
    }
}
