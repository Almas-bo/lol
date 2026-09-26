package com.farmgame.engine.state;

import com.farmgame.core.farm.Farm;
import com.farmgame.engine.program.ProgramSession;
import com.farmgame.engine.program.ProgramSession.Outcome;
import com.farmgame.engine.ui.Button;
import com.farmgame.engine.ui.GameConsole;
import com.farmgame.engine.ui.HudPanel;
import com.farmgame.engine.ui.HudStyle;
import com.farmgame.engine.ui.TextWrap;
import com.farmgame.engine.ui.UiLayout;
import com.farmgame.sandbox.compiler.CompilationResult;
import com.farmgame.sandbox.compiler.CompilerHints;
import com.farmgame.sandbox.lesson.GoalResult;
import com.farmgame.sandbox.lesson.Lesson;
import com.farmgame.sandbox.lesson.LessonContext;
import com.farmgame.sandbox.lesson.LessonProgress;
import com.farmgame.sandbox.runtime.ExecutionResult;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
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
import java.util.List;

/**
 * Учитель: ведёт игрока по курсу.
 *
 * <p>Показывает панель урока (теория, задание, подсказка, результат проверки),
 * по кнопке «Запустить» готовит ферму, компилирует и выполняет код из редактора,
 * а затем проверяет цель урока и сохраняет прогресс.
 */
public class LessonState extends BaseAppState implements CodeEditorState.Actions, RawInputListener {

    /** После стольких неудачных попыток появляется кнопка «Решение». */
    private static final int ATTEMPTS_BEFORE_SOLUTION = 3;
    private static final int BUTTONS_H = 52;

    private enum Status { IDLE, RUNNING, PASSED, FAILED, ERROR }

    private final List<Lesson> lessons;
    private final LessonProgress progress;
    private final Farm farm;
    private final ProgramSession session;
    private final GameConsole console;
    private final UiLayout.Rect rect;

    private HudPanel panel;
    private CodeEditorState editor;
    private final List<Button> buttons = new ArrayList<>();

    private int index;
    private Status status = Status.IDLE;
    private String statusMessage = "";
    private boolean hintShown;
    private int failedAttempts;
    private int runGeneration;
    private int scroll;
    private int contentHeight;
    private int redrawTick;
    private boolean opened;

    public LessonState(List<Lesson> lessons, LessonProgress progress, Farm farm, ProgramSession session,
                       GameConsole console, UiLayout.Rect rect) {
        this.lessons = lessons;
        this.progress = progress;
        this.farm = farm;
        this.session = session;
        this.console = console;
        this.rect = rect;
    }

    // ------------------------------------------------------------------ жизненный цикл

    @Override
    protected void initialize(Application app) {
        panel = new HudPanel("lesson", app.getAssetManager(), rect.width(), rect.height());
        panel.picture().setPosition(rect.x(), rect.y());
        editor = getState(CodeEditorState.class);
        editor.setActions(this);
        openLesson(startIndex());
        console.print("Добро пожаловать на ферму! Читай урок слева, пиши код справа.");
    }

    /** Урок, на котором игрок остановился в прошлый раз, или первый непройденный. */
    private int startIndex() {
        String saved = progress.currentLesson();
        for (int i = 0; i < lessons.size(); i++) {
            if (lessons.get(i).id().equals(saved) && progress.isUnlocked(lessons, i)) {
                return i;
            }
        }
        for (int i = 0; i < lessons.size(); i++) {
            if (!progress.isCompleted(lessons.get(i).id())) {
                return i;
            }
        }
        return lessons.size() - 1;
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
        session.stop();
        saveCode();
        progress.save();
    }

    @Override
    public void update(float tpf) {
        redrawTick++;
        String key = index + "|" + status + "|" + statusMessage + "|" + hintShown + "|" + failedAttempts + "|"
                + scroll + "|" + (status == Status.RUNNING ? redrawTick / 20 : 0);
        panel.redraw(key, this::paint);
    }

