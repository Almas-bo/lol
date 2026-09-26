package com.farmgame.engine.scene;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.CropType;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Grid;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

import java.util.EnumMap;
import java.util.Map;

/** Создаёт геометрию фермы: землю, сетку, грядки и культуры. */
public final class FarmSceneFactory {

    private static final ColorRGBA GRASS = new ColorRGBA(0.30f, 0.62f, 0.25f, 1f);
    private static final ColorRGBA SOIL_DRY = new ColorRGBA(0.55f, 0.38f, 0.22f, 1f);
    private static final ColorRGBA SOIL_WET = new ColorRGBA(0.33f, 0.22f, 0.12f, 1f);

    /** Радиус сферы культуры в полном размере. */
    public static final float CROP_RADIUS = 0.55f;
    /** Высота верхней грани грядки. */
    public static final float PLOT_TOP = 0.07f;

    private final Materials materials;
    private final FarmCoordinates coords;
    private final Material soilDry;
    private final Material soilWet;
    private final Map<CropType, Material> cropMaterials = new EnumMap<>(CropType.class);

    public FarmSceneFactory(AssetManager assetManager, FarmCoordinates coords) {
        this.materials = new Materials(assetManager);
        this.coords = coords;
        this.soilDry = materials.lit(SOIL_DRY);
        this.soilWet = materials.lit(SOIL_WET);
        for (CropType type : CropType.values()) {
            cropMaterials.put(type, materials.lit(colorOf(type)));
        }
    }

    /** Травяная подложка под всей фермой с запасом по краям. */
    public Spatial createGround() {
        float margin = 4f;
        float halfX = coords.width() * FarmCoordinates.CELL_SIZE / 2f + margin;
        float halfZ = coords.height() * FarmCoordinates.CELL_SIZE / 2f + margin;
        Geometry ground = new Geometry("ground", new Box(halfX, 0.05f, halfZ));
        ground.setMaterial(materials.lit(GRASS));
        ground.setLocalTranslation(coords.center().add(0, -0.1f, 0));
        return ground;
    }

    /** Линии сетки по границам клеток. */
    public Spatial createGrid() {
        float cell = FarmCoordinates.CELL_SIZE;
        // Grid(xLines, yLines, spacing): линий на одну больше, чем клеток.
        Geometry grid = new Geometry("farm-grid", new Grid(coords.height() + 1, coords.width() + 1, cell));
        grid.setMaterial(materials.unshaded(new ColorRGBA(0.15f, 0.1f, 0.05f, 1f)));
        grid.setLocalTranslation(-cell / 2f, 0.07f, -cell / 2f);
        return grid;
    }

    /** Грядка — плоский блок почвы в клетке. */
    public Geometry createPlot(GridPosition p) {
        float half = FarmCoordinates.CELL_SIZE / 2f * 0.85f;
        Geometry plot = new Geometry("plot-" + p.x() + "-" + p.y(), new Box(half, 0.06f, half));
        plot.setMaterial(soilDry);
        plot.setLocalTranslation(coords.toWorld(p).add(0, 0.01f, 0));
        return plot;
    }

    /** Меняет цвет почвы в зависимости от полива. */
    public void setWatered(Geometry plot, boolean watered) {
        plot.setMaterial(watered ? soilWet : soilDry);
    }

    /** Культура — цветная сфера; её масштаб отражает стадию роста. */
    public Geometry createCrop(GridPosition p, CropType type) {
        Geometry crop = new Geometry("crop-" + p.x() + "-" + p.y(), new Sphere(12, 16, CROP_RADIUS));
        crop.setMaterial(cropMaterials.get(type));
        crop.setLocalTranslation(coords.toWorld(p));
        return crop;
    }

    /** Масштабирует культуру по стадии роста так, чтобы она стояла на грядке, а не проваливалась в неё. */
    public void setGrowth(Geometry crop, float scale, float heightFactor) {
        crop.setLocalScale(scale, scale * heightFactor, scale);
        Vector3f position = crop.getLocalTranslation();
        crop.setLocalTranslation(position.x, PLOT_TOP + CROP_RADIUS * scale * heightFactor, position.z);
    }

    /** Цвет культуры на поле. */
    public static ColorRGBA colorOf(CropType type) {
        return switch (type) {
            case CORN -> new ColorRGBA(0.98f, 0.85f, 0.20f, 1f);
            case WHEAT -> new ColorRGBA(0.85f, 0.72f, 0.45f, 1f);
            case CARROT -> new ColorRGBA(0.95f, 0.50f, 0.12f, 1f);
            case PUMPKIN -> new ColorRGBA(0.90f, 0.40f, 0.05f, 1f);
        };
    }
}
