package com.farmgame.engine.scene;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.CropType;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;

/** Создаёт геометрию поля: грядки с бороздами, культуры, сетку и подписи координат. */
public final class FarmSceneFactory {

    /** Высота верхней грани грядки. */
    public static final float PLOT_TOP = 0.12f;

    private static final ColorRGBA SOIL_DRY = new ColorRGBA(0.52f, 0.36f, 0.22f, 1f);
    private static final ColorRGBA SOIL_WET = new ColorRGBA(0.30f, 0.20f, 0.12f, 1f);
    private static final String SOIL_TAG = "soil";

    private final AssetManager assetManager;
    private final Materials materials;
    private final FarmCoordinates coords;
    private final CropModelFactory crops;
    private final Material soilDry;
    private final Material soilWet;
    private final Material ridgeDry;
    private final Material ridgeWet;

    public FarmSceneFactory(AssetManager assetManager, Materials materials, FarmCoordinates coords) {
        this.assetManager = assetManager;
        this.materials = materials;
        this.coords = coords;
        this.crops = new CropModelFactory(materials);
        this.soilDry = materials.lit(SOIL_DRY);
        this.soilWet = materials.lit(SOIL_WET, 0.6f, 40f); // мокрая почва блестит
        this.ridgeDry = materials.lit(SOIL_DRY.mult(1.12f));
        this.ridgeWet = materials.lit(SOIL_WET.mult(1.15f), 0.6f, 40f);
    }

    /** Линии сетки по границам клеток (включаются/выключаются клавишей G). */
    public Spatial createGrid() {
        float cell = FarmCoordinates.CELL_SIZE;
        Geometry grid = new Geometry("farm-grid", new Grid(coords.height() + 1, coords.width() + 1, cell));
        grid.setMaterial(materials.unshaded(new ColorRGBA(1f, 1f, 1f, 1f).multLocal(0.85f)));
        grid.setLocalTranslation(-cell / 2f, PLOT_TOP + 0.02f, -cell / 2f);
        grid.setShadowMode(RenderQueue.ShadowMode.Off);
        return grid;
    }

    /**
     * Подписи координат вдоль краёв поля: X — по ближнему краю, Y — по левому.
     * Помогают игроку понять, что значит {@code robot.moveTo(x, y)}.
     */
    public Node createAxisLabels() {
        Node labels = new Node("axis-labels");
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        float cell = FarmCoordinates.CELL_SIZE;
        for (int x = 0; x < coords.width(); x++) {
            labels.attachChild(label(font, "x" + x, coords.toWorld(new GridPosition(x, coords.height() - 1))
                    .add(0, 0.35f, cell * 0.85f), new ColorRGBA(1f, 0.92f, 0.35f, 1f)));
        }
        for (int y = 0; y < coords.height(); y++) {
            labels.attachChild(label(font, "y" + y, coords.toWorld(new GridPosition(0, y))
                    .add(-cell * 0.85f, 0.35f, 0), new ColorRGBA(0.55f, 0.85f, 1f, 1f)));
        }
        return labels;
    }

    private Spatial label(BitmapFont font, String text, Vector3f position, ColorRGBA color) {
        BitmapText label = new BitmapText(font);
        label.setText(text);
        label.setSize(0.6f);
        label.setColor(Materials.linear(color));
        label.setQueueBucket(RenderQueue.Bucket.Transparent);
        label.setShadowMode(RenderQueue.ShadowMode.Off);
        // Центрируем текст относительно точки и разворачиваем к камере.
        label.setLocalTranslation(-label.getLineWidth() / 2f, label.getLineHeight() / 2f, 0);
        Node holder = new Node("label-" + text);
        holder.attachChild(label);
        holder.setLocalTranslation(position);
        holder.addControl(new BillboardControl());
        return holder;
    }

    /** Грядка — приподнятая полоса почвы с тремя бороздами. */
    public Node createPlot(GridPosition p) {
        Node plot = new Node("plot-" + p.x() + "-" + p.y());
        float half = FarmCoordinates.CELL_SIZE / 2f * 0.88f;
        Geometry bed = Shapes.geometry("bed", new Box(half, PLOT_TOP / 2f, half), soilDry,
                new Vector3f(0, PLOT_TOP / 2f, 0));
        bed.setUserData(SOIL_TAG, false);
        plot.attachChild(bed);
        for (int i = -1; i <= 1; i++) {
            Geometry ridge = Shapes.geometry("ridge", new Box(half * 0.95f, 0.035f, 0.12f), ridgeDry,
                    new Vector3f(0, PLOT_TOP + 0.02f, i * half * 0.6f));
            ridge.setUserData(SOIL_TAG, true);
            plot.attachChild(ridge);
        }
        plot.setLocalTranslation(coords.toWorld(p));
        return plot;
    }

    /** Меняет цвет почвы в зависимости от полива: мокрая темнее и блестит. */
    public void setWatered(Node plot, boolean watered) {
        for (Spatial child : plot.getChildren()) {
            Boolean ridge = child.getUserData(SOIL_TAG);
            if (ridge != null && child instanceof Geometry g) {
                g.setMaterial(ridge ? (watered ? ridgeWet : ridgeDry) : (watered ? soilWet : soilDry));
            }
        }
    }

    /** Модель культуры, стоящая на грядке. */
    public Node createCrop(GridPosition p, CropType type) {
        Node crop = crops.create(type);
        crop.setLocalTranslation(coords.toWorld(p).add(0, PLOT_TOP, 0));
        return crop;
    }

    /**
     * Показывает стадию роста.
     *
     * @param progress рост от 0 до 1
     * @param time     игровое время — для лёгкого покачивания на ветру
     */
    public void setGrowth(Node crop, double progress, float time) {
        float p = (float) progress;
        float scale = 0.12f + 0.88f * p;
        crop.setLocalScale(scale);
        CropModelFactory.setFruitVisible(crop, p >= 0.7f);
        float phase = crop.getLocalTranslation().x * 0.7f + crop.getLocalTranslation().z * 0.4f;
        float sway = 0.05f * p * FastMath.sin(time * 1.6f + phase);
        crop.setLocalRotation(new Quaternion().fromAngles(sway, 0, sway * 0.6f));
    }

    /** Цвет культуры (для эффектов и интерфейса). */
    public static ColorRGBA colorOf(CropType type) {
        return switch (type) {
            case CORN -> new ColorRGBA(0.98f, 0.85f, 0.20f, 1f);
            case WHEAT -> new ColorRGBA(0.93f, 0.78f, 0.38f, 1f);
            case CARROT -> new ColorRGBA(0.96f, 0.47f, 0.10f, 1f);
            case PUMPKIN -> new ColorRGBA(0.95f, 0.45f, 0.06f, 1f);
        };
    }
}
