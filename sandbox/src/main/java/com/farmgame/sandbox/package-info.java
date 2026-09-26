/**
 * Песочница — всё, что связано с кодом игрока.
 *
 * <ul>
 *   <li>{@link com.farmgame.sandbox.api} — публичный API, который видит игрок
 *       ({@code robot.moveTo(x, y)}, {@code robot.plant(crop)} ...);</li>
 *   <li>{@link com.farmgame.sandbox.command} — команды робота и способы их доставки в игровой мир;</li>
 *   <li>{@link com.farmgame.sandbox.compiler} — компиляция исходников игрока в памяти;</li>
 *   <li>{@link com.farmgame.sandbox.runtime} — реализация API и запуск программ с таймаутом.</li>
 * </ul>
 *
 * <p>Игрок программирует только против интерфейсов из пакета {@code api} — это учебный
 * пример принципа «программируй на уровне интерфейсов, а не реализаций».
 */
package com.farmgame.sandbox;
