package com.farmgame.engine.ui.editor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Текст в редакторе кода: строки, курсор, выделение, отмена/повтор и «умные» отступы.
 *
 * <p>Не зависит от графики — отрисовкой занимается {@code CodeEditorState}.
 * Позиции задаются парой (строка, колонка), обе с нуля.
 */
public final class CodeDocument {

    /** Ширина одного уровня отступа в пробелах. */
    public static final int INDENT = 4;
    private static final int UNDO_LIMIT = 300;

    private final List<StringBuilder> lines = new ArrayList<>();
    private int caretLine;
    private int caretCol;
    /** Второй конец выделения; {@code -1}, если выделения нет. */
    private int anchorLine = -1;
    private int anchorCol;
    /** Колонка, к которой стремится курсор при движении вверх/вниз по коротким строкам. */
    private int preferredCol;
    private long version;

    private final Deque<Snapshot> undo = new ArrayDeque<>();
    private final Deque<Snapshot> redo = new ArrayDeque<>();

    private record Snapshot(String text, int line, int col) {
    }

    public CodeDocument() {
        lines.add(new StringBuilder());
    }

    // ------------------------------------------------------------------ чтение

    public String text() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    public int lineCount() {
        return lines.size();
    }

    public String line(int index) {
        return lines.get(index).toString();
    }

    public int caretLine() {
        return caretLine;
    }

    public int caretCol() {
        return caretCol;
    }

    /** Увеличивается при каждом изменении текста или курсора — для перерисовки. */
    public long version() {
        return version;
    }

    public boolean hasSelection() {
        return anchorLine >= 0 && (anchorLine != caretLine || anchorCol != caretCol);
    }

    /** Начало выделения {строка, колонка} (меньшая позиция). */
    public int[] selectionStart() {
        return before(anchorLine, anchorCol, caretLine, caretCol)
                ? new int[]{anchorLine, anchorCol} : new int[]{caretLine, caretCol};
    }

    /** Конец выделения {строка, колонка} (большая позиция). */
    public int[] selectionEnd() {
        return before(anchorLine, anchorCol, caretLine, caretCol)
                ? new int[]{caretLine, caretCol} : new int[]{anchorLine, anchorCol};
    }

    public String selectedText() {
        if (!hasSelection()) {
            return "";
        }
        int[] s = selectionStart();
        int[] e = selectionEnd();
        if (s[0] == e[0]) {
            return lines.get(s[0]).substring(s[1], e[1]);
        }
        StringBuilder sb = new StringBuilder(lines.get(s[0]).substring(s[1]));
        for (int i = s[0] + 1; i < e[0]; i++) {
            sb.append('\n').append(lines.get(i));
        }
        sb.append('\n').append(lines.get(e[0]), 0, e[1]);
        return sb.toString();
    }

    // ------------------------------------------------------------------ загрузка

    /** Заменяет текст без записи в историю отмены (загрузка урока). */
    public void load(String text) {
        setTextInternal(text);
        undo.clear();
        redo.clear();
        setCaret(0, 0, false);
    }

    /** Заменяет весь текст с возможностью отмены (кнопка «Заново»). */
    public void replaceAll(String text) {
        pushUndo();
        setTextInternal(text);
        setCaret(0, 0, false);
    }

    private void setTextInternal(String text) {
        lines.clear();
        for (String l : text.replace("\r\n", "\n").replace('\r', '\n').replace("\t", " ".repeat(INDENT))
                .split("\n", -1)) {
            lines.add(new StringBuilder(l));
        }
        anchorLine = -1;
        version++;
    }

    // ------------------------------------------------------------------ ввод

    /** Ввод одного символа с клавиатуры. */
    public void type(char c) {
        if (c == '\n') {
            newline();
            return;
        }
        if (c == '\t') {
            tab();
            return;
        }
        pushUndo();
        deleteSelectionInternal();
        StringBuilder line = lines.get(caretLine);
        // "}" в пустой строке сдвигает её на уровень влево.
        if (c == '}' && line.substring(0, caretCol).isBlank() && caretCol >= INDENT) {
            line.delete(caretCol - INDENT, caretCol);
            caretCol -= INDENT;
        }
        line.insert(caretCol, c);
        caretCol++;
        changed();
    }

