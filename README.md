# Java Farm

Обучающая 3D-игра про ферму: игрок управляет роботом-трактором, **программируя его на Java**.
По ходу игры осваиваются ООП, инкапсуляция, интерфейсы/API, паттерны Builder, Command, Facade и т.д.

![Скриншот](docs/screenshot.png)

## Быстрый старт

Требуется **JDK 21+** (именно JDK, не JRE: игра компилирует код игрока через `javax.tools`).
Gradle ставить не нужно — используется wrapper.

```bash
./gradlew build          # сборка + тесты
./gradlew :engine:run    # запуск 3D-окна
```

На Windows: `gradlew.bat build` и `gradlew.bat :engine:run`.

После запуска откроется окно 1280×720: поле 8×6 грядок, робот-трактор и HUD.
Сразу стартует демо-программа игрока (`engine/src/main/resources/programs/DemoFarm.java`):
робот сажает ряд культур, поливает, ждёт урожая и собирает его три сезона подряд.

**Управление камерой:** `W A S D` — движение, `Q`/`Z` — вверх/вниз,
зажатая левая кнопка мыши — поворот, `Esc` — выход.

## Архитектура

```
engine  ──►  sandbox  ──►  core
(jME3)       (API игрока,   (логика фермы,
              компилятор)    без графики)
```

| Модуль    | Пакет                  | Назначение |
|-----------|------------------------|------------|
| `core`    | `com.farmgame.core`    | Ферма, грядки, культуры (`Crop` + Builder), робот, склад. Чистая Java, покрыта тестами. |
| `sandbox` | `com.farmgame.sandbox` | Публичный API игрока (`api`), команды робота (`command`), компиляция в памяти (`compiler`), запуск с таймаутом (`runtime`). |
| `engine`  | `com.farmgame.engine`  | `Main`, 3D-сцена из примитивов (`scene`), синхронизация логики и графики (`state`), HUD (`ui`), запуск программ (`program`). |

### Как код игрока попадает в игру

1. Исходник компилируется в памяти — `PlayerCodeCompiler` → `CompilationResult` (`Success` / `Failure` с номерами строк).
2. Классы загружаются через `SandboxClassLoader` — доступны только `java.lang`, `java.util`,
   `com.farmgame.sandbox.api` и `com.farmgame.core.crop` (без рефлексии, потоков, `System`, `java.io`).
3. `ProgramRunner` запускает программу в отдельном потоке с лимитом времени.
4. Вызовы `robot.moveTo(x, y)` превращаются в команды (`RobotCommand`) и попадают в `QueuedCommandSink`.
   Игровой цикл (`RobotCommandState`) применяет их к логике фермы, проигрывает анимацию —
   и только тогда метод игрока возвращает управление.

### Пример программы игрока

```java
import com.farmgame.core.crop.*;
import com.farmgame.sandbox.api.*;

public class MyFarm implements FarmProgram {
    @Override
    public void run(RobotApi robot, FarmApi farm) {
        robot.moveTo(2, 3);
        robot.plant(Crop.builder()
                .setType(CropType.CORN)
                .setFertilized(true)
                .build());
        robot.water();
        while (!farm.isRipe(2, 3)) {
            robot.pause(1.0);
        }
        robot.say("Собрано: " + robot.harvest());
    }
}
```

## Дальнейшие шаги

- Внутриигровой редактор кода (сейчас запускается встроенная демо-программа).
- Система уроков/заданий, открывающих новые части API (поливалки, сборщики, несколько роботов).
- Изоляция кода игрока в отдельном процессе — `SandboxClassLoader` пока учебный барьер, а не граница безопасности.
- Шрифт с кириллицей для HUD (стандартный шрифт jME содержит только латиницу).
