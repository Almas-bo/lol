package com.farmgame.sandbox.lesson;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;
import com.farmgame.core.farm.PlotState;

import java.util.regex.Pattern;

/** Готовые проверки для уроков. Каждая даёт понятное игроку сообщение, если цель не достигнута. */
public final class Goals {

    private Goals() {
    }

    /** Цели нет — «свободный режим». */
    public static LessonGoal none() {
        return context -> GoalResult.pass("Свободный режим: экспериментируйте!");
    }

    /** Робот стоит в клетке (x, y). */
    public static LessonGoal robotAt(int x, int y) {
        return context -> {
            GridPosition p = context.farm().robotPosition();
            return p.equals(new GridPosition(x, y))
                    ? GoalResult.pass("Робот на месте!")
                    : GoalResult.fail("Робот стоит в " + p + ", а нужно в (" + x + ", " + y + ")");
        };
    }

    /**
     * На грядке (x, y) растёт политая культура нужного типа.
     *
     * @param type       нужная культура или {@code null} — любая
     * @param fertilized требуется ли удобрение
     */
    public static LessonGoal planted(int x, int y, CropType type, boolean fertilized) {
        return context -> checkPlot(context.farm(), x, y, type, fertilized);
    }

    /** Весь ряд {@code y} засажен и полит. */
    public static LessonGoal rowPlanted(int y, CropType type) {
        return context -> {
            for (int x = 0; x < context.farm().width(); x++) {
                GoalResult r = checkPlot(context.farm(), x, y, type, false);
                if (!r.passed()) {
                    return r;
                }
            }
            return GoalResult.pass("Ряд " + y + " засажен!");
        };
    }

    /** Все грядки фермы засажены и политы. */
    public static LessonGoal wholeFieldPlanted() {
        return context -> {
            for (int y = 0; y < context.farm().height(); y++) {
                for (int x = 0; x < context.farm().width(); x++) {
                    GoalResult r = checkPlot(context.farm(), x, y, null, false);
                    if (!r.passed()) {
                        return r;
                    }
                }
            }
            return GoalResult.pass("Всё поле засажено!");
        };
    }

    /** Грядки на диагонали (i, i) засажены и политы. */
    public static LessonGoal diagonalPlanted() {
        return context -> {
            int n = Math.min(context.farm().width(), context.farm().height());
            for (int i = 0; i < n; i++) {
                GoalResult r = checkPlot(context.farm(), i, i, null, false);
                if (!r.passed()) {
                    return r;
                }
            }
            return GoalResult.pass("Диагональ засажена!");
        };
    }

    /** В ряду {@code y} не осталось созревших культур. */
    public static LessonGoal noRipeInRow(int y) {
        return context -> {
            for (int x = 0; x < context.farm().width(); x++) {
                if (context.farm().plotAt(new GridPosition(x, y)).isRipe()) {
                    return GoalResult.fail("На грядке (" + x + ", " + y + ") остался спелый урожай");
                }
            }
            return GoalResult.pass("Весь спелый урожай собран!");
        };
    }

    /** В ряду {@code y} остались несобранные (незрелые) культуры — их нельзя было трогать. */
    public static LessonGoal unripeKeptInRow(int y, int expected) {
        return context -> {
            int count = 0;
            for (int x = 0; x < context.farm().width(); x++) {
                PlotState plot = context.farm().plotAt(new GridPosition(x, y));
                if (!plot.isEmpty() && !plot.isRipe()) {
                    count++;
                }
            }
            return count >= expected
                    ? GoalResult.pass("Незрелые растения сохранены")
                    : GoalResult.fail("Незрелых растений в ряду " + y + " осталось " + count + " из " + expected);
        };
    }

    /** На складе не меньше {@code amount} единиц культуры. */
    public static LessonGoal harvested(CropType type, int amount) {
        return context -> {
            int have = context.farm().inventorySnapshot().getOrDefault(type, 0);
            return have >= amount
                    ? GoalResult.pass("Собрано: " + type.displayName() + " × " + have)
                    : GoalResult.fail("На складе " + type.displayName() + ": " + have + ", нужно минимум " + amount);
        };
    }

    /** Программа хотя бы раз вызвала {@code robot.say(...)} с текстом, содержащим {@code fragment}. */
    public static LessonGoal said(String fragment) {
        return context -> context.said().stream().anyMatch(s -> s.contains(fragment))
                ? GoalResult.pass("Робот сказал то, что нужно!")
                : GoalResult.fail("Робот должен сказать фразу, содержащую «" + fragment + "»"
                + (context.said().isEmpty() ? " (он пока молчит)" : ""));
    }

    /**
     * Исходный код содержит нужную конструкцию. Такие проверки учат не только результату,
     * но и приёму программирования (цикл, метод, класс...).
     */
    public static LessonGoal codeContains(String regex, String explanation) {
        Pattern pattern = Pattern.compile(regex);
        return context -> pattern.matcher(stripComments(context.source())).find()
                ? GoalResult.pass("Код использует нужную конструкцию")
                : GoalResult.fail(explanation);
    }

    /** Код содержит {@code regex} не меньше {@code times} раз. */
    public static LessonGoal codeContainsAtLeast(String regex, int times, String explanation) {
        Pattern pattern = Pattern.compile(regex);
        return context -> {
            var matcher = pattern.matcher(stripComments(context.source()));
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            return count >= times ? GoalResult.pass("Код использует нужную конструкцию") : GoalResult.fail(explanation);
        };
    }

    private static GoalResult checkPlot(Farm farm, int x, int y, CropType type, boolean fertilized) {
        PlotState plot = farm.plotAt(new GridPosition(x, y));
        String where = "Грядка (" + x + ", " + y + ")";
        if (plot.isEmpty()) {
            return GoalResult.fail(where + " пустая");
        }
        if (type != null && plot.crop().type() != type) {
            return GoalResult.fail(where + ": растёт " + plot.crop().type() + ", а нужно " + type);
        }
        if (fertilized && !plot.crop().fertilized()) {
            return GoalResult.fail(where + ": культура не удобрена — используйте setFertilized(true)");
        }
        if (!plot.watered()) {
            return GoalResult.fail(where + " не полита — вызовите robot.water()");
        }
        return GoalResult.pass(where + " в порядке");
    }

    /** Убирает комментарии, чтобы проверка кода не срабатывала на «// for ...». */
    static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
    }
}