    // ------------------------------------------------------------------ навигация

    private Lesson lesson() {
        return lessons.get(index);
    }

    private void openLesson(int newIndex) {
        session.stop();
        runGeneration++;
        if (opened) {
            saveCode();
        }
        opened = true;
        index = newIndex;
        progress.setCurrentLesson(lesson().id());
        progress.save();

        status = Status.IDLE;
        statusMessage = "";
        hintShown = false;
        failedAttempts = 0;
        scroll = 0;

        editor.load(progress.savedCode(lesson().id()).orElse(lesson().starterCode()));
        editor.setRunning(false);
        editor.setTitle("Урок " + (index + 1) + " · MyFarm.java");
        editor.focus();
        prepareFarm();
    }

    private void prepareFarm() {
        getState(RobotCommandState.class).cancelCurrent();
        lesson().prepare(farm);
        getState(RobotCommandState.class).snapToFarm();
    }

    private void saveCode() {
        progress.saveCode(lesson().id(), editor.code());
    }

    // ------------------------------------------------------------------ действия редактора

    @Override
    public void run() {
        saveCode();
        progress.save();
        session.stop();
        prepareFarm();

        int generation = ++runGeneration;
        status = Status.RUNNING;
        statusMessage = "Робот выполняет программу…";
        editor.setRunning(true);
        editor.clearError();
        console.clear();
        console.print("▶ Запуск: " + lesson().title());

        String source = editor.code();
        session.run(source, console::print).whenComplete((outcome, error) ->
                getApplication().enqueue(() -> finish(generation, source, outcome, error)));
    }

    @Override
    public void stop() {
        session.stop();
    }

    @Override
    public void resetCode() {
        editor.replace(lesson().starterCode());
        console.print("Код урока восстановлен. Передумал? Нажми Ctrl+Z в редакторе.");
    }

    /** Обработка итога запуска — в потоке рендеринга. */
    private void finish(int generation, String source, Outcome outcome, Throwable error) {
        if (generation != runGeneration) {
            return; // запуск устарел: игрок перезапустил программу или сменил урок
        }
        editor.setRunning(false);
        if (error != null) {
            fail(Status.ERROR, "Внутренняя ошибка: " + error.getMessage());
            return;
        }
        switch (outcome) {
            case Outcome.CompileError ce -> onCompileError(ce.failure());
            case Outcome.Finished f -> onFinished(f.result(), f.said(), source);
        }
    }

    private void onCompileError(CompilationResult.Failure failure) {
        CompilationResult.Diagnostic first = failure.diagnostics().getFirst();
        String hint = CompilerHints.hintFor(first.message());
        String firstLine = first.message().lines().findFirst().orElse(first.message());
        editor.setError((int) first.line(), hint.isEmpty() ? firstLine : hint + " (" + firstLine + ")");
        console.print("Ошибка компиляции:");
        for (CompilationResult.Diagnostic d : failure.diagnostics()) {
            console.print(d.toString().lines().findFirst().orElse(""));
        }
        fail(Status.ERROR, "Код не скомпилировался. " + (hint.isEmpty() ? "Смотри строку, подсвеченную красным." : hint));
    }

    private void onFinished(ExecutionResult result, List<String> said, String source) {
        switch (result.status()) {
            case SUCCESS -> checkGoal(said, source);
            case STOPPED -> {
                status = Status.IDLE;
                statusMessage = "Программа остановлена.";
                console.print("■ Остановлено");
            }
            case TIMEOUT -> fail(Status.ERROR, "Программа работала слишком долго. Нет ли бесконечного цикла?");
            case ERROR -> {
                if (result.line() > 0) {
                    editor.setError(result.line(), result.message());
                }
                console.print("Ошибка выполнения: " + result.message());
                fail(Status.ERROR, "Во время работы произошла ошибка: " + result.message());
            }
        }
    }

