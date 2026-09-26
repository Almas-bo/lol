/**
 * 3D-клиент игры на jMonkeyEngine.
 *
 * <ul>
 *   <li>{@link com.farmgame.engine.scene} — построение сцены из примитивов (земля, грядки, робот, свет);</li>
 *   <li>{@link com.farmgame.engine.state} — AppState'ы, связывающие логику фермы с графикой каждый кадр;</li>
 *   <li>{@link com.farmgame.engine.ui} — экранный интерфейс и игровая консоль;</li>
 *   <li>{@link com.farmgame.engine.program} — загрузка и запуск программ игрока.</li>
 * </ul>
 *
 * <p>Движок только <i>отображает</i> состояние {@link com.farmgame.core.farm.Farm};
 * игровые правила живут в модуле core.
 */
package com.farmgame.engine;
