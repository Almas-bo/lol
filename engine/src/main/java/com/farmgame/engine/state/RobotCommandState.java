package com.farmgame.engine.state;

import com.farmgame.core.FarmRuleException;
import com.farmgame.core.GridPosition;
import com.farmgame.core.farm.Farm;
import com.farmgame.engine.scene.FarmCoordinates;
import com.farmgame.sandbox.command.QueuedCommandSink;
import com.farmgame.sandbox.command.QueuedCommandSink.PendingCommand;
import com.farmgame.sandbox.command.RobotCommand;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

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

    /** Скорость движения робота, мировых единиц в секунду. */
    private static final float MOVE_SPEED = 4f;
    /** Длительность анимации посадки/полива/сбора. */
    private static final float ACTION_SECONDS = 0.4f;

    private final Farm farm;
    private final QueuedCommandSink sink;
    private final Spatial robotModel;
    private final FarmCoordinates coords;

    private PendingCommand current;
    private int currentResult;
    private final Deque<Vector3f> waypoints = new ArrayDeque<>();
    private float timer;

    public RobotCommandState(Farm farm, QueuedCommandSink sink, Spatial robotModel, FarmCoordinates coords) {
        this.farm = farm;
        this.sink = sink;
        this.robotModel = robotModel;
        this.coords = coords;
    }

    @Override
    public void update(float tpf) {
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
            robotModel.getLocalTranslation().y = 0f;
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
        try {
            currentResult = next.command().applyTo(farm);
        } catch (FarmRuleException e) {
            next.fail(e);
            return false;
        }
        current = next;
        timer = 0f;
        if (next.command() instanceof RobotCommand.MoveTo move) {
            planPath(from, move.target());
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
        Vector3f position = robotModel.getLocalTranslation().clone();
        float budget = MOVE_SPEED * tpf;
        while (budget > 0 && !waypoints.isEmpty()) {
            Vector3f target = waypoints.peek();
            Vector3f delta = target.subtract(position);
            float distance = delta.length();
            if (distance <= budget) {
                position.set(target);
                budget -= distance;
                waypoints.poll();
            } else {
                position.addLocal(delta.normalizeLocal().multLocal(budget));
                faceDirection(delta);
                budget = 0;
            }
        }
        robotModel.setLocalTranslation(position);
        return waypoints.isEmpty();
    }

    /** Небольшой «подскок» робота во время действия над грядкой. */
    private boolean animateAction(float tpf) {
        timer += tpf;
        float t = Math.min(timer / ACTION_SECONDS, 1f);
        robotModel.getLocalTranslation().y = 0.25f * FastMath.sin(t * FastMath.PI);
        robotModel.setLocalTranslation(robotModel.getLocalTranslation());
        return timer >= ACTION_SECONDS;
    }

    private void faceDirection(Vector3f direction) {
        if (direction.lengthSquared() > 1e-6f) {
            float angle = FastMath.atan2(direction.x, direction.z);
            robotModel.setLocalRotation(new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y));
        }
    }

    @Override
    protected void initialize(Application app) {
    }

    @Override
    protected void cleanup(Application app) {
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
