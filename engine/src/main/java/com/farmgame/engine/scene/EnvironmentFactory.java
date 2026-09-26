package com.farmgame.engine.scene;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;

import java.util.Random;

/**
 * Декорации вокруг поля: луг, холмы, забор, деревья, амбар, пруд и камни.
 * Всё собрано из примитивов; расстановка случайная, но с фиксированным seed —
 * сцена одинакова при каждом запуске.
 */
public final class EnvironmentFactory {

    private static final ColorRGBA GRASS = new ColorRGBA(0.36f, 0.62f, 0.26f, 1f);
    private static final ColorRGBA GRASS_DARK = new ColorRGBA(0.27f, 0.50f, 0.20f, 1f);
    private static final ColorRGBA FIELD_EDGE = new ColorRGBA(0.45f, 0.58f, 0.28f, 1f);
    private static final ColorRGBA WOOD = new ColorRGBA(0.52f, 0.36f, 0.22f, 1f);
    private static final ColorRGBA WOOD_LIGHT = new ColorRGBA(0.72f, 0.56f, 0.38f, 1f);
    private static final ColorRGBA BARN_RED = new ColorRGBA(0.66f, 0.16f, 0.12f, 1f);
    private static final ColorRGBA ROOF = new ColorRGBA(0.30f, 0.28f, 0.30f, 1f);
    private static final ColorRGBA TRIM = new ColorRGBA(0.95f, 0.93f, 0.88f, 1f);
    private static final ColorRGBA STONE = new ColorRGBA(0.55f, 0.55f, 0.52f, 1f);
    private static final ColorRGBA WATER = new ColorRGBA(0.25f, 0.50f, 0.72f, 1f);
    private static final ColorRGBA PATH = new ColorRGBA(0.62f, 0.52f, 0.38f, 1f);
    private static final ColorRGBA[] FOLIAGE = {
            new ColorRGBA(0.20f, 0.45f, 0.16f, 1f),
            new ColorRGBA(0.26f, 0.52f, 0.18f, 1f),
            new ColorRGBA(0.32f, 0.55f, 0.20f, 1f),
    };

    private final Materials materials;
    private final FarmCoordinates coords;
    private final Random random = new Random(42);

    public EnvironmentFactory(Materials materials, FarmCoordinates coords) {
        this.materials = materials;
        this.coords = coords;
    }

    /** Собирает все декорации в один узел. */
    public Node create() {
        Node env = new Node("environment");
        env.attachChild(createMeadow());
        env.attachChild(createHills());
        env.attachChild(createFence());
        env.attachChild(createBarn());
        env.attachChild(createPond());
        env.attachChild(createTrees());
        env.attachChild(createRocks());
        return env;
    }

    // ------------------------------------------------------------------ земля

    private Node createMeadow() {
        Node meadow = new Node("meadow");
        Vector3f c = coords.center();

        Geometry grass = Shapes.geometry("grass", new Box(220f, 0.05f, 220f), materials.lit(GRASS),
                c.add(0, -0.1f, 0));
        grass.setShadowMode(RenderQueue.ShadowMode.Receive);
        meadow.attachChild(grass);

        // Слегка другой оттенок под самим полем, чтобы оно читалось как обработанный участок.
        float halfX = coords.width() * FarmCoordinates.CELL_SIZE / 2f + 0.6f;
        float halfZ = coords.height() * FarmCoordinates.CELL_SIZE / 2f + 0.6f;
        Geometry field = Shapes.geometry("field-bed", new Box(halfX, 0.05f, halfZ), materials.lit(FIELD_EDGE),
                c.add(0, -0.06f, 0));
        field.setShadowMode(RenderQueue.ShadowMode.Receive);
        meadow.attachChild(field);

        // Тропинка от амбара к полю.
        Geometry path = Shapes.geometry("path", new Box(0.9f, 0.05f, 4.5f), materials.lit(PATH),
                new Vector3f(barnX() + 2.5f, -0.04f, c.z));
        path.setLocalRotation(Shapes.yaw(FastMath.HALF_PI));
        path.setShadowMode(RenderQueue.ShadowMode.Receive);
        meadow.attachChild(path);
        return meadow;
    }

