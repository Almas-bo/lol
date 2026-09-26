package com.farmgame.sandbox.compiler;

import com.farmgame.sandbox.api.FarmProgram;

import java.util.List;

/**
 * Результат компиляции кода игрока: либо готовая программа, либо список ошибок.
 * Запечатанный интерфейс заставляет вызывающий код обработать оба случая.
 */
public sealed interface CompilationResult {

    /** Код скомпилирован, класс загружен и создан экземпляр программы. */
    record Success(FarmProgram program) implements CompilationResult {
    }

    /** Компиляция или загрузка не удалась. */
    record Failure(List<Diagnostic> diagnostics) implements CompilationResult {
        public Failure {
            diagnostics = List.copyOf(diagnostics);
        }

        /** Все сообщения одной строкой — удобно для вывода в консоль. */
        public String summary() {
            StringBuilder sb = new StringBuilder();
            for (Diagnostic d : diagnostics) {
                sb.append(d).append(System.lineSeparator());
            }
            return sb.toString().trim();
        }
    }

    /**
     * Одно сообщение компилятора.
     *
     * @param line    номер строки (или -1, если неизвестен)
     * @param message текст ошибки
     */
    record Diagnostic(long line, String message) {
        @Override
        public String toString() {
            return line > 0 ? "Строка " + line + ": " + message : message;
        }
    }
}
