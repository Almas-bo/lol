package com.farmgame.engine.scene;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;

/**
 * Небо без текстур: большая сфера с градиентом, записанным в цвета вершин
 * (светлая дымка у горизонта → насыщенная синева в зените).
 */
public final class SkyDome {

    private static final ColorRGBA HORIZON = new ColorRGBA(0.86f, 0.92f, 0.96f, 1f);
    private static final ColorRGBA ZENITH = new ColorRGBA(0.28f, 0.55f, 0.88f, 1f);
    private static final ColorRGBA BELOW = new ColorRGBA(0.62f, 0.72f, 0.62f, 1f);

    private SkyDome() {
    }

    public static Spatial create(Materials materials) {
        Sphere mesh = new Sphere(32, 32, 500f, false, true); // interior = true: видна изнутри
        FloatBuffer positions = mesh.getFloatBuffer(VertexBuffer.Type.Position);
        int vertexCount = mesh.getVertexCount();
        FloatBuffer colors = BufferUtils.createFloatBuffer(vertexCount * 4);
        for (int i = 0; i < vertexCount; i++) {
            // До поворота полюса сферы лежат на оси Z — она станет "вверх".
            float height = positions.get(i * 3 + 2) / 500f;
            ColorRGBA c = height >= 0
                    ? new ColorRGBA().interpolateLocal(HORIZON, ZENITH, FastMath.pow(height, 0.6f))
                    : new ColorRGBA().interpolateLocal(HORIZON, BELOW, Math.min(1f, -height * 4f));
            ColorRGBA lin = Materials.linear(c);
            colors.put(lin.r).put(lin.g).put(lin.b).put(1f);
        }
        colors.flip();
        mesh.setBuffer(VertexBuffer.Type.Color, 4, colors);

        Geometry sky = new Geometry("sky", mesh);
        sky.setMaterial(materials.vertexColored());
        sky.setLocalRotation(Shapes.UPRIGHT);
        sky.setQueueBucket(RenderQueue.Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);
        sky.setShadowMode(RenderQueue.ShadowMode.Off);
        sky.setLocalTranslation(Vector3f.ZERO);
        return sky;
    }
}
