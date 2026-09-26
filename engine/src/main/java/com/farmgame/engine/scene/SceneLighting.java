package com.farmgame.engine.scene;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/** Освещение «солнечного дня»: тёплое солнце под углом и прохладный рассеянный свет неба. */
public final class SceneLighting {

    private SceneLighting() {
    }

    /**
     * Добавляет свет в сцену.
     *
     * @return солнце — от него строятся тени
     */
    public static DirectionalLight apply(Node root) {
        root.addLight(new AmbientLight(new ColorRGBA(0.60f, 0.66f, 0.80f, 1f).multLocal(0.55f)));

        DirectionalLight sun = new DirectionalLight(
                new Vector3f(-0.55f, -1f, -0.35f).normalizeLocal(),
                new ColorRGBA(1.0f, 0.92f, 0.78f, 1f).multLocal(1.35f));
        root.addLight(sun);

        // Слабая контровая подсветка, чтобы теневые стороны не были плоскими.
        DirectionalLight fill = new DirectionalLight(
                new Vector3f(0.6f, -0.4f, 0.7f).normalizeLocal(),
                new ColorRGBA(0.35f, 0.40f, 0.55f, 1f).multLocal(0.5f));
        root.addLight(fill);
        return sun;
    }
}
