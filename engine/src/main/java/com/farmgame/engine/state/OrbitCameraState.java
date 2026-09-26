package com.farmgame.engine.state;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Spatial;

/**
 * Камера, вращающаяся вокруг точки фермы (как в стратегиях).
 *
 * <ul>
 *   <li>зажатая левая или правая кнопка мыши + движение — вращение;</li>
 *   <li>колесо мыши, {@code +}/{@code -} — приближение;</li>
 *   <li>{@code WASD} / стрелки — сдвиг точки обзора;</li>
 *   <li>{@code Q}/{@code E} — поворот; {@code F} — следовать за роботом; {@code R} — сброс.</li>
 * </ul>
 * Движение сглажено: камера плавно догоняет желаемое положение.
 */
public class OrbitCameraState extends BaseAppState implements ActionListener, AnalogListener {

    private static final String ROTATE = "Cam_Rotate", LEFT = "Cam_Left", RIGHT = "Cam_Right",
            UP = "Cam_Up", DOWN = "Cam_Down", ZOOM_IN = "Cam_ZoomIn", ZOOM_OUT = "Cam_ZoomOut",
            PAN_F = "Cam_Forward", PAN_B = "Cam_Back", PAN_L = "Cam_PanLeft", PAN_R = "Cam_PanRight",
            TURN_L = "Cam_TurnLeft", TURN_R = "Cam_TurnRight", FOLLOW = "Cam_Follow", RESET = "Cam_Reset";
    private static final String[] MAPPINGS = {ROTATE, LEFT, RIGHT, UP, DOWN, ZOOM_IN, ZOOM_OUT,
            PAN_F, PAN_B, PAN_L, PAN_R, TURN_L, TURN_R, FOLLOW, RESET};

    private static final float MIN_DISTANCE = 6f, MAX_DISTANCE = 60f;
    private static final float MIN_PITCH = 0.15f, MAX_PITCH = 1.45f;

    private final Vector3f homeTarget;
    private final Spatial followTarget;

    private final Vector3f target = new Vector3f();
    private final Vector3f smoothTarget = new Vector3f();
    private float yaw, pitch, distance;
    private float smoothYaw, smoothPitch, smoothDistance;
    private boolean rotating;
    private boolean following;
    private Camera cam;
    private float viewCenterNdc;

    /**
     * @param homeTarget   начальная точка обзора (центр фермы)
     * @param followTarget объект для режима слежения (робот)
     */
    public OrbitCameraState(Vector3f homeTarget, Spatial followTarget) {
        this.homeTarget = homeTarget.clone();
        this.followTarget = followTarget;
    }

    private void reset() {
        target.set(homeTarget);
        yaw = -0.3f;
        pitch = 0.8f;
        distance = 30f;
        following = false;
    }

    @Override
    protected void initialize(Application app) {
        cam = app.getCamera();
        reset();
        smoothTarget.set(target);
        smoothYaw = yaw;
        smoothPitch = pitch;
        smoothDistance = distance;
        applyFrustum();
    }

    /**
     * Смещает центр изображения по горизонтали (в NDC, -1..1), чтобы ферма оказывалась
     * в свободной от панелей части экрана. Используется «асимметричная» пирамида обзора.
     */
    public void setViewCenter(float ndcX) {
        viewCenterNdc = ndcX;
        if (cam != null) {
            applyFrustum();
        }
    }

    private void applyFrustum() {
        float near = 0.3f;
        float far = 1200f;
        float top = near * FastMath.tan(50f * FastMath.DEG_TO_RAD / 2f);
        float right = top * cam.getWidth() / cam.getHeight();
        float shift = -viewCenterNdc * right;
        cam.setFrustum(near, far, -right + shift, right + shift, top, -top);
        cam.setParallelProjection(false);
    }