    private void checkGoal(List<String> said, String source) {
        GoalResult goal = lesson().goal().check(new LessonContext(farm, said, source));
        if (goal.passed()) {
            boolean first = !progress.isCompleted(lesson().id());
            progress.markCompleted(lesson().id());
            progress.save();
            status = Status.PASSED;
            statusMessage = goal.message() + (index + 1 < lessons.size() ? " Жми «Далее»!" : "");
            console.print("✓ Задание выполнено!");
            if (first) {
                EffectsState effects = getState(EffectsState.class);
                if (effects != null) {
                    effects.celebrate(getState(RobotCommandState.class).robotWorldPosition());
                }
                AmbientLifeState life = getState(AmbientLifeState.class);
                if (life != null) {
                    life.cheer();
                }
            }
        } else {
            console.print("✗ " + goal.message());
            fail(Status.FAILED, goal.message());
        }
    }

    private void fail(Status newStatus, String message) {
        status = newStatus;
        statusMessage = message;
        failedAttempts++;
    }

    private void onButton(String id) {
        switch (id) {
            case "prev" -> openLesson(index - 1);
            case "next" -> openLesson(index + 1);
            case "hint" -> hintShown = !hintShown;
            case "solution" -> {
                editor.replace(lesson().solution());
                console.print("Загружено решение. Разберись, как оно работает, и нажми «Запустить».");
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ отрисовка

    private void paint(Graphics2D g) {
        int w = rect.width();
        int h = rect.height();
        HudStyle.background(g, w, h);

        Shape clip = g.getClip();
        g.clipRect(0, 0, w, h - BUTTONS_H);
        int y = 14 - scroll;
        y = paintHeader(g, y, w);
        y = paintTheory(g, y, w);
        y = paintTask(g, y, w);
        if (hintShown) {
            y = paintBox(g, y, w, "Подсказка", lesson().hint(), HudStyle.INFO, new Color(40, 70, 110, 150));
        }
        y = paintStatus(g, y, w);
        contentHeight = y + scroll + 10;
        g.setClip(clip);

        int viewH = h - BUTTONS_H;
        if (contentHeight > viewH) {
            int thumbH = Math.max(24, viewH * viewH / contentHeight);
            int thumbY = (viewH - thumbH) * scroll / Math.max(1, contentHeight - viewH);
            g.setColor(new Color(255, 255, 255, 60));
            g.fillRoundRect(w - 8, thumbY + 4, 4, thumbH - 8, 4, 4);
        }
        paintButtons(g, w, h);
    }

    private int paintHeader(Graphics2D g, int y, int w) {
        g.setFont(HudStyle.SMALL);
        g.setColor(HudStyle.MUTED);
        g.drawString("Урок " + (index + 1) + " из " + lessons.size(), 16, y + 14);
        // Точки прогресса курса.
        int dotX = w - 16 - lessons.size() * 12;
        for (int i = 0; i < lessons.size(); i++) {
            boolean done = progress.isCompleted(lessons.get(i).id());
            g.setColor(i == index ? HudStyle.ACCENT : done ? HudStyle.SUCCESS : new Color(255, 255, 255, 50));
            g.fillOval(dotX + i * 12, y + 5, 8, 8);
        }
        y += 24;
        g.setFont(HudStyle.HEADING);
        g.setColor(HudStyle.TEXT_COLOR);
        for (String line : TextWrap.wrap(lesson().title(), g.getFontMetrics(), w - 32)) {
            y += 24;
            g.drawString(line, 16, y);
        }
        // Тема урока — «чип».
        g.setFont(HudStyle.KEY);
        FontMetrics fm = g.getFontMetrics();
        String topic = "Тема: " + lesson().topic();
        y += 10;
        g.setColor(new Color(0xFF, 0xC8, 0x3D, 45));
        g.fillRoundRect(16, y, fm.stringWidth(topic) + 16, 22, 11, 11);
        g.setColor(HudStyle.ACCENT);
        g.drawString(topic, 24, y + 16);
        return y + 34;
    }

    private int paintTheory(Graphics2D g, int y, int w) {
        List<String> block = new ArrayList<>();
        for (String raw : lesson().theory().strip().split("\n")) {
            if (raw.startsWith("  ")) {
                block.add(raw.substring(2));
                continue;
            }
            y = flushCode(g, y, w, block);
            if (raw.isBlank()) {
                y += 6;
                continue;
            }
            g.setFont(HudStyle.TEXT);
            g.setColor(HudStyle.TEXT_COLOR);
            for (String line : TextWrap.wrap(raw, g.getFontMetrics(), w - 32)) {
                y += 19;
                g.drawString(line, 16, y);
            }
            y += 4;
        }
        return flushCode(g, y, w, block) + 6;
    }

    /** Рисует накопленный блок кода на тёмной подложке. */
    private int flushCode(Graphics2D g, int y, int w, List<String> block) {
        if (block.isEmpty()) {
            return y;
        }
        g.setFont(HudStyle.CODE_SMALL);
        FontMetrics fm = g.getFontMetrics();
        int lineH = fm.getHeight() + 1;
        int boxH = block.size() * lineH + 12;
        g.setColor(new Color(0, 0, 0, 110));
        g.fillRoundRect(16, y + 4, w - 32, boxH, 8, 8);
        int ty = y + 8 + fm.getAscent();
        Shape clip = g.getClip();
        g.clipRect(16, y + 4, w - 32, boxH);
        for (String line : block) {
            g.setColor(line.stripLeading().startsWith("//") ? new Color(0x80, 0x8E, 0x84) : new Color(0xB5, 0xE0, 0x7A));
            g.drawString(line, 26, ty);
            ty += lineH;
        }
        g.setClip(clip);
        block.clear();
        return y + boxH + 10;
    }

    private int paintTask(Graphics2D g, int y, int w) {
        return paintBox(g, y, w, "Задание", lesson().task(), HudStyle.ACCENT, new Color(90, 70, 20, 150));
    }

    private int paintBox(Graphics2D g, int y, int w, String title, String text, Color accent, Color fill) {
        g.setFont(HudStyle.TEXT);
        List<String> lines = TextWrap.wrap(text, g.getFontMetrics(), w - 52);
        int boxH = 34 + lines.size() * 19;
        g.setColor(fill);
        g.fillRoundRect(16, y, w - 32, boxH, 10, 10);
        g.setColor(accent);
        g.fillRoundRect(16, y, 4, boxH, 4, 4);
        g.setFont(HudStyle.KEY);
        g.drawString(title, 30, y + 20);
        g.setFont(HudStyle.TEXT);
        g.setColor(HudStyle.TEXT_COLOR);
        int ty = y + 20;
        for (String line : lines) {
            ty += 19;
            g.drawString(line, 30, ty);
        }
        return y + boxH + 10;
    }

    private int paintStatus(Graphics2D g, int y, int w) {
        String title;
        Color accent;
        Color fill;
        String text = statusMessage;
        switch (status) {
            case RUNNING -> {
                title = "Выполняется" + ".".repeat(redrawTick / 20 % 4);
                accent = HudStyle.INFO;
                fill = new Color(40, 60, 90, 150);
            }
            case PASSED -> {
                title = "Задание выполнено!";
                accent = HudStyle.SUCCESS;
                fill = new Color(40, 90, 40, 170);
            }
            case FAILED -> {
                title = "Почти! Цель ещё не достигнута";
                accent = HudStyle.ERROR;
                fill = new Color(100, 45, 35, 160);
            }
            case ERROR -> {
                title = "Ошибка";
                accent = HudStyle.ERROR;
                fill = new Color(100, 35, 30, 170);
            }
            default -> {
                if (progress.isCompleted(lesson().id())) {
                    title = "Урок уже пройден";
                    accent = HudStyle.SUCCESS;
                    fill = new Color(40, 80, 40, 120);
                    text = "Можно поэкспериментировать или перейти дальше.";
                } else {
                    title = "Готов?";
                    accent = HudStyle.MUTED;
                    fill = new Color(60, 70, 64, 120);
                    text = text.isEmpty() ? "Напиши код и нажми «Запустить» (Ctrl+Enter)." : text;
                }
            }
        }
        if (failedAttempts >= ATTEMPTS_BEFORE_SOLUTION && status != Status.PASSED) {
            text += " Застрял? Можно открыть подсказку или посмотреть решение.";
        }
        return paintBox(g, y, w, title, text, accent, fill);
    }

    private void paintButtons(Graphics2D g, int w, int h) {
        buttons.clear();
        int y = h - BUTTONS_H + 10;
        g.setColor(new Color(255, 255, 255, 30));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(0, h - BUTTONS_H, w, h - BUTTONS_H);

        boolean showSolution = failedAttempts >= ATTEMPTS_BEFORE_SOLUTION || progress.isCompleted(lesson().id());
        boolean canNext = index + 1 < lessons.size() && progress.isUnlocked(lessons, index + 1);
        // Полные подписи, а если панель узкая — короткие.
        String[] full = {"Назад", hintShown ? "Скрыть" : "Подсказка", "Решение", "Далее"};
        String[] compact = {"", hintShown ? "Скрыть" : "Совет", "Ответ", "Далее"};
        String[] labels = fits(g, full, showSolution, w) ? full : compact;

        int x = 14;
        buttons.add(Button.draw(g, "prev", labels[0], Button.Icon.PREV, x, y, 32, HudStyle.BUTTON_GRAY, index > 0));
        x += buttons.getLast().width() + 8;
        buttons.add(Button.draw(g, "hint", labels[1], Button.Icon.BULB, x, y, 32, HudStyle.BUTTON_BLUE, true));
        x += buttons.getLast().width() + 8;
        if (showSolution) {
            buttons.add(Button.draw(g, "solution", labels[2], Button.Icon.KEY, x, y, 32, HudStyle.BUTTON_AMBER,
                    status != Status.RUNNING));
        }
        int nextW = Button.measure(g, labels[3], Button.Icon.NEXT);
        buttons.add(Button.draw(g, "next", labels[3], Button.Icon.NEXT, w - 14 - nextW, y, 32,
                status == Status.PASSED ? HudStyle.BUTTON_GREEN : HudStyle.BUTTON_GRAY, canNext));
    }

    private static boolean fits(Graphics2D g, String[] labels, boolean withSolution, int panelWidth) {
        int total = 14 + Button.measure(g, labels[0], Button.Icon.PREV) + 8
                + Button.measure(g, labels[1], Button.Icon.BULB) + 8
                + (withSolution ? Button.measure(g, labels[2], Button.Icon.KEY) + 8 : 0)
                + Button.measure(g, labels[3], Button.Icon.NEXT) + 14;
        return total <= panelWidth;
    }

    // ------------------------------------------------------------------ ввод

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (!evt.isPressed()) {
            return;
        }
        int[] local = panel.toLocal(evt.getX(), evt.getY());
        if (local == null) {
            return;
        }
        evt.setConsumed();
        for (Button b : buttons) {
            if (b.contains(local[0], local[1])) {
                onButton(b.id());
                return;
            }
        }
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {
        if (evt.getDeltaWheel() != 0 && panel.toLocal(evt.getX(), evt.getY()) != null) {
            int max = Math.max(0, contentHeight - (rect.height() - BUTTONS_H));
            scroll = Math.max(0, Math.min(max, scroll + (evt.getDeltaWheel() > 0 ? -40 : 40)));
            evt.setConsumed();
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
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
