package com.farmgame.engine.scene;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

/**
 * Собирает модель «робота-трактора» из примитивов: корпус, кабина, колёса и сенсор.
 * Модель смотрит вдоль оси +Z.
 */
public final class RobotModelFactory {

    private final Materials materials;

    public RobotModelFactory(AssetManager assetManager) {
        this.materials = new Materials(assetManager);
    }

    public Node create() {
        Node robot = new Node("robot");

        ColorRGBA bodyColor = new ColorRGBA(0.90f, 0.35f, 0.10f, 1f);
        ColorRGBA wheelColor = new ColorRGBA(0.12f, 0.12f, 0.12f, 1f);

        robot.attachChild(part("body", new Box(0.45f, 0.25f, 0.65f), bodyColor, new Vector3f(0, 0.55f, 0)));
        robot.attachChild(part("cabin", new Box(0.35f, 0.25f, 0.3f), new ColorRGBA(0.85f, 0.9f, 0.95f, 1f),
                new Vector3f(0, 1.05f, -0.2f)));
        robot.attachChild(part("sensor", new Sphere(12, 12, 0.15f), new ColorRGBA(0.2f, 0.8f, 1f, 1f),
                new Vector3f(0, 1.45f, -0.2f)));
        robot.attachChild(part("nose", new Box(0.3f, 0.12f, 0.1f), bodyColor.mult(0.7f),
                new Vector3f(0, 0.5f, 0.72f)));

        Quaternion wheelRotation = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        float[][] wheelSpots = {{-0.52f, 0.45f}, {0.52f, 0.45f}, {-0.52f, -0.45f}, {0.52f, -0.45f}};
        for (int i = 0; i < wheelSpots.length; i++) {
            float radius = wheelSpots[i][1] < 0 ? 0.35f : 0.28f; // задние колёса больше, как у трактора
            Geometry wheel = part("wheel-" + i, new Cylinder(8, 16, radius, 0.18f, true), wheelColor,
                    new Vector3f(wheelSpots[i][0], radius, wheelSpots[i][1]));
            wheel.setLocalRotation(wheelRotation);
            robot.attachChild(wheel);
        }
        return robot;
    }

    private Geometry part(String name, Mesh mesh, ColorRGBA color, Vector3f offset) {
        Geometry geometry = new Geometry(name, mesh);
        geometry.setMaterial(materials.lit(color));
        geometry.setLocalTranslation(offset);
        return geometry;
    }
}
