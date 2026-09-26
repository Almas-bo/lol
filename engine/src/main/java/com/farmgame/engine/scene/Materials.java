package com.farmgame.engine.scene;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

/** Фабрика материалов: всё освещается стандартным шейдером Lighting.j3md, цвет задаётся напрямую. */
public final class Materials {

    private final AssetManager assetManager;

    public Materials(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    /** Матовый освещаемый материал заданного цвета. */
    public Material lit(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", color);
        mat.setColor("Ambient", color.mult(0.6f));
        mat.setColor("Specular", ColorRGBA.White.mult(0.1f));
        mat.setFloat("Shininess", 8f);
        return mat;
    }

    /** Неосвещаемый материал (для линий сетки). */
    public Material unshaded(ColorRGBA color) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", color);
        return mat;
    }
}