    /** Вставка текста (из буфера обмена). */
    public void insert(String text) {
        if (text.isEmpty()) {
            return;
        }
        pushUndo();
        deleteSelectionInternal();
        insertRaw(text.replace("\r\n", "\n").replace('\r', '\n').replace("\t", " ".repeat(INDENT)));
        changed();
    }

    private void insertRaw(String text) {
        String[] parts = text.split("\n", -1);
        StringBuilder line = lines.get(caretLine);
        String tail = line.substring(caretCol);
        line.setLength(caretCol);
        line.append(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            lines.add(caretLine + i, new StringBuilder(parts[i]));
        }
        caretLine += parts.length - 1;
        caretCol = lines.get(caretLine).length();
        lines.get(caretLine).append(tail);
    }

    /** Enter с автоматическим отступом; после «{» отступ увеличивается. */
    public void newline() {
        pushUndo();
        deleteSelectionInternal();
        StringBuilder line = lines.get(caretLine);
        String before = line.substring(0, caretCol);
        String after = line.substring(caretCol);
        String indent = leadingSpaces(before.isBlank() ? line.toString() : before);
        boolean opensBlock = before.stripTrailing().endsWith("{");
        String inner = opensBlock ? indent + " ".repeat(INDENT) : indent;

        line.setLength(caretCol);
        if (opensBlock && after.stripLeading().startsWith("}")) {
            // {|}  →  {\n    |\n}
            lines.add(caretLine + 1, new StringBuilder(inner));
            lines.add(caretLine + 2, new StringBuilder(indent + after.stripLeading()));
        } else {
            lines.add(caretLine + 1, new StringBuilder(inner + after.stripLeading()));
        }
        caretLine++;
        caretCol = inner.length();
        changed();
    }

    /** Tab: отступ строки/выделенных строк или вставка пробелов до следующего уровня. */
    public void tab() {
        pushUndo();
        if (hasSelection()) {
            int[] s = selectionStart();
            int[] e = selectionEnd();
            for (int i = s[0]; i <= e[0]; i++) {
                lines.get(i).insert(0, " ".repeat(INDENT));
            }
            anchorLine = s[0];
            anchorCol = 0;
            caretLine = e[0];
            caretCol = lines.get(e[0]).length();
        } else {
            int spaces = INDENT - caretCol % INDENT;
            lines.get(caretLine).insert(caretCol, " ".repeat(spaces));
            caretCol += spaces;
        }
        changed();
    }

    /** Shift+Tab: убрать один уровень отступа у текущей или выделенных строк. */
    public void untab() {
        pushUndo();
        int from = hasSelection() ? selectionStart()[0] : caretLine;
        int to = hasSelection() ? selectionEnd()[0] : caretLine;
        for (int i = from; i <= to; i++) {
            StringBuilder line = lines.get(i);
            int remove = Math.min(INDENT, leadingSpaces(line.toString()).length());
            line.delete(0, remove);
            if (i == caretLine) {
                caretCol = Math.max(0, caretCol - remove);
            }
            if (i == anchorLine) {
                anchorCol = Math.max(0, anchorCol - remove);
            }
        }
        changed();
    }

    /** Backspace: удаляет выделение, символ слева, целый уровень отступа или склеивает строки. */
    public void backspace() {
        if (hasSelection()) {
            pushUndo();
            deleteSelectionInternal();
            changed();
            return;
        }
        if (caretLine == 0 && caretCol == 0) {
            return;
        }
        pushUndo();
        StringBuilder line = lines.get(caretLine);
        if (caretCol == 0) {
            StringBuilder prev = lines.get(caretLine - 1);
            caretCol = prev.length();
            prev.append(line);
            lines.remove(caretLine);
            caretLine--;
        } else if (line.substring(0, caretCol).isBlank() && caretCol % INDENT == 0) {
            line.delete(caretCol - INDENT, caretCol);
            caretCol -= INDENT;
        } else {
            line.deleteCharAt(caretCol - 1);
            caretCol--;
        }
        changed();
    }

