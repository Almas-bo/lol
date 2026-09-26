package com.farmgame.engine.scene;

import com.farmgame.engine.GraphicsQuality;
import com.jme3.asset.AssetManager;
import com.jme3.light.DirectionalLight;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.filters.FXAAFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.EdgeFilteringMode;

/**
 * Пост-обработка кадра: тени от солнца, затенение в углах (SSAO),
 * свечение ярких объектов (bloom) и сглаживание (FXAA). Набор эффектов зависит от {@link GraphicsQuality}.
 */
public final class PostEffects {

    private PostEffects() {
    }

    public static void apply(AssetManager assetManager, ViewPort viewPort, DirectionalLight sun,
                             GraphicsQuality quality) {
        if (!quality.shadows() && !quality.postEffects()) {
            return;
        }
        FilterPostProcessor fpp = new FilterPostProcessor(assetManager);
        if (quality.msaaSamples() > 0) {
            fpp.setNumSamples(quality.msaaSamples());
        }

        if (quality.shadows()) {
            DirectionalLightShadowFilter shadows =
                    new DirectionalLightShadowFilter(assetManager, quality.shadowMapSize(), 3);
            shadows.setLight(sun);
            shadows.setShadowIntensity(0.45f);
            shadows.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
            shadows.setLambda(0.6f);
            fpp.addFilter(shadows);
        }
        if (quality.ambientOcclusion()) {
            // Радиус, интенсивность, масштаб, смещение — подобраны под размер клетки 2 единицы.
            fpp.addFilter(new SSAOFilter(1.2f, 1.6f, 0.25f, 0.1f));
        }
        if (quality.ambientOcclusion()) {
            BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
            bloom.setBloomIntensity(1.6f);
            fpp.addFilter(bloom);
        }
        if (quality.postEffects()) {
            fpp.addFilter(new FXAAFilter());
        }
        viewPort.addProcessor(fpp);
    }
}
