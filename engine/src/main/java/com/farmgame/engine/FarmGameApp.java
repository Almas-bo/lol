package com.farmgame.engine;

import com.farmgame.core.farm.Farm;
import com.farmgame.engine.program.ProgramSession;
import com.farmgame.engine.scene.EnvironmentFactory;
import com.farmgame.engine.scene.FarmCoordinates;
import com.farmgame.engine.scene.FarmSceneFactory;
import com.farmgame.engine.scene.Materials;
import com.farmgame.engine.scene.ModelLibrary;
import com.farmgame.engine.scene.PostEffects;
import com.farmgame.engine.scene.RobotModel;
import com.farmgame.engine.scene.SceneLighting;
import com.farmgame.engine.scene.SkyDome;
import com.farmgame.engine.state.AmbientLifeState;
import com.farmgame.engine.state.CodeEditorState;
import com.farmgame.engine.state.EffectsState;
import com.farmgame.engine.state.FarmRenderState;
import com.farmgame.engine.state.HudState;
import com.farmgame.engine.state.LessonState;
import com.farmgame.engine.state.OrbitCameraState;
import com.farmgame.engine.state.RobotCommandState;
import com.farmgame.engine.ui.GameConsole;
import com.farmgame.engine.ui.UiLayout;
import com.farmgame.sandbox.lesson.LessonCatalog;
import com.farmgame.sandbox.lesson.LessonProgress;
import com.farmgame.sandbox.command.QueuedCommandSink;
import com.jme3.app.DebugKeysAppState;
import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Spatial;

import java.nio.file.Path;

/**
 * Главное приложение jMonkeyEngine.
 *
 * <p>Собирает вместе четыре слоя:
 * <ol>
 *   <li>логику ({@link Farm} из модуля core);</li>
 *   <li>песочницу (очередь команд {@link QueuedCommandSink}, в которую пишет код игрока);</li>
 *   <li>графику (сцена + AppState'ы, которые каждый кадр синхронизируют её с логикой);</li>
 *   <li>обучение: редактор кода ({@link CodeEditorState}) и уроки ({@link LessonState}).</li>
 * </ol>
 */
public class FarmGameApp extends SimpleApplication {

    /** Размер фермы в клетках. */
    public static final int FARM_WIDTH = 8;
    public static final int FARM_HEIGHT = 6;

    private static final String TOGGLE_GRID = "Toggle_Grid";
    private static final String TOGGLE_FPS = "Toggle_Fps";
    private boolean fpsVisible;

    private final GraphicsQuality quality;
    private final Farm farm = new Farm(FARM_WIDTH, FARM_HEIGHT);
    private final QueuedCommandSink commandSink = new QueuedCommandSink();
    private final GameConsole console = new GameConsole(40);

    public FarmGameApp() {
        this(GraphicsQuality.HIGH);
    }

    public FarmGameApp(GraphicsQuality quality) {
        // Свою камеру даёт OrbitCameraState, поэтому стандартная FlyCam не подключается.
        super(new StatsAppState(), new DebugKeysAppState());
        this.quality = quality;
    }

    @Override
    public void simpleInitApp() {
        setDisplayStatView(false); // отладочная статистика jME
        setDisplayFps(false);      // счётчик кадров — по F3
        Materials materials = new Materials(assetManager);
        FarmCoordinates coords = new FarmCoordinates(farm.width(), farm.height());

        // Статическая часть сцены: небо, окружение, сетка, подписи координат.
        rootNode.attachChild(SkyDome.create(materials));
        rootNode.attachChild(new EnvironmentFactory(materials, new ModelLibrary(assetManager), coords).create());
        FarmSceneFactory sceneFactory = new FarmSceneFactory(assetManager, materials, coords);
        Spatial grid = sceneFactory.createGrid();
        grid.setCullHint(Spatial.CullHint.Always); // по умолчанию выключена, клавиша G
        rootNode.attachChild(grid);
        rootNode.attachChild(sceneFactory.createAxisLabels());

        // Робот-трактор из примитивов.
        RobotModel robot = new RobotModel(materials);
        robot.node().setLocalTranslation(coords.toWorld(farm.robotPosition()));
        rootNode.attachChild(robot.node());

        // Свет, тени и пост-эффекты. Небо тени не отбрасывает (ShadowMode задан в SkyDome).
        DirectionalLight sun = SceneLighting.apply(rootNode);
        rootNode.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        PostEffects.apply(assetManager, viewPort, sun, quality);

        // Динамика: грядки, выполнение команд робота, эффекты, камера.
        UiLayout layout = UiLayout.forScreen(cam.getWidth(), cam.getHeight());
        OrbitCameraState camera = new OrbitCameraState(coords.center(), robot.node());
        camera.setViewCenter(layout.viewCenterNdc());
        stateManager.attachAll(
                new FarmRenderState(farm, sceneFactory, rootNode),
                new RobotCommandState(farm, commandSink, robot, coords),
                new EffectsState(),
                new AmbientLifeState(coords, robot.node()),
                camera);

        // Обучение: редактор кода, уроки, консоль и панель фермы.
        stateManager.attach(new CodeEditorState(layout.editor()));
        stateManager.attach(new LessonState(LessonCatalog.lessons(), new LessonProgress(progressFile()),
                farm, new ProgramSession(farm, commandSink), console, layout.lesson()));
        stateManager.attach(new HudState(farm, console, layout));

        inputManager.addMapping(TOGGLE_GRID, new KeyTrigger(KeyInput.KEY_G));
        inputManager.addListener((ActionListener) (name, pressed, tpf) -> {
            if (pressed) {
                grid.setCullHint(grid.getCullHint() == Spatial.CullHint.Always
                        ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
            }
        }, TOGGLE_GRID);
        inputManager.addMapping(TOGGLE_FPS, new KeyTrigger(KeyInput.KEY_F3));
        inputManager.addListener((ActionListener) (name, pressed, tpf) -> {
            if (pressed) {
                fpsVisible = !fpsVisible;
                setDisplayFps(fpsVisible);
            }
        }, TOGGLE_FPS);

    }

    /** Файл прогресса; можно переопределить свойством {@code -Dfarmgame.progress=путь}. */
    private static Path progressFile() {
        String custom = System.getProperty("farmgame.progress");
        return custom != null ? Path.of(custom) : LessonProgress.defaultFile();
    }

    public Farm farm() {
        return farm;
    }
}
