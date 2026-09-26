package com.farmgame.sandbox.lesson;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.Crop;
import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;

import java.util.List;
import java.util.function.Consumer;

/**
 * Курс «Java на ферме»: от первого вызова метода до собственного класса.
 * Уроки идут по порядку; следующий открывается после выполнения предыдущего.
 */
public final class LessonCatalog {

    private static final String IMPORTS = """
            import com.farmgame.core.crop.*;
            import com.farmgame.sandbox.api.*;

            """;

    private static final Consumer<Farm> EMPTY_FARM = farm -> { };

    private LessonCatalog() {
    }

    /** Все уроки курса в порядке прохождения. */
    public static List<Lesson> lessons() {
        return List.of(
                firstCommand(),
                sequence(),
                builder(),
                variables(),
                forLoop(),
                ifCondition(),
                whileLoop(),
                methods(),
                nestedLoops(),
                classes(),
                freeMode());
    }

    /** Оборачивает тело метода run в полноценный класс программы. */
    static String program(String body) {
        return IMPORTS + """
                public class MyFarm implements FarmProgram {

                    @Override
                    public void run(RobotApi robot, FarmApi farm) {
                """ + body.indent(8) + """
                    }
                }
                """;
    }

    // ------------------------------------------------------------------ 1

    private static Lesson firstCommand() {
        return new Lesson("first-command", "Первая команда", "Вызов метода",
                """
                Привет! Это твой робот-трактор. Он понимает команды на языке Java.

                Команда роботу — это вызов метода. Пишем имя объекта, точку, имя метода и аргументы в скобках. В конце — точка с запятой:
                  robot.moveTo(2, 1);

                Числа в скобках — координаты клетки: x (вдоль поля) и y (вглубь). Подписи x0…x7 и y0…y5 видны по краям поля.
                """,
                "Отправь робота в клетку x = 3, y = 2.",
                "Внутри метода run напиши одну строку: robot.moveTo(3, 2); — не забудь точку с запятой.",
                program("// Напиши команду здесь:\n\n"),
                program("robot.moveTo(3, 2);\n"),
                EMPTY_FARM,
                Goals.robotAt(3, 2));
    }

    // ------------------------------------------------------------------ 2

    private static Lesson sequence() {
        return new Lesson("sequence", "Посадка", "Последовательность команд",
                """
                Программа выполняется сверху вниз: строка за строкой.

                Чтобы вырастить морковь, робот должен доехать до грядки, посадить семя и полить его:
                  robot.moveTo(1, 1);
                  robot.plant(Crop.of(CropType.CARROT));
                  robot.water();

                Crop.of(...) создаёт объект «культура». CropType — перечисление (enum) всех видов: CORN, WHEAT, CARROT, PUMPKIN.
                """,
                "Посади морковь (CARROT) на грядку (2, 1) и полей её.",
                "Три команды по порядку: moveTo(2, 1), plant(Crop.of(CropType.CARROT)), water().",
                program("robot.moveTo(2, 1);\n// посади и полей морковь\n\n"),
                program("""
                        robot.moveTo(2, 1);
                        robot.plant(Crop.of(CropType.CARROT));
                        robot.water();
                        """),
                EMPTY_FARM,
                Goals.planted(2, 1, CropType.CARROT, false));
    }

    // ------------------------------------------------------------------ 3

    private static Lesson builder() {
        return new Lesson("builder", "Строитель культур", "Паттерн Builder",
                """
                Иногда у объекта много настроек. Чтобы не путаться в длинном конструкторе, используют паттерн Builder («строитель»): настройки задаются по одной, а build() собирает готовый объект.
                  Crop corn = Crop.builder()
                          .setType(CropType.CORN)
                          .setName("Кукуруза у сарая")
                          .setFertilized(true)
                          .build();

                Каждый setXxx(...) возвращает тот же строитель, поэтому вызовы идут цепочкой. Удобрение (setFertilized(true)) удваивает урожай.
                """,
                "Посади УДОБРЕННУЮ кукурузу на грядку (4, 2) с помощью Crop.builder() и полей её.",
                "Создай переменную: Crop corn = Crop.builder().setType(CropType.CORN).setFertilized(true).build(); затем moveTo, plant(corn), water.",
                program("""
                        Crop corn = Crop.builder()
                                .setType(CropType.CORN)
                                // добавь удобрение
                                .build();

                        """),
                program("""
                        Crop corn = Crop.builder()
                                .setType(CropType.CORN)
                                .setFertilized(true)
                                .build();
                        robot.moveTo(4, 2);
                        robot.plant(corn);
                        robot.water();
                        """),
                EMPTY_FARM,
                Goals.planted(4, 2, CropType.CORN, true)
                        .and(Goals.codeContains("Crop\\s*\\.\\s*builder\\s*\\(", "Создай культуру через Crop.builder()")));
    }