    private Node createHills() {
        Node hills = new Node("hills");
        Vector3f c = coords.center();
        for (int i = 0; i < 14; i++) {
            float angle = i / 14f * FastMath.TWO_PI + random.nextFloat() * 0.3f;
            float distance = 70f + random.nextFloat() * 40f;
            float radius = 18f + random.nextFloat() * 22f;
            Geometry hill = Shapes.geometry("hill-" + i, new Sphere(16, 24, radius),
                    materials.lit(i % 2 == 0 ? GRASS : GRASS_DARK),
                    c.add(FastMath.cos(angle) * distance, -radius * 0.75f, FastMath.sin(angle) * distance));
            hill.setLocalScale(1.6f, 1f, 1.2f);
            hills.attachChild(hill);
        }
        return hills;
    }

    // ------------------------------------------------------------------ постройки

    private Node createFence() {
        Node fence = new Node("fence");
        float cell = FarmCoordinates.CELL_SIZE;
        float minX = -cell, maxX = coords.width() * cell;
        float minZ = -cell, maxZ = coords.height() * cell;
        float gateCenterZ = coords.center().z;

        int post = 0;
        for (float x = minX; x <= maxX + 0.01f; x += cell) {
            fence.attachChild(fencePost(post++, x, minZ));
            fence.attachChild(fencePost(post++, x, maxZ));
        }
        for (float z = minZ + cell; z < maxZ - 0.01f; z += cell) {
            fence.attachChild(fencePost(post++, maxX, z));
            if (Math.abs(z - gateCenterZ) > cell) { // проём-ворота со стороны амбара
                fence.attachChild(fencePost(post++, minX, z));
            }
        }
        float lengthX = (maxX - minX) / 2f;
        float lengthZ = (maxZ - minZ) / 2f;
        for (float h : new float[]{0.45f, 0.85f}) {
            fence.attachChild(rail(minX + lengthX, h, minZ, lengthX, 0.04f));
            fence.attachChild(rail(minX + lengthX, h, maxZ, lengthX, 0.04f));
            fence.attachChild(rail(maxX, h, minZ + lengthZ, 0.04f, lengthZ));
            // Левая сторона — две секции вокруг ворот.
            float gateHalf = cell;
            float lowLen = (gateCenterZ - gateHalf - minZ) / 2f;
            float highLen = (maxZ - gateCenterZ - gateHalf) / 2f;
            fence.attachChild(rail(minX, h, minZ + lowLen, 0.04f, lowLen));
            fence.attachChild(rail(minX, h, maxZ - highLen, 0.04f, highLen));
        }
        return fence;
    }

    private Geometry fencePost(int index, float x, float z) {
        return Shapes.geometry("fence-post-" + index, new Box(0.08f, 0.55f, 0.08f), materials.lit(WOOD_LIGHT),
                new Vector3f(x, 0.5f, z));
    }

    private Geometry rail(float x, float y, float z, float halfX, float halfZ) {
        return Shapes.geometry("fence-rail", new Box(halfX, 0.05f, halfZ), materials.lit(WOOD_LIGHT),
                new Vector3f(x, y, z));
    }

    private float barnX() {
        return -FarmCoordinates.CELL_SIZE - 12f;
    }

    private Node createBarn() {
        Node barn = new Node("barn");
        float w = 3f, h = 2.4f, d = 4f; // полуразмеры корпуса
        barn.attachChild(Shapes.geometry("barn-body", new Box(w, h, d), materials.lit(BARN_RED), new Vector3f(0, h, 0)));

        // Двускатная крыша: два наклонённых "листа".
        float roofAngle = FastMath.DEG_TO_RAD * 35f;
        float slope = w / FastMath.cos(roofAngle) / 2f + 0.3f;
        for (int side : new int[]{-1, 1}) {
            Geometry roof = Shapes.geometry("barn-roof", new Box(slope, 0.12f, d + 0.3f), materials.lit(ROOF),
                    new Vector3f(side * w / 2f, 2 * h + FastMath.tan(roofAngle) * w / 2f, 0));
            roof.setLocalRotation(new Quaternion().fromAngleAxis(-side * roofAngle, Vector3f.UNIT_Z));
            barn.attachChild(roof);
        }
        // Фронтон (треугольник заменён повернутым квадратом, утопленным в корпус).
        Geometry gable = Shapes.geometry("barn-gable", new Box(w * 0.72f, w * 0.72f, d - 0.05f),
                materials.lit(BARN_RED), new Vector3f(0, 2 * h, 0));
        gable.setLocalRotation(new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Z));
        gable.setLocalScale(1f, FastMath.tan(roofAngle), 1f);
        barn.attachChild(gable);

