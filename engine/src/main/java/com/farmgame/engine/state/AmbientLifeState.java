package com.farmgame.engine.state;

import com.farmgame.engine.scene.FarmCoordinates;
import com.farmgame.engine.scene.ModelLibrary;
import com.jme3.anim.AnimComposer;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * «Жизнь» вокруг фермы: плывущие облака, лиса, гуляющая вдоль забора,
 * и робот-маскот у ворот, который радуется выполненным заданиям.
 */
public class AmbientLifeState extends BaseAppState {

    private static final float CLOUD_SPEED = 1.4f;
    private static final float CLOUD_RANGE = 90f;
    private static final float FOX_SPEED = 1.7f;

    private final FarmCoordinates coords;
    private final Spatial robot;
    private final Node lifeNode = new Node("ambient-life");
    private final List<Node> clouds = new ArrayList<>();
    private final Random random = new Random(11);

    private ModelLibrary models;

    private Node fox;
    private AnimComposer foxAnim;
    private final List<Vector3f> foxPath = new ArrayList<>();
    private int foxTarget;
    private float foxRest;
    private float foxHeading;

    private Node mascot;
    private AnimComposer mascotAnim;
    private float cheerTime;

    /**
     * @param robot модель робота — маскот поворачивается к ней
     */
    public AmbientLifeState(FarmCoordinates coords, Spatial robot) {
        this.coords = coords;
        this.robot = robot;
    }

    @Override
    protected void initialize(Application app) {
        models = new ModelLibrary(app.getAssetManager());
        createClouds();
        createFox();
        createMascot();
    }

    private void createClouds() {
        Vector3f c = coords.center();
        for (int i = 0; i < 8; i++) {
            Node cluster = new Node("cloud-" + i);
            int puffs = 3 + random.nextInt(3);
            for (int p = 0; p < puffs; p++) {
                Node puff = models.load(ModelLibrary.CLOUD, 2.5f + random.nextFloat() * 2f);
                puff.setLocalTranslation(p * 2.6f - puffs * 1.3f, random.nextFloat() * 1.2f, random.nextFloat() * 2f - 1f);
                puff.setLocalScale(1.4f, 0.7f, 1.1f);
                cluster.attachChild(puff);
            }
            cluster.setLocalTranslation(c.x - CLOUD_RANGE + random.nextFloat() * 2 * CLOUD_RANGE,
                    18f + random.nextFloat() * 8f, c.z - 50f + random.nextFloat() * 90f);
            // Облака отбрасывают мягкие бегущие тени на поле.
            cluster.setShadowMode(RenderQueue.ShadowMode.Cast);
            clouds.add(cluster);
            lifeNode.attachChild(cluster);
        }
    }

    private void createFox() {
        fox = models.load(ModelLibrary.FOX, 1.05f);
        foxAnim = ModelLibrary.animations(fox);
        // Прямоугольный маршрут снаружи забора.
        float cell = FarmCoordinates.CELL_SIZE;
        float minX = -cell - 3f;
        float maxX = coords.width() * cell + 3f;
        float minZ = -cell - 3f;
        float maxZ = coords.height() * cell + 3f;
        foxPath.add(new Vector3f(minX, 0, maxZ));
        foxPath.add(new Vector3f(maxX, 0, maxZ));
        foxPath.add(new Vector3f(maxX, 0, minZ));
        foxPath.add(new Vector3f(minX, 0, minZ));
        fox.setLocalTranslation(foxPath.get(3));
        foxTarget = 0;
        play(foxAnim, "Walk");
        lifeNode.attachChild(fox);
    }

    private void createMascot() {
        mascot = models.load(ModelLibrary.FARMER, 1.8f);
        mascotAnim = ModelLibrary.animations(mascot);
        mascot.setLocalTranslation(-FarmCoordinates.CELL_SIZE - 2.2f, 0, coords.center().z - 3.2f);
        play(mascotAnim, "idle");
        lifeNode.attachChild(mascot);
    }

    /** Маскот подпрыгивает от радости (задание выполнено). */
    public void cheer() {
        cheerTime = 2.4f;
        play(mascotAnim, "jump");
    }

    @Override
    public void update(float tpf) {
        updateClouds(tpf);
        updateFox(tpf);
        updateMascot(tpf);
    }

    private void updateClouds(float tpf) {
        float center = coords.center().x;
        for (Node cloud : clouds) {
            Vector3f p = cloud.getLocalTranslation();
            float x = p.x + CLOUD_SPEED * tpf;
            if (x > center + CLOUD_RANGE) {
                x = center - CLOUD_RANGE;
            }
            cloud.setLocalTranslation(x, p.y, p.z);
        }
    }

    private void updateFox(float tpf) {
        if (foxRest > 0) {
            foxRest -= tpf;
            if (foxRest <= 0) {
                play(foxAnim, "Walk");
            }
            return;
        }
        Vector3f position = fox.getLocalTranslation().clone();
        Vector3f target = foxPath.get(foxTarget);
        Vector3f delta = target.subtract(position);
        float distance = delta.length();
        if (distance < 0.05f) {
            foxTarget = (foxTarget + 1) % foxPath.size();
            if (random.nextFloat() < 0.5f) {
                foxRest = 3f + random.nextFloat() * 3f;
                play(foxAnim, "Survey"); // лиса останавливается и осматривается
            }
            return;
        }
        float desired = FastMath.atan2(delta.x, delta.z);
        float diff = desired - foxHeading;
        while (diff > FastMath.PI) {
            diff -= FastMath.TWO_PI;
        }
        while (diff < -FastMath.PI) {
            diff += FastMath.TWO_PI;
        }
        foxHeading += FastMath.clamp(diff, -3f * tpf, 3f * tpf);
        fox.setLocalRotation(new Quaternion().fromAngleAxis(foxHeading, Vector3f.UNIT_Y));
        position.addLocal(delta.normalizeLocal().multLocal(Math.min(distance, FOX_SPEED * tpf)));
        fox.setLocalTranslation(position);
    }

    private void updateMascot(float tpf) {
        if (cheerTime > 0) {
            cheerTime -= tpf;
            if (cheerTime <= 0) {
                play(mascotAnim, "idle");
            }
        }
        // Маскот следит взглядом за роботом.
        Vector3f toRobot = robot.getWorldTranslation().subtract(mascot.getWorldTranslation());
        float angle = FastMath.atan2(toRobot.x, toRobot.z);
        Quaternion target = new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y);
        Quaternion current = mascot.getLocalRotation().clone();
        current.slerp(target, Math.min(1f, tpf * 3f));
        mascot.setLocalRotation(current);
    }

    private static void play(AnimComposer composer, String clip) {
        if (composer != null && composer.getAnimClipsNames().contains(clip)) {
            composer.setCurrentAction(clip);
        }
    }

    @Override
    protected void cleanup(Application app) {
        lifeNode.detachAllChildren();
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getRootNode().attachChild(lifeNode);
    }

    @Override
    protected void onDisable() {
        lifeNode.removeFromParent();
    }
}
