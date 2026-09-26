package com.farmgame.engine.state;

import com.farmgame.engine.ui.Button;
import com.farmgame.engine.ui.HudPanel;
import com.farmgame.engine.ui.HudStyle;
import com.farmgame.engine.ui.TextWrap;
import com.farmgame.engine.ui.UiLayout;
import com.farmgame.engine.ui.editor.CodeDocument;
import com.farmgame.engine.ui.editor.SyntaxHighlighter;
import com.farmgame.engine.ui.editor.SyntaxHighlighter.Kind;
import com.farmgame.engine.ui.editor.SystemClipboard;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Встроенный редактор кода: панель справа, где игрок пишет программу для робота.
 *
 * <p>Клик по панели — фокус на редакторе (клавиши идут в код, а не в камеру),
 * {@code Esc} или клик по 3D-сцене — снять фокус. {@code Ctrl+Enter} или {@code F5} — запуск.
 * Пока редактор в фокусе, все события клавиатуры «поглощаются» и не доходят до камеры.
 */
public class CodeEditorState extends BaseAppState implements RawInputListener {

    /** Действия кнопок редактора, которые обрабатывает урок. */
    public interface Actions {
        void run();

        void stop();

        void resetCode();
    }

    private static final int HEADER_H = 52;
    private static final int FOOTER_H = 30;
    private static final int GUTTER_W = 46;
    private static final int PAD = 10;

    private static final Color BG = new Color(24, 28, 26, 238);
    private static final Color GUTTER_BG = new Color(18, 21, 19, 245);
    private static final Color CURRENT_LINE = new Color(255, 255, 255, 14);
    private static final Color SELECTION = new Color(60, 110, 170, 150);
    private static final Color ERROR_LINE = new Color(170, 50, 40, 90);
    private static final Color CARET = new Color(0xFF, 0xD3, 0x4D);
    private static final Color LINE_NUMBER = new Color(110, 120, 112);
    private static final Map<Kind, Color> SYNTAX = new EnumMap<>(Kind.class);

    static {
        SYNTAX.put(Kind.PLAIN, new Color(0xE8, 0xE6, 0xDC));
        SYNTAX.put(Kind.KEYWORD, new Color(0xFF, 0x9D, 0x5C));
        SYNTAX.put(Kind.TYPE, new Color(0x7F, 0xD6, 0xC2));
        SYNTAX.put(Kind.STRING, new Color(0xB5, 0xE0, 0x7A));
        SYNTAX.put(Kind.NUMBER, new Color(0xE6, 0xC0, 0x7B));
        SYNTAX.put(Kind.COMMENT, new Color(0x80, 0x8E, 0x84));
        SYNTAX.put(Kind.ANNOTATION, new Color(0xC4, 0x9B, 0xE0));
        SYNTAX.put(Kind.METHOD, new Color(0x82, 0xB8, 0xFF));
    }

    private final UiLayout.Rect rect;
    private final CodeDocument doc = new CodeDocument();
    private Actions actions;
    private HudPanel panel;
    private SystemClipboard clipboard;
    private final List<Button> buttons = new ArrayList<>();

    private boolean focused = true;
    private boolean running;
    private boolean dragging;
    private boolean ctrl;
    private boolean shift;
    private String title = "MyFarm.java";
    private int errorLine = -1;
    private String errorMessage = "";

    private int scrollLine;
    private int scrollX;
    private int lineHeight = 20;
    private int charWidth = 8;
    private long seenVersion = -1;
    private float time;
    private float lastEditTime;

    public CodeEditorState(UiLayout.Rect rect) {
        this.rect = rect;
    }

    // ------------------------------------------------------------------ API для урока

    public void setActions(Actions actions) {
        this.actions = actions;
    }

    /** Загружает код (без истории отмены). */
    public void load(String code) {
        doc.load(code);
        scrollLine = 0;
        scrollX = 0;
        clearError();
    }

    /** Заменяет код с возможностью отменить (Ctrl+Z). */
    public void replace(String code) {
        doc.replaceAll(code);
        clearError();
    }