    @Override
    protected void onEnable() {
        InputManager input = getApplication().getInputManager();
        input.addMapping(ROTATE, new MouseButtonTrigger(MouseInput.BUTTON_LEFT),
                new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
        input.addMapping(LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        input.addMapping(RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        input.addMapping(UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        input.addMapping(DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        input.addMapping(ZOOM_IN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false),
                new KeyTrigger(KeyInput.KEY_EQUALS), new KeyTrigger(KeyInput.KEY_ADD));
        input.addMapping(ZOOM_OUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true),
                new KeyTrigger(KeyInput.KEY_MINUS), new KeyTrigger(KeyInput.KEY_SUBTRACT));
        input.addMapping(PAN_F, new KeyTrigger(KeyInput.KEY_W), new KeyTrigger(KeyInput.KEY_UP));
        input.addMapping(PAN_B, new KeyTrigger(KeyInput.KEY_S), new KeyTrigger(KeyInput.KEY_DOWN));
        input.addMapping(PAN_L, new KeyTrigger(KeyInput.KEY_A), new KeyTrigger(KeyInput.KEY_LEFT));
        input.addMapping(PAN_R, new KeyTrigger(KeyInput.KEY_D), new KeyTrigger(KeyInput.KEY_RIGHT));
        input.addMapping(TURN_L, new KeyTrigger(KeyInput.KEY_Q));
        input.addMapping(TURN_R, new KeyTrigger(KeyInput.KEY_E));
        input.addMapping(FOLLOW, new KeyTrigger(KeyInput.KEY_F));
        input.addMapping(RESET, new KeyTrigger(KeyInput.KEY_R));
        input.addListener(this, MAPPINGS);
    }

    @Override
    protected void onDisable() {
        InputManager input = getApplication().getInputManager();
        input.removeListener(this);
        for (String mapping : MAPPINGS) {
            if (input.hasMapping(mapping)) {
                input.deleteMapping(mapping);
            }
        }
    }

    @Override
    public void onAction(String name, boolean pressed, float tpf) {
        switch (name) {
            case ROTATE -> rotating = pressed;
            case FOLLOW -> {
                if (pressed) {
                    following = !following;
                }
            }
            case RESET -> {
                if (pressed) {
                    reset();
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void onAnalog(String name, float value, float tpf) {
        float panSpeed = distance * 0.9f * tpf;
        switch (name) {
            case LEFT -> yaw += rotating ? value * 3f : 0f;
            case RIGHT -> yaw -= rotating ? value * 3f : 0f;
            case UP -> pitch += rotating ? value * 2f : 0f;
            case DOWN -> pitch -= rotating ? value * 2f : 0f;
            case ZOOM_IN -> distance *= 1f - Math.min(value * 0.5f, 0.3f);
            case ZOOM_OUT -> distance *= 1f + Math.min(value * 0.5f, 0.3f);
            case TURN_L -> yaw += 1.5f * tpf;
            case TURN_R -> yaw -= 1.5f * tpf;
            case PAN_F -> pan(0, -panSpeed);
            case PAN_B -> pan(0, panSpeed);
            case PAN_L -> pan(-panSpeed, 0);
            case PAN_R -> pan(panSpeed, 0);
            default -> {
            }
        }
        pitch = FastMath.clamp(pitch, MIN_PITCH, MAX_PITCH);
        distance = FastMath.clamp(distance, MIN_DISTANCE, MAX_DISTANCE);
    }

    /** Сдвиг точки обзора в плоскости земли относительно направления камеры. */
    private void pan(float right, float back) {
        following = false;
        float sin = FastMath.sin(yaw), cos = FastMath.cos(yaw);
        target.addLocal(right * cos + back * sin, 0, -right * sin + back * cos);
    }

    @Override
    public void update(float tpf) {
        if (following && followTarget != null) {
            target.set(followTarget.getWorldTranslation());
        }
        float k = Math.min(1f, tpf * 6f); // скорость сглаживания
        smoothTarget.interpolateLocal(target, k);
        smoothYaw += (yaw - smoothYaw) * k;
        smoothPitch += (pitch - smoothPitch) * k;
        smoothDistance += (distance - smoothDistance) * k;

        float horizontal = smoothDistance * FastMath.cos(smoothPitch);
        Vector3f offset = new Vector3f(horizontal * FastMath.sin(smoothYaw),
                smoothDistance * FastMath.sin(smoothPitch),
                horizontal * FastMath.cos(smoothYaw));
        cam.setLocation(smoothTarget.add(offset));
        cam.lookAt(smoothTarget, Vector3f.UNIT_Y);
    }

    /** Включён ли режим слежения за роботом. */
    public boolean isFollowing() {
        return following;
    }

    @Override
    protected void cleanup(Application app) {
    }
}
