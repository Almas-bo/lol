package com.farmgame.engine.scene;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

/** Базовое освещение: мягкий рассеянный свет + «солнце». */
public final class SceneLighting {

    private SceneLighting() {
    }

    public static void apply(Node root) {
        AmbientLight ambient = new AmbientLight(ColorRGBA.White.mult(0.45f));
        root.addLight(ambient);

        DirectionalLight sun = new DirectionalLight(
                new Vector3f(-0.5f, -1f, -0.4f).normalizeLocal(),
                ColorRGBA.White.mult(0.9f));
        root.addLight(sun);
    }
}
