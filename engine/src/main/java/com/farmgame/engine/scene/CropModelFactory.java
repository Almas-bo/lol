package com.farmgame.engine.scene;

import com.farmgame.core.crop.CropType;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

/**
 * Модели культур из примитивов. У каждой модели точка отсчёта — у основания (на уровне почвы),
 * поэтому рост можно показывать простым масштабированием узла.
 *
 * <p>Плоды (початок, колосья, тыква...) — дочерний узел {@value #FRUIT}; движок показывает
 * его только на поздних стадиях роста.
 */
public final class CropModelFactory {

    /** Имя дочернего узла с плодами. */
    public static final String FRUIT = "fruit";

    private static final ColorRGBA LEAF = new ColorRGBA(0.28f, 0.60f, 0.20f, 1f);
    private static final ColorRGBA LEAF_LIGHT = new ColorRGBA(0.45f, 0.72f, 0.25f, 1f);
    private static final ColorRGBA STEM = new ColorRGBA(0.40f, 0.62f, 0.22f, 1f);

    private final Materials materials;

    public CropModelFactory(Materials materials) {
        this.materials = materials;
    }

    public Node create(CropType type) {
        Node crop = switch (type) {
            case CORN -> corn();
            case WHEAT -> wheat();
            case CARROT -> carrot();
            case PUMPKIN -> pumpkin();
        };
        crop.setName("crop-" + type.name().toLowerCase());
        return crop;
    }

    /** Показывает или прячет плоды. */
    public static void setFruitVisible(Node crop, boolean visible) {
        Spatial fruit = crop.getChild(FRUIT);
        if (fruit != null) {
            fruit.setCullHint(visible ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
        }
    }

    private Node corn() {
        Node node = new Node();
        Node fruit = new Node(FRUIT);
        float[][] stalks = {{-0.3f, -0.25f}, {0.3f, -0.2f}, {0f, 0.3f}};
        for (int i = 0; i < stalks.length; i++) {
            Vector3f base = new Vector3f(stalks[i][0], 0, stalks[i][1]);
            node.attachChild(Shapes.column("corn-stalk", 0.06f, 0.04f, 1.7f, materials.lit(STEM), base));
            for (int l = 0; l < 3; l++) {
                Geometry leaf = Shapes.geometry("corn-leaf", new Box(0.04f, 0.4f, 0.1f), materials.lit(LEAF),
                        base.add(0, 0.5f + l * 0.35f, 0));
                leaf.setLocalRotation(Shapes.tilt(l * 2.1f + i, 0.7f));
                leaf.move(leaf.getLocalRotation().mult(new Vector3f(0, 0.35f, 0)));
                node.attachChild(leaf);
            }
            Geometry cob = Shapes.geometry("corn-cob", new Sphere(8, 12, 0.11f),
                    materials.lit(new ColorRGBA(0.98f, 0.82f, 0.18f, 1f), 0.3f, 24f),
                    base.add(0.1f, 1.05f, 0));
            cob.setLocalScale(1f, 2.6f, 1f);
            fruit.attachChild(cob);
            fruit.attachChild(Shapes.column("corn-tassel", 0.08f, 0.0f, 0.3f,
                    materials.lit(new ColorRGBA(0.85f, 0.70f, 0.35f, 1f)), base.add(0, 1.7f, 0)));
        }
        node.attachChild(fruit);
        return node;
    }

    private Node wheat() {
        Node node = new Node();
        Node fruit = new Node(FRUIT);
        ColorRGBA straw = new ColorRGBA(0.78f, 0.72f, 0.35f, 1f);
        ColorRGBA ear = new ColorRGBA(0.93f, 0.78f, 0.38f, 1f);
        for (int i = 0; i < 12; i++) {
            float a = i * 2.4f;
            float r = 0.12f + (i % 4) * 0.12f;
            Vector3f base = new Vector3f(FastMath.cos(a) * r, 0, FastMath.sin(a) * r);
            float height = 0.8f + (i % 3) * 0.12f;
            Geometry stalk = Shapes.column("wheat-stalk", 0.02f, 0.015f, height, materials.lit(straw), base);
            node.attachChild(stalk);
            Geometry head = Shapes.geometry("wheat-ear", new Sphere(6, 8, 0.05f), materials.lit(ear),
                    base.add(0, height + 0.08f, 0));
            head.setLocalScale(1f, 3.2f, 1f);
            fruit.attachChild(head);
        }
        node.attachChild(fruit);
        return node;
    }

    private Node carrot() {
        Node node = new Node();
        Node fruit = new Node(FRUIT);
        float[][] spots = {{-0.3f, -0.3f}, {0.3f, -0.25f}, {-0.25f, 0.3f}, {0.3f, 0.3f}};
        for (float[] s : spots) {
            Vector3f base = new Vector3f(s[0], 0, s[1]);
            for (int l = 0; l < 5; l++) {
                Geometry leaf = Shapes.geometry("carrot-leaf", new Box(0.025f, 0.22f, 0.06f),
                        materials.lit(l % 2 == 0 ? LEAF : LEAF_LIGHT), base.add(0, 0.25f, 0));
                leaf.setLocalRotation(Shapes.tilt(l * 1.25f, 0.45f));
                node.attachChild(leaf);
            }
            // Оранжевая "макушка" корнеплода, выглядывающая из земли.
            fruit.attachChild(Shapes.column("carrot-top", 0.13f, 0.1f, 0.12f,
                    materials.lit(new ColorRGBA(0.96f, 0.47f, 0.10f, 1f), 0.2f, 16f), base.add(0, -0.02f, 0)));
        }
        node.attachChild(fruit);
        return node;
    }

    private Node pumpkin() {
        Node node = new Node();
        Node fruit = new Node(FRUIT);
        for (int l = 0; l < 4; l++) {
            Geometry leaf = Shapes.geometry("pumpkin-leaf", new Sphere(6, 10, 0.35f), materials.lit(LEAF),
                    new Vector3f(FastMath.cos(l * 1.6f) * 0.5f, 0.08f, FastMath.sin(l * 1.6f) * 0.5f));
            leaf.setLocalScale(1f, 0.15f, 1f);
            node.attachChild(leaf);
        }
        ColorRGBA orange = new ColorRGBA(0.95f, 0.45f, 0.06f, 1f);
        // Рёбра тыквы: несколько сплюснутых сфер, повёрнутых вокруг вертикали.
        for (int i = 0; i < 4; i++) {
            Geometry lobe = Shapes.geometry("pumpkin-lobe", new Sphere(12, 16, 0.5f),
                    materials.glossy(i % 2 == 0 ? orange : orange.mult(0.92f)), new Vector3f(0, 0.4f, 0));
            lobe.setLocalScale(1.1f, 0.78f, 0.7f);
            lobe.setLocalRotation(Shapes.yaw(i * FastMath.QUARTER_PI));
            fruit.attachChild(lobe);
        }
        fruit.attachChild(Shapes.column("pumpkin-stem", 0.06f, 0.04f, 0.25f,
                materials.lit(new ColorRGBA(0.35f, 0.40f, 0.15f, 1f)), new Vector3f(0, 0.75f, 0)));
        node.attachChild(fruit);
        return node;
    }
}