    public String code() {
        return doc.text();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    /** Подсвечивает строку с ошибкой (нумерация с 1) и показывает сообщение под кодом. */
    public void setError(int line, String message) {
        errorLine = line;
        errorMessage = message == null ? "" : message;
        if (line > 0) {
            scrollToLine(line - 1);
        }
    }

    public void clearError() {
        errorLine = -1;
        errorMessage = "";
    }

    public void focus() {
        focused = true;
    }

    public boolean isFocused() {
        return focused;
    }

    public HudPanel panel() {
        return panel;
    }

    // ------------------------------------------------------------------ жизненный цикл

    @Override
    protected void initialize(Application app) {
        panel = new HudPanel("code-editor", app.getAssetManager(), rect.width(), rect.height());
        panel.picture().setPosition(rect.x(), rect.y());
        clipboard = new SystemClipboard(app);
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(panel.picture());
        getApplication().getInputManager().addRawInputListener(this);
    }

    @Override
    protected void onDisable() {
        panel.picture().removeFromParent();
        getApplication().getInputManager().removeRawInputListener(this);
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    public void update(float tpf) {
        time += tpf;
        if (doc.version() != seenVersion) {
            seenVersion = doc.version();
            lastEditTime = time;
            ensureCaretVisible();
        }
        boolean caretOn = focused && ((time - lastEditTime) % 1.0f) < 0.6f;
        String key = doc.version() + "|" + scrollLine + "|" + scrollX + "|" + focused + "|" + caretOn + "|"
                + running + "|" + errorLine + "|" + errorMessage + "|" + title;
        panel.redraw(key, g -> paint(g, caretOn));
    }

    // ------------------------------------------------------------------ отрисовка

    private int textTop() {
        return HEADER_H;
    }

    private int textBottom() {
        return rect.height() - FOOTER_H - (errorMessage.isEmpty() ? 0 : errorBarHeight());
    }

    private int errorBarHeight() {
        return 58;
    }

    private int visibleLines() {
        return Math.max(1, (textBottom() - textTop() - 6) / lineHeight);
    }

    private int textLeft() {
        return GUTTER_W + PAD;
    }

    private void paint(Graphics2D g, boolean caretOn) {
        int w = rect.width();
        int h = rect.height();
        g.setColor(BG);
        g.fillRoundRect(0, 0, w, h, 16, 16);
        g.setColor(focused ? new Color(0xFF, 0xC8, 0x3D, 150) : new Color(255, 255, 255, 50));
        g.setStroke(new BasicStroke(focused ? 2f : 1.5f));
        g.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);

        g.setFont(HudStyle.CODE);
        FontMetrics fm = g.getFontMetrics();
        lineHeight = fm.getHeight() + 3;
        charWidth = Math.max(1, fm.charWidth('m'));

        paintHeader(g);
        paintText(g, fm, caretOn);
        if (!errorMessage.isEmpty()) {
            paintErrorBar(g);
        }
        paintFooter(g);
    }

    private void paintHeader(Graphics2D g) {
        buttons.clear();
        HudStyle.title(g, "Код", 16, 32);
        g.setFont(HudStyle.SMALL);
        g.setColor(HudStyle.MUTED);
        g.drawString(title, 62, 32);

        int bh = 32;
        int by = 10;
        int x = rect.width() - 12;
        x -= Button.measure(g, "Заново", Button.Icon.RESET);
        buttons.add(Button.draw(g, "reset", "Заново", Button.Icon.RESET, x, by, bh, HudStyle.BUTTON_GRAY, !running));
        x -= 8 + Button.measure(g, "Стоп", Button.Icon.STOP);
        buttons.add(Button.draw(g, "stop", "Стоп", Button.Icon.STOP, x, by, bh, HudStyle.BUTTON_RED, running));
        String runText = running ? "Выполняется…" : "Запустить";
        x -= 8 + Button.measure(g, runText, Button.Icon.PLAY);
        buttons.add(Button.draw(g, "run", runText, Button.Icon.PLAY, x, by, bh, HudStyle.BUTTON_GREEN, !running));

        g.setColor(new Color(255, 255, 255, 30));
        g.drawLine(0, HEADER_H - 1, rect.width(), HEADER_H - 1);
    }

    private void paintText(Graphics2D g, FontMetrics fm, boolean caretOn) {
        int top = textTop();
        int bottom = textBottom();
        g.setColor(GUTTER_BG);
        g.fillRect(2, top, GUTTER_W, bottom - top);

        Shape oldClip = g.getClip();
        g.clipRect(0, top, rect.width(), bottom - top);

        // Состояние многострочных комментариев нужно считать с начала файла.
        boolean inComment = false;
        for (int i = 0; i < scrollLine && i < doc.lineCount(); i++) {
            inComment = SyntaxHighlighter.tokenize(doc.line(i), inComment).endsInBlockComment();
        }

        int[] selStart = doc.hasSelection() ? doc.selectionStart() : null;
        int[] selEnd = doc.hasSelection() ? doc.selectionEnd() : null;
        int left = textLeft() - scrollX;
        int last = Math.min(doc.lineCount(), scrollLine + visibleLines() + 1);
        for (int i = scrollLine; i < last; i++) {
            int y = top + 4 + (i - scrollLine) * lineHeight;
            int baseline = y + fm.getAscent() + 1;
            String line = doc.line(i);

            if (i + 1 == errorLine) {
                g.setColor(ERROR_LINE);
                g.fillRect(GUTTER_W + 2, y, rect.width(), lineHeight);
            } else if (i == doc.caretLine() && focused) {
                g.setColor(CURRENT_LINE);
                g.fillRect(GUTTER_W + 2, y, rect.width(), lineHeight);
            }
            if (selStart != null && i >= selStart[0] && i <= selEnd[0]) {
                int from = i == selStart[0] ? selStart[1] : 0;
                int to = i == selEnd[0] ? selEnd[1] : line.length() + 1;
                g.setColor(SELECTION);
                g.fillRect(left + from * charWidth, y, Math.max(4, (to - from) * charWidth), lineHeight);
            }

            // Номер строки.
            g.setFont(HudStyle.CODE_SMALL);
            g.setColor(i + 1 == errorLine ? HudStyle.ERROR : i == doc.caretLine() ? HudStyle.TEXT_COLOR : LINE_NUMBER);
            String number = String.valueOf(i + 1);
            g.drawString(number, GUTTER_W - 6 - g.getFontMetrics().stringWidth(number), baseline);
            g.setFont(HudStyle.CODE);

            // Код с подсветкой (по символам — моноширинная сетка).
            Shape lineClip = g.getClip();
            g.clipRect(GUTTER_W + 4, top, rect.width() - GUTTER_W - 8, bottom - top);
            var tokens = SyntaxHighlighter.tokenize(line, inComment);
            for (var span : tokens.spans()) {
                g.setColor(SYNTAX.get(span.kind()));
                g.drawString(line.substring(span.start(), span.end()), left + span.start() * charWidth, baseline);
            }
            inComment = tokens.endsInBlockComment();

            if (caretOn && i == doc.caretLine()) {
                g.setColor(CARET);
                g.fillRect(left + doc.caretCol() * charWidth - 1, y + 1, 2, lineHeight - 2);
            }
            g.setClip(lineClip);
        }
        g.setClip(oldClip);

        // Полоса прокрутки.
        int total = doc.lineCount();
        int visible = visibleLines();
        if (total > visible) {
            int trackH = bottom - top - 8;
            int thumbH = Math.max(24, trackH * visible / total);
            int thumbY = top + 4 + (trackH - thumbH) * scrollLine / Math.max(1, total - visible);
            g.setColor(new Color(255, 255, 255, 50));
            g.fillRoundRect(rect.width() - 9, thumbY, 5, thumbH, 5, 5);
        }
    }

    private void paintErrorBar(Graphics2D g) {
        int y = textBottom();
        int h = errorBarHeight();
        g.setColor(new Color(110, 30, 25, 230));
        g.fillRect(2, y, rect.width() - 4, h);
        g.setFont(HudStyle.SMALL);
        g.setColor(HudStyle.ERROR);
        String prefix = errorLine > 0 ? "Строка " + errorLine + ": " : "";
        List<String> lines = TextWrap.wrap(prefix + errorMessage, g.getFontMetrics(), rect.width() - 28);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            g.drawString(lines.get(i), 14, y + 18 + i * 17);
        }
    }