    /** Delete: удаляет выделение или символ справа (в конце строки — склеивает со следующей). */
    public void delete() {
        if (hasSelection()) {
            pushUndo();
            deleteSelectionInternal();
            changed();
            return;
        }
        StringBuilder line = lines.get(caretLine);
        if (caretCol == line.length() && caretLine == lines.size() - 1) {
            return;
        }
        pushUndo();
        if (caretCol == line.length()) {
            line.append(lines.remove(caretLine + 1));
        } else {
            line.deleteCharAt(caretCol);
        }
        changed();
    }

    /** Удаляет выделенный текст (для «Вырезать»). */
    public void deleteSelection() {
        if (hasSelection()) {
            pushUndo();
            deleteSelectionInternal();
            changed();
        }
    }

    /** Ctrl+/: закомментировать или раскомментировать строку (или выделенные строки). */
    public void toggleComment() {
        pushUndo();
        int from = hasSelection() ? selectionStart()[0] : caretLine;
        int to = hasSelection() ? selectionEnd()[0] : caretLine;
        boolean allCommented = true;
        for (int i = from; i <= to; i++) {
            String l = lines.get(i).toString();
            if (!l.isBlank() && !l.stripLeading().startsWith("//")) {
                allCommented = false;
            }
        }
        for (int i = from; i <= to; i++) {
            StringBuilder l = lines.get(i);
            int indent = leadingSpaces(l.toString()).length();
            if (allCommented) {
                if (l.indexOf("//", indent) == indent) {
                    int remove = l.length() > indent + 2 && l.charAt(indent + 2) == ' ' ? 3 : 2;
                    l.delete(indent, indent + remove);
                    if (i == caretLine) {
                        caretCol = Math.max(indent, caretCol - remove);
                    }
                }
            } else if (!l.toString().isBlank()) {
                l.insert(indent, "// ");
                if (i == caretLine && caretCol >= indent) {
                    caretCol += 3;
                }
            }
        }
        caretCol = Math.min(caretCol, lines.get(caretLine).length());
        changed();
    }

    private void deleteSelectionInternal() {
        if (!hasSelection()) {
            anchorLine = -1;
            return;
        }
        int[] s = selectionStart();
        int[] e = selectionEnd();
        StringBuilder first = lines.get(s[0]);
        String tail = lines.get(e[0]).substring(e[1]);
        first.setLength(s[1]);
        first.append(tail);
        for (int i = e[0]; i > s[0]; i--) {
            lines.remove(i);
        }
        caretLine = s[0];
        caretCol = s[1];
        anchorLine = -1;
    }

    // ------------------------------------------------------------------ курсор

    /**
     * Ставит курсор в позицию (с ограничением по границам текста).
     *
     * @param extend {@code true} — расширить выделение (Shift или перетаскивание мышью)
     */
    public void setCaret(int line, int col, boolean extend) {
        startOrClearSelection(extend);
        caretLine = clamp(line, 0, lines.size() - 1);
        caretCol = clamp(col, 0, lines.get(caretLine).length());
        preferredCol = caretCol;
        version++;
    }

    public void moveLeft(boolean extend, boolean word) {
        if (!extend && hasSelection()) {
            int[] s = selectionStart();
            setCaret(s[0], s[1], false);
            return;
        }
        startOrClearSelection(extend);
        if (caretCol > 0) {
            caretCol = word ? wordBoundaryLeft(lines.get(caretLine).toString(), caretCol) : caretCol - 1;
        } else if (caretLine > 0) {
            caretLine--;
            caretCol = lines.get(caretLine).length();
        }
        preferredCol = caretCol;
        version++;
    }

