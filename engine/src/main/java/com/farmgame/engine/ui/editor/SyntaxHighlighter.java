package com.farmgame.engine.ui.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Простая подсветка синтаксиса Java построчно. Поддерживает многострочные
 * комментарии: состояние «внутри комментария» передаётся от строки к строке.
 */
public final class SyntaxHighlighter {

    /** Вид фрагмента текста. */
    public enum Kind {
        PLAIN, KEYWORD, TYPE, STRING, NUMBER, COMMENT, ANNOTATION, METHOD
    }

    /** Фрагмент строки [start, end) одного вида. */
    public record Span(int start, int end, Kind kind) {
    }

    /** Результат разбора строки. */
    public record LineTokens(List<Span> spans, boolean endsInBlockComment) {
    }

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", "default",
            "do", "double", "else", "enum", "extends", "false", "final", "finally", "float", "for", "if",
            "implements", "import", "instanceof", "int", "interface", "long", "new", "null", "package",
            "private", "protected", "public", "record", "return", "short", "static", "super", "switch",
            "this", "throw", "throws", "true", "try", "var", "void", "while", "yield");

    private SyntaxHighlighter() {
    }

    /**
     * Разбирает строку.
     *
     * @param inBlockComment начинается ли строка внутри {@code /* ... * /}
     */
    public static LineTokens tokenize(String line, boolean inBlockComment) {
        List<Span> spans = new ArrayList<>();
        int i = 0;
        int n = line.length();
        boolean inComment = inBlockComment;
        while (i < n) {
            if (inComment) {
                int close = line.indexOf("*/", i);
                int end = close < 0 ? n : close + 2;
                spans.add(new Span(i, end, Kind.COMMENT));
                inComment = close < 0;
                i = end;
                continue;
            }
            char c = line.charAt(i);
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '/') {
                spans.add(new Span(i, n, Kind.COMMENT));
                break;
            }
            if (c == '/' && i + 1 < n && line.charAt(i + 1) == '*') {
                inComment = true;
                int close = line.indexOf("*/", i + 2);
                int end = close < 0 ? n : close + 2;
                spans.add(new Span(i, end, Kind.COMMENT));
                inComment = close < 0;
                i = end;
                continue;
            }
            if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < n && line.charAt(j) != c) {
                    j += line.charAt(j) == '\\' ? 2 : 1;
                }
                int end = Math.min(n, j + 1);
                spans.add(new Span(i, end, Kind.STRING));
                i = end;
                continue;
            }
            if (Character.isDigit(c)) {
                int j = i;
                while (j < n && (Character.isLetterOrDigit(line.charAt(j)) || line.charAt(j) == '.'
                        || line.charAt(j) == '_')) {
                    j++;
                }
                spans.add(new Span(i, j, Kind.NUMBER));
                i = j;
                continue;
            }
            if (c == '@' || Character.isJavaIdentifierStart(c)) {
                int j = i + 1;
                while (j < n && Character.isJavaIdentifierPart(line.charAt(j))) {
                    j++;
                }
                String word = line.substring(i, j);
                spans.add(new Span(i, j, classify(word, line, j)));
                i = j;
                continue;
            }
            int j = i + 1;
            while (j < n && !startsToken(line, j)) {
                j++;
            }
            spans.add(new Span(i, j, Kind.PLAIN));
            i = j;
        }
        return new LineTokens(spans, inComment);
    }

    private static Kind classify(String word, String line, int end) {
        if (word.startsWith("@")) {
            return Kind.ANNOTATION;
        }
        if (KEYWORDS.contains(word)) {
            return Kind.KEYWORD;
        }
        int k = end;
        while (k < line.length() && line.charAt(k) == ' ') {
            k++;
        }
        if (k < line.length() && line.charAt(k) == '(') {
            return Character.isUpperCase(word.charAt(0)) ? Kind.TYPE : Kind.METHOD;
        }
        return Character.isUpperCase(word.charAt(0)) ? Kind.TYPE : Kind.PLAIN;
    }

    private static boolean startsToken(String line, int i) {
        char c = line.charAt(i);
        return c == '"' || c == '\'' || c == '@' || c == '/' || Character.isJavaIdentifierStart(c)
                || Character.isDigit(c);
    }
}
