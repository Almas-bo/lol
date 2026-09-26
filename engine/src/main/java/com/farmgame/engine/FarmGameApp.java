package com.farmgame.engine;

import com.farmgame.core.farm.Farm;
import com.farmgame.engine.program.ProgramLauncher;
import com.farmgame.engine.scene.FarmCoordinates;
import com.farmgame.engine.scene.FarmSceneFactory;
import com.farmgame.engine.scene.RobotModelFactory;
import com.farmgame.engine.scene.SceneLighting;
import com.farmgame.engine.state.FarmRenderState;
import com.farmgame.engine.state.HudState;
import com.farmgame.engine.state.RobotCommandState;
import com.farmgame.engine.ui.GameConsole;
import com.farmgame.sandbox.command.QueuedCommandSink;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/**
 * Главное приложение jMonkeyEngine.
 *
 * <p>Собирает вместе три слоя:
 * <ol>
 *   <li>логику ({@link Farm} из модуля core);</li>
 *   <li>песочницу (очередь команд {@link QueuedCommandSink}, в которую пишет код игрока);</li>
 *   <li>графику (сцена + AppState'ы, которые каждый кадр синхронизируют её с логикой).</li>
 * </ol>
 */
public class FarmGameApp extends SimpleApplication {

    /** Размер фермы в клетках. */
    public static final int FARM_WIDTH = 8;
    public static final int FARM_HEIGHT = 6;

    private final Farm farm = new Farm(FARM_WIDTH, FARM_HEIGHT);
    private final QueuedCommandSink commandSink = new QueuedCommandSink();
    private final GameConsole console = new GameConsole(8);

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false); // отладочная статистика jME; FPS оставляем
        viewPort.setBackgroundColor(new ColorRGBA(0.53f, 0.78f, 0.95f, 1f)); // небо

        FarmCoordinates coords = new FarmCoordinates(farm.width(), farm.height());

        // Статическая часть сцены: земля и сетка.
        FarmSceneFactory sceneFactory = new FarmSceneFactory(assetManager, coords);
        rootNode.attachChild(sceneFactory.createGround());
        rootNode.attachChild(sceneFactory.createGrid());

        // Робот-трактор из примитивов.
        Node robot = new RobotModelFactory(assetManager).create();
        robot.setLocalTranslation(coords.toWorld(farm.robotPosition()));
        rootNode.attachChild(robot);

        SceneLighting.apply(rootNode);
        setupCamera(coords);

        // Динамика: грядки, выполнение команд робота, интерфейс.
        stateManager.attachAll(
                new FarmRenderState(farm, sceneFactory, rootNode),
                new RobotCommandState(farm, commandSink, robot, coords),
                new HudState(farm, console));

        new ProgramLauncher(farm, commandSink, console).launchDemo();
    }

    private void setupCamera(FarmCoordinates coords) {
        Vector3f center = coords.center();
        cam.setLocation(center.add(0, 14, 16));
        cam.lookAt(center, Vector3f.UNIT_Y);

        // Свободная камера: WASD + зажатая левая кнопка мыши для поворота.
        flyCam.setMoveSpeed(12f);
        flyCam.setDragToRotate(true);
    }

    public Farm farm() {
        return farm;
    }
}
