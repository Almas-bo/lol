package com.farmgame.engine.state;

import com.farmgame.core.FarmRuleException;
import com.farmgame.core.GridPosition;
import com.farmgame.core.farm.Farm;
import com.farmgame.core.farm.PlotState;
import com.farmgame.engine.scene.FarmCoordinates;
import com.farmgame.engine.scene.FarmSceneFactory;
import com.farmgame.engine.scene.Materials;
import com.farmgame.engine.scene.RobotModel;
import com.farmgame.sandbox.command.QueuedCommandSink;
import com.farmgame.sandbox.command.QueuedCommandSink.PendingCommand;
import com.farmgame.sandbox.command.RobotCommand;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Torus;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Выполняет команды робота из очереди песочницы и анимирует модель.
 *
 * <p>Порядок для каждой команды: применить к логике фермы (здесь же проверяются правила) →
 * проиграть анимацию → сообщить коду игрока о завершении. Ошибка правил сразу
 * возвращается игроку, анимация не проигрывается.
 */
public class RobotCommandState extends BaseAppState {

    /** Максимальная скорость движения робота, мировых единиц в секунду. */
    private static final float MOVE_SPEED = 4.5f;
    /** Скорость поворота, радиан в секунду. */
    private static final float TURN_SPEED = 7f;
    /** Длительность анимации посадки/полива/сбора. */
    private static final float ACTION_SECONDS = 0.55f;

    private final Farm farm;
    private final QueuedCommandSink sink;
    private final RobotModel robot;
    private final FarmCoordinates coords;

    private PendingCommand current;
    private int currentResult;
    private PlotState plotBefore;
    private final Deque<Vector3f> waypoints = new ArrayDeque<>();
    private float timer;
    private boolean effectFired;
    private float heading;
    private Geometry targetMarker;

    public RobotCommandState(Farm farm, QueuedCommandSink sink, RobotModel robot, FarmCoordinates coords) {
        this.farm = farm;
        this.sink = sink;
        this.robot = robot;
        this.coords = coords;
    }

    @Override
    protected void initialize(Application app) {
        Materials materials = new Materials(app.getAssetManager());
        targetMarker = new Geometry("move-target", new Torus(24, 8, 0.06f, 0.75f));
        targetMarker.setMaterial(materials.glowing(new ColorRGBA(1f, 0.9f, 0.3f, 1f)));
        targetMarker.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X));
        targetMarker.setShadowMode(RenderQueue.ShadowMode.Off);
        targetMarker.setCullHint(Spatial.CullHint.Always);
        ((SimpleApplication) app).getRootNode().attachChild(targetMarker);
    }

    @Override
    public void update(float tpf) {
        robot.update(tpf, current != null);
        if (current == null && !startNext()) {
            return;
        }
        boolean finished = switch (current.command()) {
            case RobotCommand.MoveTo move -> animateMove(tpf);
            case RobotCommand.Pause pause -> (timer += tpf) >= pause.seconds();
            case RobotCommand.Plant p -> animateAction(tpf);
            case RobotCommand.Water w -> animateAction(tpf);
            case RobotCommand.Harvest h -> animateAction(tpf);
        };
        if (finished) {
            robot.setNod(0f);
            targetMarker.setCullHint(Spatial.CullHint.Always);
            current.complete(currentResult);
            current = null;
        }
    }

    /** Берёт следующую команду и применяет её к логике. */
    private boolean startNext() {
        PendingCommand next = sink.poll();
        if (next == null) {
            return false;
        }
        GridPosition from = farm.robotPosition();
        plotBefore = farm.plotAt(from);
        try {
            currentResult = next.command().applyTo(farm);
        } catch (FarmRuleException e) {
            next.fail(e);
            return false;
        }
        current = next;
        timer = 0f;
        effectFired = false;
        if (next.command() instanceof RobotCommand.MoveTo move) {
            planPath(from, move.target());
            targetMarker.setLocalTranslation(coords.toWorld(move.target()).add(0, FarmSceneFactory.PLOT_TOP + 0.08f, 0));
            targetMarker.setCullHint(Spatial.CullHint.Inherit);
        }
        return true;
    }

    /** Путь «буквой Г»: сначала по X, потом по Y — как ходит робот по сетке. */
    private void planPath(GridPosition from, GridPosition to) {
        waypoints.clear();
        waypoints.add(coords.toWorld(new GridPosition(to.x(), from.y())));
        waypoints.add(coords.toWorld(to));
    }

    private boolean animateMove(float tpf) {
        Spatial node = robot.node();
        Vector3f position = node.getLocalTranslation().clone();
        // Пульсация маркера цели.
        targetMarker.setLocalScale(1f + 0.12f * FastMath.sin(timer * 8f));
        timer += tpf;

        while (!waypoints.isEmpty() && waypoints.peek().distance(position) < 1e-3f) {
            waypoints.poll();
        }
        if (waypoints.isEmpty()) {
            return true;
        }
        Vector3f delta = waypoints.peek().subtract(position);
        // Сначала разворачиваемся к цели, потом едем — как настоящий трактор.
        float desired = FastMath.atan2(delta.x, delta.z);
        float diff = normalizeAngle(desired - heading);
        float maxTurn = TURN_SPEED * tpf;
        heading += FastMath.clamp(diff, -maxTurn, maxTurn);
        node.setLocalRotation(new Quaternion().fromAngleAxis(heading, Vector3f.UNIT_Y));
        if (Math.abs(diff) > 0.35f) {
            robot.rollWheels(0.02f); // колёса чуть проворачиваются на развороте
            return false;
        }

        float distance = delta.length();
        float step = Math.min(distance, MOVE_SPEED * tpf);
        position.addLocal(delta.normalizeLocal().multLocal(step));
        node.setLocalTranslation(position);
        robot.rollWheels(step);
        return false;
    }

    /** «Кивок» робота над грядкой и визуальный эффект в середине действия. */
    private boolean animateAction(float tpf) {
        timer += tpf;
        float t = Math.min(timer / ACTION_SECONDS, 1f);
        robot.setNod(0.18f * FastMath.sin(t * FastMath.PI));
        if (!effectFired && t >= 0.4f) {
            effectFired = true;
            fireEffect();
        }
        return timer >= ACTION_SECONDS;
    }

    private void fireEffect() {
        EffectsState effects = getState(EffectsState.class);
        if (effects == null) {
            return;
        }
        Vector3f spot = coords.toWorld(farm.robotPosition());
        switch (current.command()) {
            case RobotCommand.Water w -> effects.water(spot);
            case RobotCommand.Plant p -> effects.dirt(spot);
            case RobotCommand.Harvest h -> {
                if (currentResult > 0 && plotBefore.crop() != null) {
                    effects.harvest(spot, FarmSceneFactory.colorOf(plotBefore.crop().type()));
                }
            }
            default -> {
            }
        }
    }

    private static float normalizeAngle(float angle) {
        while (angle > FastMath.PI) {
            angle -= FastMath.TWO_PI;
        }
        while (angle < -FastMath.PI) {
            angle += FastMath.TWO_PI;
        }
        return angle;
    }

    @Override
    protected void cleanup(Application app) {
        targetMarker.removeFromParent();
        if (current != null) {
            current.fail(new IllegalStateException("Игра закрыта"));
        }
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}