    // ------------------------------------------------------------------ 4

    private static Lesson variables() {
        return new Lesson("variables", "Разговорчивый робот", "Переменные и строки",
                """
                Переменная — это именованная «коробка» для значения. У каждой переменной есть тип:
                  int steps = 5;              // целое число
                  String name = "Тракторыч";  // строка текста

                Строки склеиваются оператором +, а числа внутри строки превращаются в текст:
                  robot.say("Меня зовут " + name + ", я проеду " + steps + " клеток");

                robot.say(...) выводит сообщение в консоль игры (слева внизу).
                """,
                "Заведи переменную с именем робота и пусть он поздоровается: фраза должна начинаться с «Привет».",
                "String name = \"Робби\"; robot.say(\"Привет, я \" + name);",
                program("String name = \"...\";\n\n"),
                program("""
                        String name = "Робби";
                        int x = 5;
                        robot.say("Привет, я " + name + "! Еду в клетку " + x);
                        robot.moveTo(x, 0);
                        """),
                EMPTY_FARM,
                Goals.said("Привет")
                        .and(Goals.codeContains("\\b(String|int|double|var)\\s+\\w+\\s*=", "Объяви хотя бы одну переменную, например String name = \"Робби\";")));
    }

    // ------------------------------------------------------------------ 5

    private static Lesson forLoop() {
        return new Lesson("for-loop", "Целый ряд", "Цикл for",
                """
                Засадить 8 грядок восемью одинаковыми кусками кода — скучно. Цикл повторяет код сам:
                  for (int x = 0; x < 8; x++) {
                      robot.moveTo(x, 0);
                  }

                Три части в скобках: с чего начать (int x = 0), пока что выполнять (x < 8), что делать после каждого шага (x++ — увеличить на 1).

                Ширину поля лучше не писать числом, а спросить у фермы: farm.getWidth().
                """,
                "С помощью цикла for засади пшеницей (WHEAT) весь ряд y = 0 и полей каждую грядку.",
                "for (int x = 0; x < farm.getWidth(); x++) { robot.moveTo(x, 0); robot.plant(Crop.of(CropType.WHEAT)); robot.water(); }",
                program("for (int x = 0; x < farm.getWidth(); x++) {\n    // что делать на каждой грядке?\n}\n"),
                program("""
                        for (int x = 0; x < farm.getWidth(); x++) {
                            robot.moveTo(x, 0);
                            robot.plant(Crop.of(CropType.WHEAT));
                            robot.water();
                        }
                        """),
                EMPTY_FARM,
                Goals.rowPlanted(0, CropType.WHEAT)
                        .and(Goals.codeContains("\\bfor\\s*\\(", "Используй цикл for, а не отдельные команды")));
    }

    // ------------------------------------------------------------------ 6

    private static Lesson ifCondition() {
        return new Lesson("if-condition", "Только спелое", "Условие if",
                """
                В ряду y = 2 растут морковь и пшеница. Часть уже созрела, часть — нет.

                Условие if выполняет код, только если выражение в скобках истинно (true):
                  if (farm.isRipe(x, 2)) {
                      robot.harvest();
                  }

                Можно добавить else — что делать в противном случае. Сравнения: ==, !=, <, >; логика: && (и), || (или), ! (не).
                """,
                "Пройди по ряду y = 2 и собери урожай ТОЛЬКО со спелых грядок. Незрелые оставь расти.",
                "Внутри цикла for по x: сначала проверь if (farm.isRipe(x, 2)), и только тогда moveTo и harvest().",
                program("for (int x = 0; x < farm.getWidth(); x++) {\n    // проверь, созрела ли грядка (x, 2)\n}\n"),
                program("""
                        int total = 0;
                        for (int x = 0; x < farm.getWidth(); x++) {
                            if (farm.isRipe(x, 2)) {
                                robot.moveTo(x, 2);
                                total += robot.harvest();
                            }
                        }
                        robot.say("Собрано: " + total);
                        """),
                LessonCatalog::mixedRow,
                Goals.noRipeInRow(2)
                        .and(Goals.harvested(CropType.CARROT, 4))
                        .and(Goals.unripeKeptInRow(2, 4))
                        .and(Goals.codeContains("\\bif\\s*\\(", "Используй условие if, чтобы проверять спелость")));
    }

    /** Ряд y = 2: на чётных x спелая морковь, на нечётных — неполитая (незрелая) пшеница. */
    private static void mixedRow(Farm farm) {
        for (int x = 0; x < farm.width(); x++) {
            farm.moveRobot(new GridPosition(x, 2));
            if (x % 2 == 0) {
                farm.plantAtRobot(Crop.of(CropType.CARROT));
                farm.waterAtRobot();
            } else {
                farm.plantAtRobot(Crop.of(CropType.WHEAT));
            }
        }
        farm.tick(CropType.CARROT.growthSeconds());
    }

