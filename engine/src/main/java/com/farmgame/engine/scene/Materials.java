package com.farmgame.engine.scene;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

import java.util.HashMap;
import java.util.Map;

/**
 * Фабрика материалов. Одинаковые материалы кэшируются: меньше переключений шейдеров,
 * выше FPS.
 *
 * <p>Цвета в коде задаются привычными sRGB-значениями (как в графических редакторах),
 * а в шейдер передаются в линейном пространстве — этого требует включённая гамма-коррекция.
 * Без перевода вся сцена выглядит «выбеленной».
 */
public final class Materials {

    private final AssetManager assetManager;
    private final Map<String, Material> cache = new HashMap<>();

    public Materials(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /** Переводит sRGB-цвет в линейное пространство. */
    public static ColorRGBA linear(ColorRGBA srgb) {
        return new ColorRGBA().setAsSrgb(srgb.r, srgb.g, srgb.b, srgb.a);
    }

    /** Матовый освещаемый материал (почва, трава, дерево). */
    public Material lit(ColorRGBA color) {
        return lit(color, 0.05f, 4f);
    }

    /** Глянцевый материал (корпус робота, тыква). */
    public Material glossy(ColorRGBA color) {
        return lit(color, 0.5f, 48f);
    }

    /**
     * Освещаемый материал с настраиваемым бликом.
     *
     * @param specular  сила блика 0..1
     * @param shininess «резкость» блика (чем больше, тем меньше пятно)
     */
    public Material lit(ColorRGBA color, float specular, float shininess) {
        String key = "lit:" + color + ":" + specular + ":" + shininess;
        return cache.computeIfAbsent(key, k -> {
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", linear(color));
            mat.setColor("Ambient", linear(color));
            mat.setColor("Specular", ColorRGBA.White.mult(specular));
            mat.setFloat("Shininess", shininess);
            return mat;
        });
    }

    /**
     * Освещаемый материал с текстурой; {@code tint} умножается на цвет текстуры.
     */
    public Material textured(String key, Texture texture, ColorRGBA tint, float specular, float shininess) {
        return cache.computeIfAbsent("tex:" + key + ":" + tint + ":" + specular, k -> {
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            mat.setTexture("DiffuseMap", texture);
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", linear(tint));
            mat.setColor("Ambient", linear(tint));
            mat.setColor("Specular", ColorRGBA.White.mult(specular));
            mat.setFloat("Shininess", shininess);
            return mat;
        });
    }

    /** Светящийся материал (фары, маячок) — подсвечивается bloom-фильтром. */
    public Material glowing(ColorRGBA color) {
        return cache.computeIfAbsent("glow:" + color, k -> {
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", linear(color));
            mat.setColor("GlowColor", linear(color));
            return mat;
        });
    }

    /** Неосвещаемый материал (линии, небо). */
    public Material unshaded(ColorRGBA color) {
        return cache.computeIfAbsent("unshaded:" + color, k -> {
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", linear(color));
            return mat;
        });
    }

    /** Полупрозрачный неосвещаемый материал (брызги воды). */
    public Material translucent(ColorRGBA color) {
        return cache.computeIfAbsent("translucent:" + color, k -> {
            Material mat = unshadedCopy(color);
            mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            return mat;
        });
    }

    /** Материал, берущий цвет из вершин меша (градиентное небо). */
    public Material vertexColored() {
        return cache.computeIfAbsent("vertex-color", k -> {
            Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setBoolean("VertexColor", true);
            return mat;
        });
    }

    private Material unshadedCopy(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", linear(color));
        return mat;
    }
}
