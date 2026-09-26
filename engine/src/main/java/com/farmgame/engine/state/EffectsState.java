package com.farmgame.engine.state;

import com.farmgame.engine.scene.Materials;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Простая система частиц из примитивов: брызги воды при поливе, комья земли при посадке,
 * «салют» из плодов при сборе урожая. Не требует текстур.
 */
public class EffectsState extends BaseAppState {

    private static final float GRAVITY = 9f;
    private static final ColorRGBA WATER = new ColorRGBA(0.45f, 0.72f, 1f, 0.75f);
    private static final ColorRGBA DIRT = new ColorRGBA(0.45f, 0.30f, 0.18f, 1f);

    private final Node effectsNode = new Node("effects");
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private final Mesh droplet = new Sphere(6, 8, 0.05f);
    private final Mesh chunk = new Box(0.07f, 0.07f, 0.07f);
    private Materials materials;

    private static final class Particle {
        final Geometry geometry;
        final Vector3f velocity;
        float life;
        final float maxLife;

        Particle(Geometry geometry, Vector3f velocity, float life) {
            this.geometry = geometry;
            this.velocity = velocity;
            this.life = life;
            this.maxLife = life;
        }
    }

    @Override
    protected void initialize(Application app) {
        materials = new Materials(app.getAssetManager());
        effectsNode.setShadowMode(RenderQueue.ShadowMode.Off);
    }

    /** Струя воды из «лейки» робота на грядку. */
    public void water(Vector3f target) {
        Material mat = materials.translucent(WATER);
        for (int i = 0; i < 40; i++) {
            Vector3f start = target.add(rand(0.25f), 1.6f + random.nextFloat() * 0.4f, rand(0.25f));
            Vector3f velocity = new Vector3f(rand(1.2f), -1f - random.nextFloat(), rand(1.2f));
            spawn(droplet, mat, start, velocity, 0.35f + random.nextFloat() * 0.3f)
                    .geometry.setQueueBucket(RenderQueue.Bucket.Transparent);
        }
    }

    /** Комья земли при посадке. */
    public void dirt(Vector3f target) {
        burst(target, materials.lit(DIRT), 18, 2.5f);
    }

    /** Разлёт плодов при сборе урожая. */
    public void harvest(Vector3f target, ColorRGBA color) {
        burst(target, materials.glowing(color.mult(0.9f)), 26, 4f);
    }

    /** «Салют» над роботом, когда задание урока выполнено. */
    public void celebrate(Vector3f position) {
        ColorRGBA[] colors = {
                new ColorRGBA(1f, 0.85f, 0.2f, 1f), new ColorRGBA(0.4f, 0.9f, 0.4f, 1f),
                new ColorRGBA(0.4f, 0.7f, 1f, 1f), new ColorRGBA(1f, 0.45f, 0.4f, 1f)};
        for (ColorRGBA c : colors) {
            burst(position.add(0, 1.2f, 0), materials.glowing(c), 22, 6f);
        }
    }

    private void burst(Vector3f target, Material mat, int count, float power) {
        for (int i = 0; i < count; i++) {
            Vector3f velocity = new Vector3f(rand(1.5f), power * (0.6f + random.nextFloat() * 0.5f), rand(1.5f));
            spawn(chunk, mat, target.add(0, 0.3f, 0), velocity, 0.7f + random.nextFloat() * 0.4f);
        }
    }

    private Particle spawn(Mesh mesh, Material mat, Vector3f position, Vector3f velocity, float life) {
        Geometry g = new Geometry("particle", mesh);
        g.setMaterial(mat);
        g.setLocalTranslation(position);
        effectsNode.attachChild(g);
        Particle p = new Particle(g, velocity, life);
        particles.add(p);
        return p;
    }

    private float rand(float spread) {
        return (random.nextFloat() - 0.5f) * 2f * spread;
    }

    @Override
    public void update(float tpf) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= tpf;
            if (p.life <= 0) {
                p.geometry.removeFromParent();
                it.remove();
                continue;
            }
            p.velocity.y -= GRAVITY * tpf;
            p.geometry.move(p.velocity.mult(tpf));
            if (p.geometry.getLocalTranslation().y < 0.1f) {
                p.geometry.getLocalTranslation().y = 0.1f;
                p.velocity.set(0, 0, 0);
            }
            p.geometry.setLocalScale(FastMath.clamp(p.life / p.maxLife * 1.5f, 0.1f, 1f));
            p.geometry.rotate(tpf * 3f, tpf * 2f, 0);
        }
    }

    @Override
    protected void cleanup(Application app) {
        effectsNode.detachAllChildren();
        particles.clear();
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getRootNode().attachChild(effectsNode);
    }

    @Override
    protected void onDisable() {
        effectsNode.removeFromParent();
    }
}