    public void moveRight(boolean extend, boolean word) {
        if (!extend && hasSelection()) {
            int[] e = selectionEnd();
            setCaret(e[0], e[1], false);
            return;
        }
        startOrClearSelection(extend);
        StringBuilder line = lines.get(caretLine);
        if (caretCol < line.length()) {
            caretCol = word ? wordBoundaryRight(line.toString(), caretCol) : caretCol + 1;
        } else if (caretLine < lines.size() - 1) {
            caretLine++;
            caretCol = 0;
        }
        preferredCol = caretCol;
        version++;
    }

    /** Перемещение на {@code delta} строк (вверх — отрицательное). */
    public void moveVertical(int delta, boolean extend) {
        startOrClearSelection(extend);
        int target = clamp(caretLine + delta, 0, lines.size() - 1);
        if (target == caretLine) {
            caretCol = delta < 0 ? 0 : lines.get(caretLine).length();
        } else {
            caretLine = target;
            caretCol = Math.min(preferredCol, lines.get(caretLine).length());
        }
        version++;
    }

    /** Home: к первому непробельному символу, повторно — в начало строки. */
    public void home(boolean extend) {
        startOrClearSelection(extend);
        int indent = leadingSpaces(lines.get(caretLine).toString()).length();
        caretCol = caretCol == indent ? 0 : indent;
        preferredCol = caretCol;
        version++;
    }

    public void end(boolean extend) {
        startOrClearSelection(extend);
        caretCol = lines.get(caretLine).length();
        preferredCol = caretCol;
        version++;
    }

    public void documentStart(boolean extend) {
        setCaret(0, 0, extend);
    }

    public void documentEnd(boolean extend) {
        setCaret(lines.size() - 1, Integer.MAX_VALUE, extend);
    }

    public void selectAll() {
        anchorLine = 0;
        anchorCol = 0;
        caretLine = lines.size() - 1;
        caretCol = lines.get(caretLine).length();
        version++;
    }

    private void startOrClearSelection(boolean extend) {
        if (extend) {
            if (anchorLine < 0) {
                anchorLine = caretLine;
                anchorCol = caretCol;
            }
        } else {
            anchorLine = -1;
        }
    }

    // ------------------------------------------------------------------ отмена

    public boolean undo() {
        if (undo.isEmpty()) {
            return false;
        }
        redo.push(snapshot());
        restore(undo.pop());
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty()) {
            return false;
        }
        undo.push(snapshot());
        restore(redo.pop());
        return true;
    }

    private void pushUndo() {
        undo.push(snapshot());
        if (undo.size() > UNDO_LIMIT) {
            undo.removeLast();
        }
        redo.clear();
    }

    private Snapshot snapshot() {
        return new Snapshot(text(), caretLine, caretCol);
    }

    private void restore(Snapshot s) {
        setTextInternal(s.text());
        caretLine = clamp(s.line(), 0, lines.size() - 1);
        caretCol = clamp(s.col(), 0, lines.get(caretLine).length());
        preferredCol = caretCol;
    }

    private void changed() {
        preferredCol = caretCol;
        version++;
    }

    // ------------------------------------------------------------------ утилиты

    static String leadingSpaces(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return s.substring(0, i);
    }

    private static int wordBoundaryLeft(String s, int col) {
        int i = col;
        while (i > 0 && !Character.isLetterOrDigit(s.charAt(i - 1))) {
            i--;
        }
        while (i > 0 && Character.isLetterOrDigit(s.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    private static int wordBoundaryRight(String s, int col) {
        int i = col;
        while (i < s.length() && !Character.isLetterOrDigit(s.charAt(i))) {
            i++;
        }
        while (i < s.length() && Character.isLetterOrDigit(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static boolean before(int l1, int c1, int l2, int c2) {
        return l1 < l2 || (l1 == l2 && c1 <= c2);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