        // Ворота с белой окантовкой и "крестом" — на стороне, обращённой к полю (+X).
        barn.attachChild(Shapes.geometry("barn-door", new Box(0.05f, 1.6f, 1.5f), materials.lit(BARN_RED.mult(0.8f)),
                new Vector3f(w + 0.02f, 1.6f, 0)));
        barn.attachChild(Shapes.geometry("barn-trim-top", new Box(0.08f, 0.1f, 1.6f), materials.lit(TRIM),
                new Vector3f(w + 0.05f, 3.25f, 0)));
        for (int side : new int[]{-1, 1}) {
            barn.attachChild(Shapes.geometry("barn-trim-side", new Box(0.08f, 1.65f, 0.1f), materials.lit(TRIM),
                    new Vector3f(w + 0.05f, 1.65f, side * 1.55f)));
            Geometry cross = Shapes.geometry("barn-cross", new Box(0.07f, 2.1f, 0.08f), materials.lit(TRIM),
                    new Vector3f(w + 0.06f, 1.6f, 0));
            cross.setLocalRotation(new Quaternion().fromAngleAxis(side * 0.75f, Vector3f.UNIT_X));
            barn.attachChild(cross);
        }
        // Окошко на фронтоне.
        barn.attachChild(Shapes.geometry("barn-window", new Box(0.06f, 0.4f, 0.4f), materials.lit(TRIM),
                new Vector3f(w + 0.03f, 4.1f, 0)));

        // Силос рядом с амбаром.
        barn.attachChild(Shapes.column("silo", 1.3f, 1.3f, 6.5f, materials.lit(STONE.mult(1.2f)),
                new Vector3f(-1.5f, 0, -d - 1.8f)));
        Geometry siloTop = Shapes.geometry("silo-top", new Sphere(12, 16, 1.3f), materials.lit(ROOF),
                new Vector3f(-1.5f, 6.5f, -d - 1.8f));
        barn.attachChild(siloTop);

