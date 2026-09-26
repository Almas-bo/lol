# Как запустить Java Farm — подробная инструкция

Это пошаговое руководство для тех, кто запускает проект впервые. Если вы уже работали
с Java и Gradle, достаточно раздела [Быстрый старт](#быстрый-старт).

- [Быстрый старт](#быстрый-старт)
- [Шаг 1. Требования к компьютеру](#шаг-1-требования-к-компьютеру)
- [Шаг 2. Установка JDK 21](#шаг-2-установка-jdk-21)
- [Шаг 3. Установка Git и получение проекта](#шаг-3-установка-git-и-получение-проекта)
- [Шаг 4. Сборка и запуск из терминала](#шаг-4-сборка-и-запуск-из-терминала)
- [Шаг 5. Запуск из IntelliJ IDEA](#шаг-5-запуск-из-intellij-idea)
- [Параметры запуска](#параметры-запуска)
- [Управление в игре](#управление-в-игре)
- [Готовая сборка без Gradle](#готовая-сборка-без-gradle)
- [Как изменить программу робота](#как-изменить-программу-робота)
- [Решение проблем](#решение-проблем)

---

## Быстрый старт

```bash
git clone https://github.com/Almas-bo/lol.git
cd lol
git checkout claude/farm-game-init-6barre   # пока проект живёт в этой ветке
./gradlew :engine:run
```

На Windows вместо `./gradlew` пишите `gradlew.bat` (в PowerShell — `.\gradlew.bat`).

---

## Шаг 1. Требования к компьютеру

| Что | Минимум | Комментарий |
|-----|---------|-------------|
| ОС | Windows 10/11, macOS 12+, Linux (x64 или ARM64) | |
| Java | **JDK 21 или новее** | Именно JDK, не JRE — игра компилирует код игрока |
| Видеокарта | с поддержкой **OpenGL 3.2+** | Любая видеокарта/встроенная графика последних ~10 лет |
| Память | 4 ГБ ОЗУ | Сама игра использует ~300–500 МБ |
| Интернет | только для **первого** запуска | Gradle скачает себя и библиотеки (~100 МБ) |

Устанавливать Gradle отдельно **не нужно**: в проекте есть Gradle Wrapper (`gradlew`),
который сам скачает нужную версию.

---

## Шаг 2. Установка JDK 21

Сначала проверьте, может быть JDK уже установлен. Откройте терминал
(Windows: `Win+R` → `cmd`; macOS: «Терминал»; Linux: `Ctrl+Alt+T`) и выполните:

```bash
java -version
javac -version
```

Если **обе** команды выводят версию 21 или выше (например, `openjdk version "21.0.4"`
и `javac 21.0.4`) — переходите к шагу 3. Если `javac` не найден — у вас JRE, нужен JDK.

### Windows

1. Откройте <https://adoptium.net/temurin/releases/?version=21> и скачайте
   **JDK 21, Windows, x64, `.msi`**.
2. Запустите установщик. На экране выбора компонентов включите
   **«Set JAVA_HOME variable»** и **«Add to PATH»** (кликните по значку и выберите
   «Will be installed on local hard drive»).
3. **Закройте и заново откройте** терминал, проверьте `java -version` и `javac -version`.

Альтернатива через winget: `winget install EclipseAdoptium.Temurin.21.JDK`.

### macOS

С Homebrew:

```bash
brew install --cask temurin@21
```

Или скачайте `.pkg` для вашей архитектуры с <https://adoptium.net/temurin/releases/?version=21>
(**aarch64** — для Mac на M1/M2/M3/M4, **x64** — для Mac на Intel).

### Linux

```bash
# Ubuntu / Debian / Mint
sudo apt update && sudo apt install openjdk-21-jdk

# Fedora
sudo dnf install java-21-openjdk-devel

# Arch / Manjaro
sudo pacman -S jdk21-openjdk
```

Если в системе несколько версий Java, выберите 21-ю:
`sudo update-alternatives --config java` (Ubuntu/Debian) или `archlinux-java set java-21-openjdk` (Arch).

### Проверка JAVA_HOME (если Gradle пишет, что не нашёл Java)

```bash
# macOS / Linux
echo $JAVA_HOME
# Windows (cmd)
echo %JAVA_HOME%
```

Путь должен вести в папку JDK 21, например `C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot`
или `/usr/lib/jvm/java-21-openjdk-amd64`.

---

## Шаг 3. Установка Git и получение проекта

Git: Windows — <https://git-scm.com/download/win>; macOS — `xcode-select --install` или
`brew install git`; Linux — `sudo apt install git`.

```bash
git clone https://github.com/Almas-bo/lol.git
cd lol
git checkout claude/farm-game-init-6barre
```

Без Git: на странице репозитория на GitHub выберите ветку `claude/farm-game-init-6barre`,
нажмите **Code → Download ZIP**, распакуйте и откройте папку в терминале.

---

## Шаг 4. Сборка и запуск из терминала

Все команды выполняются **в корне проекта** (там, где лежит файл `gradlew`).

### 4.1. Сборка и тесты

```bash
./gradlew build            # macOS / Linux
gradlew.bat build          # Windows cmd
.\gradlew.bat build        # Windows PowerShell
```

Первый запуск занимает 1–3 минуты: скачиваются Gradle и библиотеки jMonkeyEngine.
В конце должно быть `BUILD SUCCESSFUL`.

### 4.2. Запуск игры

```bash
./gradlew :engine:run
```

Через несколько секунд откроется окно 1280×720:

- ферма 8×6 грядок с забором, амбаром, прудом и деревьями;
- робот-трактор, который выполняет **демо-программу игрока**: сажает ряд культур,
  поливает, ждёт урожай и собирает его три сезона подряд;
- слева сверху — панель фермы (позиция робота, склад), слева снизу — консоль
  программы, справа — подсказка по управлению.

Закрыть игру — `Esc` или крестик окна. В терминале после этого Gradle напишет `BUILD SUCCESSFUL`.

---

## Шаг 5. Запуск из IntelliJ IDEA

1. Установите IntelliJ IDEA (бесплатной Community Edition достаточно).
2. **File → Open…** → выберите папку проекта (где лежит `settings.gradle.kts`) → **Open**.
   На вопрос «Trust project?» ответьте **Trust Project**.
3. Дождитесь окончания импорта Gradle (прогресс внизу справа).
4. **File → Project Structure → Project → SDK** — выберите JDK 21
   (или **Add SDK → Download JDK… → версия 21, Eclipse Temurin**).
5. Откройте `engine/src/main/java/com/farmgame/engine/Main.java` и нажмите зелёный ▶
   рядом с `public static void main`.

Чтобы передать параметры (например, качество графики): **Run → Edit Configurations… → Main →
Program arguments** → `--quality=medium`.

Альтернатива: панель **Gradle** (справа) → `farm-game → engine → Tasks → application → run`.

**macOS:** при запуске через ▶ добавьте в **VM options** строку `-XstartOnFirstThread`
(при запуске через Gradle это делается автоматически).

---

## Параметры запуска

Параметры передаются через `--args`:

```bash
./gradlew :engine:run --args="--quality=low"
./gradlew :engine:run --args="--quality=medium --width=1600 --height=900"
./gradlew :engine:run --args="--fullscreen"
```

| Параметр | Значения | По умолчанию | Что делает |
|----------|----------|--------------|------------|
| `--quality=` | `low`, `medium`, `high` | `high` | Качество графики (см. ниже) |
| `--width=` / `--height=` | число пикселей | `1280` / `720` | Размер окна |
| `--fullscreen` | — | выкл. | Полноэкранный режим |

| Качество | Тени | Сглаживание | SSAO (мягкое затенение) | Свечение (bloom) | Для чего |
|----------|------|-------------|-------------------------|------------------|----------|
| `low` | нет | нет | нет | нет | Слабые ноутбуки, виртуальные машины |
| `medium` | 1024 px | FXAA + MSAA×2 | нет | нет | Встроенная графика Intel/AMD |
| `high` | 2048 px, мягкие | FXAA + MSAA×4 | да | да | Дискретная видеокарта |

Если FPS (счётчик в левом нижнем углу) ниже 30 — понизьте качество.

---

## Управление в игре

| Клавиша / мышь | Действие |
|----------------|----------|
| Зажать левую или правую кнопку мыши + двигать | Вращать камеру вокруг фермы |
| Колесо мыши, `+` / `-` | Приблизить / отдалить |
| `W` `A` `S` `D` или стрелки | Сдвинуть точку обзора |
| `Q` / `E` | Повернуть камеру |
| `F` | Камера следует за роботом (вкл/выкл) |
| `R` | Вернуть камеру в исходное положение |
| `G` | Показать/скрыть сетку клеток |
| `H` | Показать/скрыть интерфейс |
| `Esc` | Выйти из игры |

Подписи `x0…x7` и `y0…y5` по краям поля — это координаты клеток,
которые используются в `robot.moveTo(x, y)`.

---

## Готовая сборка без Gradle

Можно собрать папку с игрой и скриптом запуска — удобно, чтобы передать другу
(на его компьютере всё равно нужен JDK 21):

```bash
./gradlew :engine:installDist
```

Результат: `engine/build/install/engine/`. Запуск:

```bash
engine/build/install/engine/bin/engine                    # macOS / Linux
engine\build\install\engine\bin\engine.bat                # Windows
engine/build/install/engine/bin/engine --quality=low      # с параметрами
```

Или ZIP-архив для распространения: `./gradlew :engine:distZip` →
`engine/build/distributions/engine-0.1.0-SNAPSHOT.zip`.

---

## Как изменить программу робота

Пока внутриигрового редактора нет, программа робота берётся из файла
`engine/src/main/resources/programs/DemoFarm.java`. Отредактируйте его и снова
выполните `./gradlew :engine:run` — игра скомпилирует ваш код при старте.

Попробуйте, например, засадить всё поле тыквами:

```java
for (int y = 0; y < farm.getHeight(); y++) {
    for (int x = 0; x < farm.getWidth(); x++) {
        robot.moveTo(x, y);
        robot.plant(Crop.builder().setType(CropType.PUMPKIN).build());
        robot.water();
    }
}
```

Если в коде ошибка, она появится в панели «Консоль программы» с номером строки,
например `Строка 12: cannot find symbol`. Доступные методы описаны в
`sandbox/src/main/java/com/farmgame/sandbox/api/` (`RobotApi`, `FarmApi`).

Программе игрока разрешены только `java.lang`, `java.util`, API робота и культуры —
поэтому вместо `System.out.println` используйте `robot.say("...")`, а вместо
`Thread.sleep` — `robot.pause(секунды)`.

---

## Решение проблем

**`JAVA_HOME is not set and no 'java' command could be found`**
JDK не установлен или терминал открыт до установки. Переоткройте терминал; проверьте шаг 2.

**`Unsupported class file major version 65` / `release version 21 not supported`**
Gradle запущен на старой Java. Проверьте `java -version` и `JAVA_HOME` — должна быть 21+.

**`Компилятор Java недоступен: запустите игру на JDK, а не на JRE`**
Игра запущена на JRE. Установите JDK (шаг 2) и укажите его в `JAVA_HOME`
(или в IntelliJ: Project Structure → SDK).

**`./gradlew: Permission denied` (macOS/Linux)**
```bash
chmod +x gradlew
```

**`'.\gradlew' is not recognized` (Windows)**
Вы не в корне проекта. Выполните `cd` в папку, где лежит `gradlew.bat`.

**Сборка «зависла» на `Downloading…` или ошибки `Could not resolve…` / `429 Too Many Requests`**
Нужен интернет при первом запуске. Повторите команду через минуту. За корпоративным
прокси создайте файл `~/.gradle/gradle.properties`:
```properties
systemProp.https.proxyHost=proxy.example.com
systemProp.https.proxyPort=8080
```

**Окно не открывается: `GLFW error`, `No OpenGL context`, `OpenGL 3.2 not supported`**
- обновите драйвер видеокарты (сайт NVIDIA / AMD / Intel);
- на ноутбуке с двумя видеокартами запустите Java на дискретной
  (Windows: Параметры → Дисплей → Графика → добавить `java.exe` → «Высокая производительность»);
- в виртуальной машине включите 3D-ускорение или используйте `--quality=low`;
- Linux + Wayland: попробуйте `GDK_BACKEND=x11 ./gradlew :engine:run`
  или сессию «Xorg» на экране входа.

**macOS: окно не появляется или ошибка про `XstartOnFirstThread`**
Запускайте через `./gradlew :engine:run` — флаг добавляется автоматически.
В IntelliJ добавьте `-XstartOnFirstThread` в VM options (шаг 5).

**Низкий FPS / вентилятор шумит**
`./gradlew :engine:run --args="--quality=low"`.

**Кириллица в консоли терминала выглядит как `????`**
На игру не влияет (в окне игры текст отображается корректно). Windows:
выполните `chcp 65001` перед запуском.

**Хочу начать с чистого листа**
```bash
./gradlew clean build
```
