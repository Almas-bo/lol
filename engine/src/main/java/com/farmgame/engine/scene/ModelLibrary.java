package com.farmgame.engine.scene;

import com.jme3.anim.AnimComposer;
import com.jme3.asset.AssetManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.material.MatParam;
import com.jme3.material.MatParamTexture;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

import java.util.HashMap;
import java.util.Map;

/**
 * Загрузка готовых 3D-моделей (glTF/GLB) из ресурсов {@code Models/...}.
 *
 * <p>Модели из интернета приходят с PBR-материалами, которым нужна карта окружения.
 * Чтобы они выглядели так же, как остальная сцена (наше солнце, тени, гамма),
 * материалы заменяются на {@code Lighting.j3md} с той же текстурой и цветом.
 * Список моделей и их лицензии — в {@code docs/CREDITS.md}.
 */
public final class ModelLibrary {

    // Kenney (CC0 / MIT): у каждого набора своя текстура-палитра Textures/colormap.png.
    public static final String PINE = "Models/kenney/arena/tree.glb";
    public static final String CLOUD = "Models/kenney/platformer/cloud.glb";
    public static final String GRASS = "Models/kenney/platformer/grass.glb";
    public static final String GRASS_SMALL = "Models/kenney/platformer/grass-small.glb";
    public static final String FLAG = "Models/kenney/platformer/flag.glb";
    public static final String COIN = "Models/kenney/platformer/coin.glb";
    public static final String FARMER = "Models/kenney/platformer/character.glb";
    public static final String TRUCK_GREEN = "Models/kenney/racing/vehicle-truck-green.glb";
    public static final String TRUCK_RED = "Models/kenney/racing/vehicle-truck-red.glb";
    // Khronos glTF Sample Assets: модель CC0, риг и анимации CC-BY 4.0.
    public static final String FOX = "Models/fox/Fox.glb";

    private final AssetManager assetManager;
    private final Map<String, Spatial> prototypes = new HashMap<>();

    public ModelLibrary(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /**
     * Загружает модель (с кэшем) и возвращает её копию.
     *
     * @param height желаемая высота модели в мировых единицах; основание модели — на y = 0
     */
    public Node load(String path, float height) {
        Spatial prototype = prototypes.computeIfAbsent(path, p -> {
            Spatial model = assetManager.loadModel(p);
            convertMaterials(model);
            return model;
        });
        // Анимированные модели нужно копировать глубоко (у каждой копии свой скелет).
        Spatial copy = hasAnimation(prototype) ? prototype.deepClone() : prototype.clone(false);
        return normalize(copy, height);
    }

    /** Ставит модель основанием на y = 0, центрирует по X/Z и масштабирует до нужной высоты. */
    private static Node normalize(Spatial model, float height) {
        model.updateGeometricState();
        BoundingBox box = (BoundingBox) model.getWorldBound();
        float scale = height / Math.max(1e-4f, box.getYExtent() * 2f);
        Vector3f center = box.getCenter();
        model.setLocalScale(model.getLocalScale().mult(scale));
        model.setLocalTranslation(model.getLocalTranslation()
                .subtract(center.x * scale, (center.y - box.getYExtent()) * scale, center.z * scale));
        Node holder = new Node(model.getName() + "-holder");
        holder.attachChild(model);
        return holder;
    }

    /** Первый {@link AnimComposer} в модели или {@code null}. */
    public static AnimComposer animations(Spatial model) {
        AnimComposer[] found = new AnimComposer[1];
        model.depthFirstTraversal(s -> {
            if (found[0] == null && s.getControl(AnimComposer.class) != null) {
                found[0] = s.getControl(AnimComposer.class);
            }
        });
        return found[0];
    }

    private static boolean hasAnimation(Spatial model) {
        return animations(model) != null;
    }

    /** Заменяет PBR-материалы на Lighting.j3md, сохраняя текстуру, цвет и двусторонность. */
    private void convertMaterials(Spatial model) {
        Map<Material, Material> converted = new HashMap<>();
        model.depthFirstTraversal(spatial -> {
            if (spatial instanceof Geometry g && g.getMaterial() != null) {
                g.setMaterial(converted.computeIfAbsent(g.getMaterial(), this::toLighting));
            }
        });
    }

    private Material toLighting(Material pbr) {
        if (!pbr.getMaterialDef().getAssetName().contains("PBR")) {
            return pbr;
        }
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        MatParamTexture map = pbr.getTextureParam("BaseColorMap");
        ColorRGBA color = ColorRGBA.White;
        MatParam base = pbr.getParam("BaseColor");
        if (base != null && base.getValue() instanceof ColorRGBA c) {
            color = c;
        }
        if (map != null) {
            Texture texture = map.getTextureValue();
            // Палитры Kenney — это цветные квадраты: без сглаживания цвета не «перетекают».
            texture.setMagFilter(Texture.MagFilter.Nearest);
            mat.setTexture("DiffuseMap", texture);
        }
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", color);
        mat.setColor("Ambient", color);
        mat.setColor("Specular", ColorRGBA.White.mult(0.08f));
        mat.setFloat("Shininess", 12f);
        RenderState.FaceCullMode cull = pbr.getAdditionalRenderState().getFaceCullMode();
        mat.getAdditionalRenderState().setFaceCullMode(cull);
        if (pbr.getAdditionalRenderState().getBlendMode() == RenderState.BlendMode.Alpha) {
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        }
        return mat;
    }
}
