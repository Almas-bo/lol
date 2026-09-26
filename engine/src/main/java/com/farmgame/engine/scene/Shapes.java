package com.farmgame.engine.scene;

import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.shape.Cylinder;

/** Мелкие помощники для сборки моделей из примитивов. */
final class Shapes {

    /** Поворот, ставящий цилиндр jME (ось Z) вертикально (ось Y). */
    static final Quaternion UPRIGHT = new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X);

    private static final float MIN_RADIUS = 0.001f;

    private Shapes() {
    }

    static Geometry geometry(String name, Mesh mesh, Material material, Vector3f position) {
        Geometry geometry = new Geometry(name, mesh);
        geometry.setMaterial(material);
        geometry.setLocalTranslation(position);
        return geometry;
    }

    /**
     * Вертикальный цилиндр/конус, стоящий основанием в точке {@code base}.
     *
     * @param bottomRadius радиус снизу
     * @param topRadius    радиус сверху (0 — конус)
     */
    static Geometry column(String name, float bottomRadius, float topRadius, float height, Material material,
                           Vector3f base) {
        // У Cylinder radius относится к -Z, radius2 к +Z; после UPRIGHT -Z смотрит вниз.
        // jME не допускает нулевой радиус, поэтому "острие" конуса — очень маленький круг.
        Cylinder mesh = new Cylinder(2, 12, Math.max(bottomRadius, MIN_RADIUS), Math.max(topRadius, MIN_RADIUS),
                height, true, false);
        Geometry geometry = geometry(name, mesh, material, base.add(0, height / 2f, 0));
        geometry.setLocalRotation(UPRIGHT);
        return geometry;
    }

    static Quaternion yaw(float radians) {
        return new Quaternion().fromAngleAxis(radians, Vector3f.UNIT_Y);
    }

    static Quaternion tilt(float yaw, float pitch) {
        return yaw(yaw).mult(new Quaternion().fromAngleAxis(pitch, Vector3f.UNIT_X));
    }
}