    private void paintFooter(Graphics2D g) {
        int y = rect.height() - FOOTER_H;
        g.setColor(new Color(255, 255, 255, 30));
        g.drawLine(0, y, rect.width(), y);
        g.setFont(HudStyle.SMALL);
        g.setColor(HudStyle.MUTED);
        String hint = focused
                ? "Ctrl+Enter — запуск · Ctrl+Z — отмена · Ctrl+/ — комментарий · Esc — к камере"
                : "Клик по коду — писать · Мышь — камера · F — за роботом · H — скрыть панели";
        List<String> lines = TextWrap.wrap(hint, g.getFontMetrics(), rect.width() - 24);
        g.drawString(lines.getFirst(), 12, y + 20);
    }

    // ------------------------------------------------------------------ прокрутка

    private void ensureCaretVisible() {
        int line = doc.caretLine();
        int visible = visibleLines();
        if (line < scrollLine) {
            scrollLine = line;
        } else if (line >= scrollLine + visible) {
            scrollLine = line - visible + 1;
        }
        int caretX = doc.caretCol() * charWidth;
        int viewW = rect.width() - textLeft() - 16;
        if (caretX < scrollX) {
            scrollX = Math.max(0, caretX - 4 * charWidth);
        } else if (caretX > scrollX + viewW) {
            scrollX = caretX - viewW + 4 * charWidth;
        }
    }