    // ------------------------------------------------------------------ 7

    private static Lesson whileLoop() {
        return new Lesson("while-loop", "Терпение", "Цикл while",
                """
                Цикл for удобен, когда известно число повторений. А если нужно ждать, пока что-то случится? Для этого есть while — он повторяет код, ПОКА условие истинно:
                  while (!farm.isRipe(3, 3)) {
                      robot.pause(1);
                  }

                ! означает «не»: «пока грядка НЕ созрела — подожди секунду». Когда условие станет ложным, программа пойдёт дальше.

                Осторожно: если условие никогда не станет ложным, цикл будет вечным — тогда нажми «Стоп».
                """,
                "Посади тыкву (PUMPKIN) на грядку (3, 3), полей, дождись созревания циклом while и собери урожай.",
                "После plant и water: while (!farm.isRipe(3, 3)) { robot.pause(1); } и затем robot.harvest();",
                program("robot.moveTo(3, 3);\nrobot.plant(Crop.of(CropType.PUMPKIN));\nrobot.water();\n\n// дождись, пока тыква созреет\n\n"),
                program("""
                        robot.moveTo(3, 3);
                        robot.plant(Crop.of(CropType.PUMPKIN));
                        robot.water();
                        while (!farm.isRipe(3, 3)) {
                            robot.say("Растёт...");
                            robot.pause(2);
                        }
                        robot.harvest();
                        """),
                EMPTY_FARM,
                Goals.harvested(CropType.PUMPKIN, CropType.PUMPKIN.baseYield())
                        .and(Goals.codeContains("\\bwhile\\s*\\(", "Дождись созревания с помощью цикла while")));
    }

    // ------------------------------------------------------------------ 8

    private static Lesson methods() {
        return new Lesson("methods", "Свои команды", "Методы",
                """
                Если один и тот же код нужен несколько раз, его выносят в метод — свою собственную команду:
                  private void plantRow(RobotApi robot, int y, CropType type) {
                      for (int x = 0; x < 8; x++) {
                          robot.moveTo(x, y);
                          robot.plant(Crop.of(type));
                          robot.water();
                      }
                  }

                Параметры (robot, y, type) — входные данные метода. Теперь засадить ряд — одна строка:
                  plantRow(robot, 1, CropType.CORN);

                void значит, что метод ничего не возвращает. Метод объявляется внутри класса, но снаружи run.
                """,
                "Напиши свой метод plantRow и с его помощью засади ряды y = 1 и y = 4 (любой культурой, с поливом).",
                "Объяви метод после закрывающей скобки run(...) { }, но до последней } класса. В run вызови его дважды.",
                IMPORTS + """
                        public class MyFarm implements FarmProgram {

                            @Override
                            public void run(RobotApi robot, FarmApi farm) {
                                // вызови свой метод для рядов 1 и 4
                            }

                            // объяви здесь метод plantRow(...)
                        }
                        """,
                IMPORTS + """
                        public class MyFarm implements FarmProgram {

                            @Override
                            public void run(RobotApi robot, FarmApi farm) {
                                plantRow(robot, farm, 1, CropType.CORN);
                                plantRow(robot, farm, 4, CropType.CARROT);
                            }

                            private void plantRow(RobotApi robot, FarmApi farm, int y, CropType type) {
                                for (int x = 0; x < farm.getWidth(); x++) {
                                    robot.moveTo(x, y);
                                    robot.plant(Crop.of(type));
                                    robot.water();
                                }
                            }
                        }
                        """,
                EMPTY_FARM,
                Goals.rowPlanted(1, null)
                        .and(Goals.rowPlanted(4, null))
                        .and(Goals.codeContains("\\bvoid\\s+(?!run\\b)\\w+\\s*\\(",
                                "Объяви собственный метод, например private void plantRow(...)")));
    }

    // ------------------------------------------------------------------ 9

    private static Lesson nestedLoops() {
        return new Lesson("nested-loops", "Всё поле", "Вложенные циклы",
                """
                Цикл можно положить внутрь другого цикла. Внешний перебирает ряды, внутренний — грядки в ряду:
                  for (int y = 0; y < farm.getHeight(); y++) {
                      for (int x = 0; x < farm.getWidth(); x++) {
                          // клетка (x, y)
                      }
                  }

                Внутренний цикл полностью проходит для каждого шага внешнего: 6 рядов × 8 грядок = 48 повторений.

                Совет: чередуй культуры, например по остатку от деления (x + y) % 4 — он даёт 0, 1, 2, 3.
                """,
                "Засади и полей ВСЁ поле, используя два вложенных цикла for.",
                "CropType[] types = CropType.values(); — массив всех культур. Внутри: types[(x + y) % types.length].",
                program("for (int y = 0; y < farm.getHeight(); y++) {\n    // внутренний цикл по x\n}\n"),
                program("""
                        CropType[] types = CropType.values();
                        for (int y = 0; y < farm.getHeight(); y++) {
                            for (int x = 0; x < farm.getWidth(); x++) {
                                robot.moveTo(x, y);
                                robot.plant(Crop.of(types[(x + y) % types.length]));
                                robot.water();
                            }
                        }
                        """),
                EMPTY_FARM,
                Goals.wholeFieldPlanted()
                        .and(Goals.codeContainsAtLeast("\\bfor\\s*\\(", 2, "Используй два цикла for — один внутри другого")));
    }

