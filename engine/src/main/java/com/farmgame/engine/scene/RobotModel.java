package com.farmgame.engine.scene;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D-модель робота-трактора из примитивов и её анимации: вращение колёс, мигающий маячок,
 * покачивание корпуса. Модель смотрит вдоль оси +Z.
 *
 * <p>Класс инкапсулирует детали модели: остальной движок работает только с
 * {@link #node()} и методами анимации.
 */
public final class RobotModel {

    private static final ColorRGBA BODY = new ColorRGBA(0.95f, 0.62f, 0.10f, 1f);
    private static final ColorRGBA BODY_DARK = new ColorRGBA(0.20f, 0.22f, 0.25f, 1f);
    private static final ColorRGBA GLASS = new ColorRGBA(0.45f, 0.70f, 0.85f, 1f);
    private static final ColorRGBA TIRE = new ColorRGBA(0.10f, 0.10f, 0.11f, 1f);
    private static final ColorRGBA RIM = new ColorRGBA(0.85f, 0.85f, 0.80f, 1f);
    private static final ColorRGBA BEACON = new ColorRGBA(1f, 0.35f, 0.1f, 1f);
    private static final ColorRGBA LIGHT = new ColorRGBA(1f, 0.97f, 0.8f, 1f);

    private final Node root = new Node("robot");
    private final Node chassis = new Node("chassis");
    private final List<Wheel> wheels = new ArrayList<>();
    private final Spatial beacon;
    private float time;

    private record Wheel(Node spinner, float radius) {
    }

    public RobotModel(Materials materials) {
        root.attachChild(chassis);

        // Корпус: капот спереди, моторный отсек, платформа сзади.
        chassis.attachChild(Shapes.geometry("hood", new Box(0.32f, 0.22f, 0.45f), materials.glossy(BODY),
                new Vector3f(0, 0.62f, 0.35f)));
        chassis.attachChild(Shapes.geometry("grille", new Box(0.26f, 0.16f, 0.02f), materials.lit(BODY_DARK),
                new Vector3f(0, 0.60f, 0.81f)));
        chassis.attachChild(Shapes.geometry("frame", new Box(0.40f, 0.10f, 0.75f), materials.lit(BODY_DARK),
                new Vector3f(0, 0.40f, 0)));
        chassis.attachChild(Shapes.geometry("fender-l", new Box(0.12f, 0.05f, 0.35f), materials.glossy(BODY),
                new Vector3f(-0.50f, 0.95f, -0.4f)));
        chassis.attachChild(Shapes.geometry("fender-r", new Box(0.12f, 0.05f, 0.35f), materials.glossy(BODY),
                new Vector3f(0.50f, 0.95f, -0.4f)));

        // Кабина: стойки, стекло, крыша.
        Vector3f cabin = new Vector3f(0, 1.05f, -0.35f);
        chassis.attachChild(Shapes.geometry("cabin-base", new Box(0.38f, 0.12f, 0.35f), materials.glossy(BODY),
                cabin.add(0, -0.3f, 0)));
        chassis.attachChild(Shapes.geometry("cabin-glass", new Box(0.34f, 0.28f, 0.31f),
                materials.lit(GLASS, 0.9f, 96f), cabin.add(0, 0.1f, 0)));
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                chassis.attachChild(Shapes.geometry("pillar", new Box(0.03f, 0.3f, 0.03f), materials.lit(BODY_DARK),
                        cabin.add(sx * 0.36f, 0.1f, sz * 0.33f)));
            }
        }
        chassis.attachChild(Shapes.geometry("roof", new Box(0.44f, 0.04f, 0.42f), materials.glossy(BODY),
                cabin.add(0, 0.42f, 0)));

        // Выхлопная труба и маячок на крыше.
        chassis.attachChild(Shapes.column("exhaust", 0.045f, 0.045f, 0.55f, materials.lit(BODY_DARK),
                new Vector3f(0.2f, 0.8f, 0.55f)));
        beacon = Shapes.geometry("beacon", new Sphere(10, 12, 0.08f), materials.glowing(BEACON),
                cabin.add(0, 0.52f, 0));
        chassis.attachChild(beacon);

        // Фары.
        for (int sx : new int[]{-1, 1}) {
            chassis.attachChild(Shapes.geometry("headlight", new Sphere(8, 10, 0.06f), materials.glowing(LIGHT),
                    new Vector3f(sx * 0.2f, 0.72f, 0.81f)));
        }
        // "Глаза" робота на крыше кабины — дружелюбный вид для обучающей игры.
        for (int sx : new int[]{-1, 1}) {
            chassis.attachChild(Shapes.geometry("eye", new Sphere(8, 10, 0.07f),
                    materials.glowing(new ColorRGBA(0.3f, 0.9f, 1f, 1f)), cabin.add(sx * 0.14f, 0.28f, 0.32f)));
        }

        // Колёса: задние большие, передние маленькие — как у трактора.
        addWheel(materials, -0.52f, -0.42f, 0.42f, 0.20f);
        addWheel(materials, 0.52f, -0.42f, 0.42f, 0.20f);
        addWheel(materials, -0.40f, 0.48f, 0.26f, 0.14f);
        addWheel(materials, 0.40f, 0.48f, 0.26f, 0.14f);
    }

    private void addWheel(Materials materials, float x, float z, float radius, float width) {
        Node spinner = new Node("wheel");
        Quaternion sideways = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);

        Geometry tire = new Geometry("tire", new Cylinder(2, 20, radius, width, true));
        tire.setMaterial(materials.lit(TIRE, 0.1f, 8f));
        tire.setLocalRotation(sideways);
        spinner.attachChild(tire);

        Geometry rim = new Geometry("rim", new Cylinder(2, 14, radius * 0.55f, width + 0.02f, true));
        rim.setMaterial(materials.lit(RIM, 0.4f, 32f));
        rim.setLocalRotation(sideways);
        spinner.attachChild(rim);

        // Протектор: выступы, по которым видно вращение.
        for (int i = 0; i < 8; i++) {
            float a = i * FastMath.TWO_PI / 8f;
            Geometry lug = Shapes.geometry("lug", new Box(width / 2f + 0.01f, 0.04f, 0.05f), materials.lit(TIRE),
                    new Vector3f(0, FastMath.sin(a) * radius, FastMath.cos(a) * radius));
            lug.setLocalRotation(new Quaternion().fromAngleAxis(-a, Vector3f.UNIT_X));
            spinner.attachChild(lug);
        }
        spinner.setLocalTranslation(x, radius, z);
        chassis.attachChild(spinner);
        wheels.add(new Wheel(spinner, radius));
    }

    public Node node() {
        return root;
    }

    /** Прокручивает колёса на пройденное расстояние (в мировых единицах). */
    public void rollWheels(float distance) {
        for (Wheel wheel : wheels) {
            wheel.spinner().rotate(distance / wheel.radius(), 0, 0);
        }
    }

    /**
     * Анимация «на месте»: маячок мигает, корпус слегка вибрирует от работающего мотора.
     *
     * @param busy выполняет ли робот команду (тогда маячок мигает)
     */
    public void update(float tpf, boolean busy) {
        time += tpf;
        boolean beaconOn = busy && (time % 0.6f) < 0.3f;
        beacon.setCullHint(beaconOn || !busy ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
        chassis.setLocalTranslation(0, 0.012f * FastMath.sin(time * 40f) * (busy ? 1f : 0.3f), 0);
    }

    /** Наклон корпуса вперёд-назад (для «кивка» при работе с грядкой). */
    public void setNod(float radians) {
        chassis.setLocalRotation(new Quaternion().fromAngleAxis(radians, Vector3f.UNIT_X));
    }
}