    private void scrollToLine(int line) {
        int visible = visibleLines();
        if (line < scrollLine || line >= scrollLine + visible) {
            scrollLine = Math.max(0, line - visible / 2);
        }
    }

    private void scrollBy(int lines) {
        int max = Math.max(0, doc.lineCount() - visibleLines());
        scrollLine = Math.max(0, Math.min(max, scrollLine + lines));
    }

    // ------------------------------------------------------------------ клавиатура

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        int code = evt.getKeyCode();
        switch (code) {
            case KeyInput.KEY_LCONTROL, KeyInput.KEY_RCONTROL, KeyInput.KEY_LMETA, KeyInput.KEY_RMETA -> {
                ctrl = evt.isPressed();
                return;
            }
            case KeyInput.KEY_LSHIFT, KeyInput.KEY_RSHIFT -> {
                shift = evt.isPressed();
                return;
            }
            default -> {
            }
        }
        boolean down = evt.isPressed() || evt.isRepeating();
        // F5 запускает программу даже без фокуса на редакторе.
        if (code == KeyInput.KEY_F5 && down && !running && actions != null) {
            actions.run();
            evt.setConsumed();
            return;
        }
        if (!focused || !isEnabled()) {
            return;
        }
        evt.setConsumed();

        // Символы (включая кириллицу) приходят отдельными событиями без кода клавиши.
        if (code == KeyInput.KEY_UNKNOWN) {
            char c = evt.getKeyChar();
            if (evt.isPressed() && c >= 32 && c != 127 && !ctrl) {
                doc.type(c);
                clearErrorOnEdit();
            }
            return;
        }
        if (!down) {
            return;
        }
        if (ctrl) {
            handleShortcut(code);
            return;
        }
        switch (code) {
            case KeyInput.KEY_ESCAPE -> focused = false;
            case KeyInput.KEY_RETURN, KeyInput.KEY_NUMPADENTER -> edit(doc::newline);
            case KeyInput.KEY_BACK -> edit(doc::backspace);
            case KeyInput.KEY_DELETE -> edit(doc::delete);
            case KeyInput.KEY_TAB -> edit(shift ? doc::untab : doc::tab);
            case KeyInput.KEY_LEFT -> doc.moveLeft(shift, false);
            case KeyInput.KEY_RIGHT -> doc.moveRight(shift, false);
            case KeyInput.KEY_UP -> doc.moveVertical(-1, shift);
            case KeyInput.KEY_DOWN -> doc.moveVertical(1, shift);
            case KeyInput.KEY_HOME -> doc.home(shift);
            case KeyInput.KEY_END -> doc.end(shift);
            case KeyInput.KEY_PGUP -> doc.moveVertical(-visibleLines(), shift);
            case KeyInput.KEY_PGDN -> doc.moveVertical(visibleLines(), shift);
            default -> {
            }
        }
    }

    private void handleShortcut(int code) {
        switch (code) {
            case KeyInput.KEY_RETURN, KeyInput.KEY_NUMPADENTER -> {
                if (!running && actions != null) {
                    actions.run();
                }
            }
            case KeyInput.KEY_A -> doc.selectAll();
            case KeyInput.KEY_C -> {
                if (doc.hasSelection()) {
                    clipboard.set(doc.selectedText());
                }
            }
            case KeyInput.KEY_X -> {
                if (doc.hasSelection()) {
                    clipboard.set(doc.selectedText());
                    edit(doc::deleteSelection);
                }
            }
            case KeyInput.KEY_V -> {
                String text = clipboard.get();
                edit(() -> doc.insert(text));
            }
            case KeyInput.KEY_Z -> {
                if (shift) {
                    doc.redo();
                } else {
                    doc.undo();
                }
            }
            case KeyInput.KEY_Y -> doc.redo();
            case KeyInput.KEY_SLASH, KeyInput.KEY_DIVIDE -> edit(doc::toggleComment);
            case KeyInput.KEY_LEFT -> doc.moveLeft(shift, true);
            case KeyInput.KEY_RIGHT -> doc.moveRight(shift, true);
            case KeyInput.KEY_HOME -> doc.documentStart(shift);
            case KeyInput.KEY_END -> doc.documentEnd(shift);
            default -> {
            }
        }
    }

    private void edit(Runnable change) {
        change.run();
        clearErrorOnEdit();
    }

    private void clearErrorOnEdit() {
        // Сообщение остаётся, пока игрок не начнёт исправлять код.
        if (errorLine > 0 || !errorMessage.isEmpty()) {
            clearError();
        }
    }

    // ------------------------------------------------------------------ мышь

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        int[] local = panel.toLocal(evt.getX(), evt.getY());
        if (!evt.isPressed()) {
            if (dragging) {
                dragging = false;
                evt.setConsumed();
            }
            return;
        }
        if (local == null) {
            focused = false;
            return;
        }
        evt.setConsumed();
        focused = true;
        for (Button b : buttons) {
            if (b.contains(local[0], local[1])) {
                onButton(b.id());
                return;
            }
        }
        if (local[1] >= textTop() && local[1] < textBottom()) {
            placeCaret(local, shift);
            dragging = true;
        }
    }

    private void onButton(String id) {
        if (actions == null) {
            return;
        }
        switch (id) {
            case "run" -> actions.run();
            case "stop" -> actions.stop();
            case "reset" -> actions.resetCode();
            default -> {
            }
        }
    }

    private void placeCaret(int[] local, boolean extend) {
        int line = scrollLine + (local[1] - textTop() - 4) / lineHeight;
        int col = Math.round((local[0] - textLeft() + scrollX) / (float) charWidth);
        doc.setCaret(line, col, extend);
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        if (dragging) {
            int localX = evt.getX() - rect.x();
            int localY = rect.height() - (evt.getY() - rect.y());
            placeCaret(new int[]{localX, localY}, true);
            evt.setConsumed();
            return;
        }
        if (evt.getDeltaWheel() != 0 && panel.toLocal(evt.getX(), evt.getY()) != null) {
            scrollBy(evt.getDeltaWheel() > 0 ? -3 : 3);
            panel.invalidate();
            evt.setConsumed();
        }
    }

    @Override
    public void beginInput() {
    }

    @Override
    public void endInput() {
    }

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {
    }

    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
    }

    @Override
    public void onTouchEvent(TouchEvent evt) {
    }
}