    // ------------------------------------------------------------------ 10

    private static Lesson classes() {
        return new Lesson("classes", "Свой садовник", "Классы и инкапсуляция",
                """
                Класс — это чертёж объекта: какие у него данные (поля) и что он умеет (методы). Ты уже пользуешься классом MyFarm!

                Создадим своего помощника. Поле private видно только внутри класса — это инкапсуляция: снаружи никто не сломает внутреннее устройство.
                  static class Gardener {
                      private final RobotApi robot;

                      Gardener(RobotApi robot) {      // конструктор
                          this.robot = robot;
                      }

                      void plantAt(int x, int y) {
                          robot.moveTo(x, y);
                          robot.plant(Crop.of(CropType.CORN));
                          robot.water();
                      }
                  }

                Объект создаётся оператором new:
                  Gardener g = new Gardener(robot);
                  g.plantAt(0, 0);
                """,
                "Создай класс Gardener с private-полем и засади с его помощью диагональ (0,0), (1,1) … (5,5).",
                "Объяви static class Gardener внутри MyFarm (рядом с run). В run: new Gardener(robot) и цикл for (int i = 0; i < farm.getHeight(); i++) g.plantAt(i, i);",
                IMPORTS + """
                        public class MyFarm implements FarmProgram {

                            @Override
                            public void run(RobotApi robot, FarmApi farm) {
                                // создай садовника и засади диагональ
                            }

                            // объяви здесь класс Gardener
                        }
                        """,
                IMPORTS + """
                        public class MyFarm implements FarmProgram {

                            @Override
                            public void run(RobotApi robot, FarmApi farm) {
                                Gardener gardener = new Gardener(robot);
                                for (int i = 0; i < farm.getHeight(); i++) {
                                    gardener.plantAt(i, i);
                                }
                                robot.say("Посажено растений: " + gardener.planted());
                            }

                            static class Gardener {
                                private final RobotApi robot;
                                private int planted;

                                Gardener(RobotApi robot) {
                                    this.robot = robot;
                                }

                                void plantAt(int x, int y) {
                                    robot.moveTo(x, y);
                                    robot.plant(Crop.of(CropType.CORN));
                                    robot.water();
                                    planted++;
                                }

                                int planted() {
                                    return planted;
                                }
                            }
                        }
                        """,
                EMPTY_FARM,
                Goals.diagonalPlanted()
                        .and(Goals.codeContainsAtLeast("\\bclass\\s+\\w+", 2, "Объяви свой класс, например static class Gardener { ... }"))
                        .and(Goals.codeContains("\\bprivate\\s+(final\\s+)?\\w+(<[^>]*>)?\\s+\\w+\\s*[;=]",
                                "Сделай хотя бы одно поле класса private — это и есть инкапсуляция"))
                        .and(Goals.codeContains("\\bnew\\s+\\w+\\s*\\(", "Создай объект своего класса через new")));
    }

    // ------------------------------------------------------------------ свободный режим

    private static Lesson freeMode() {
        return new Lesson("free-mode", "Свободная ферма", "Всё вместе",
                """
                Курс пройден — поздравляем! Теперь ферма в твоём распоряжении.

                Методы робота: moveTo(x, y), getX(), getY(), plant(crop), water(), harvest(), pause(секунды), say(текст).
                Методы фермы: getWidth(), getHeight(), isEmpty(x, y), isRipe(x, y), getHarvested(тип).

                Идеи: собери 100 единиц урожая, засади поле шахматным узором, найди самый короткий маршрут по полю.
                """,
                "Экспериментируй! Проверки нет — запускай что хочешь.",
                "Попробуй объединить всё: класс-помощник, методы, циклы и while для ожидания урожая.",
                program("""
                        robot.say("Свободный режим!");
                        CropType[] types = CropType.values();
                        for (int x = 0; x < farm.getWidth(); x++) {
                            robot.moveTo(x, 3);
                            robot.plant(Crop.of(types[x % types.length]));
                            robot.water();
                        }
                        """),
                program("robot.say(\"Свободный режим\");\n"),
                EMPTY_FARM,
                Goals.none());
    }
}
