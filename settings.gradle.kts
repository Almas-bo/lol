rootProject.name = "farm-game"

/*
 * Модули проекта:
 *  - core    — чистая логика фермы (без графики), легко тестируется.
 *  - sandbox — API для игрока, компиляция и выполнение его Java-кода.
 *  - engine  — 3D-графика на jMonkeyEngine и точка входа (Main).
 *
 * Зависимости направлены строго "вниз": engine -> sandbox -> core.
 */
include("core", "sandbox", "engine")