        barn.setLocalTranslation(barnX(), 0, coords.center().z);
        return barn;
    }

    private Node createPond() {
        Node pond = new Node("pond");
        Vector3f position = new Vector3f(coords.width() * FarmCoordinates.CELL_SIZE + 9f, 0,
                coords.height() * FarmCoordinates.CELL_SIZE + 4f);
        Geometry water = Shapes.column("pond-water", 4f, 4f, 0.08f, materials.lit(WATER, 0.9f, 96f),
                position.add(0, -0.03f, 0));
        water.setLocalScale(1.4f, 1f, 1f);
        water.setShadowMode(RenderQueue.ShadowMode.Receive);
        pond.attachChild(water);
        for (int i = 0; i < 16; i++) {
            float a = i / 16f * FastMath.TWO_PI;
            Geometry stone = Shapes.geometry("pond-stone", new Sphere(8, 10, 0.45f + random.nextFloat() * 0.25f),
                    materials.lit(STONE), position.add(FastMath.cos(a) * 5.6f, 0, FastMath.sin(a) * 4f));
            stone.setLocalScale(1f, 0.5f, 1f);
            pond.attachChild(stone);
        }
        return pond;
    }

    // ------------------------------------------------------------------ природа

    private Node createTrees() {
        Node trees = new Node("trees");
        Vector3f c = coords.center();
        int placed = 0;
        while (placed < 26) {
            float angle = random.nextFloat() * FastMath.TWO_PI;
            float distance = 17f + random.nextFloat() * 32f;
            Vector3f p = c.add(FastMath.cos(angle) * distance, 0, FastMath.sin(angle) * distance);
            if (nearBarnOrPond(p)) {
                continue;
            }
            trees.attachChild(random.nextFloat() < 0.35f ? pine(placed, p) : leafyTree(placed, p));
            placed++;
        }
        return trees;
    }

    private boolean nearBarnOrPond(Vector3f p) {
        float barnZ = coords.center().z;
        float pondX = coords.width() * FarmCoordinates.CELL_SIZE + 9f;
        float pondZ = coords.height() * FarmCoordinates.CELL_SIZE + 4f;
        return (Math.abs(p.x - barnX()) < 8f && Math.abs(p.z - barnZ) < 10f)
                || (Math.abs(p.x - pondX) < 8f && Math.abs(p.z - pondZ) < 7f);
    }

    private Node leafyTree(int index, Vector3f position) {
        Node tree = new Node("tree-" + index);
        float scale = 0.8f + random.nextFloat() * 0.6f;
        tree.attachChild(Shapes.column("trunk", 0.28f, 0.2f, 2.2f, materials.lit(WOOD), Vector3f.ZERO));
        ColorRGBA leaves = FOLIAGE[random.nextInt(FOLIAGE.length)];
        float[][] blobs = {{0, 2.8f, 0, 1.4f}, {0.7f, 2.4f, 0.3f, 1.0f}, {-0.6f, 2.5f, -0.3f, 1.05f}, {0.1f, 3.5f, -0.2f, 0.9f}};
        for (float[] b : blobs) {
            tree.attachChild(Shapes.geometry("foliage", new Sphere(10, 14, b[3]), materials.lit(leaves),
                    new Vector3f(b[0], b[1], b[2])));
        }
        tree.setLocalTranslation(position);
        tree.setLocalScale(scale);
        tree.setLocalRotation(Shapes.yaw(random.nextFloat() * FastMath.TWO_PI));
        return tree;
    }

    private Node pine(int index, Vector3f position) {
        Node tree = new Node("pine-" + index);
        float scale = 0.9f + random.nextFloat() * 0.7f;
        tree.attachChild(Shapes.column("trunk", 0.22f, 0.18f, 1.2f, materials.lit(WOOD), Vector3f.ZERO));
        ColorRGBA needles = FOLIAGE[0].mult(0.85f);
        for (int tier = 0; tier < 3; tier++) {
            float radius = 1.5f - tier * 0.35f;
            tree.attachChild(Shapes.column("pine-tier", radius, 0.05f, 1.8f, materials.lit(needles),
                    new Vector3f(0, 1.0f + tier * 1.0f, 0)));
        }
        tree.setLocalTranslation(position);
        tree.setLocalScale(scale);
        return tree;
    }

    private Node createRocks() {
        Node rocks = new Node("rocks");
        Vector3f c = coords.center();
        for (int i = 0; i < 18; i++) {
            float angle = random.nextFloat() * FastMath.TWO_PI;
            float distance = 12f + random.nextFloat() * 25f;
            Vector3f p = c.add(FastMath.cos(angle) * distance, 0, FastMath.sin(angle) * distance);
            if (nearBarnOrPond(p)) {
                continue;
            }
            Geometry rock = Shapes.geometry("rock-" + i, new Sphere(6, 8, 0.3f + random.nextFloat() * 0.5f),
                    materials.lit(STONE), p);
            rock.setLocalScale(1f + random.nextFloat(), 0.6f, 1f);
            rock.setLocalRotation(Shapes.yaw(random.nextFloat() * FastMath.TWO_PI));
            rocks.attachChild(rock);
        }
        // Кустики травы.
        Cylinder tuft = new Cylinder(2, 6, 0.25f, 0.02f, 0.5f, true, false);
        for (int i = 0; i < 60; i++) {
            float angle = random.nextFloat() * FastMath.TWO_PI;
            float distance = 11f + random.nextFloat() * 30f;
            Vector3f p = c.add(FastMath.cos(angle) * distance, 0.25f, FastMath.sin(angle) * distance);
            Geometry g = Shapes.geometry("tuft", tuft, materials.lit(GRASS_DARK), p);
            g.setLocalRotation(Shapes.UPRIGHT);
            rocks.attachChild(g);
        }
        return rocks;
    }
}
