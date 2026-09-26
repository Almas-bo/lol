package com.farmgame.engine.state;

import com.farmgame.core.GridPosition;
import com.farmgame.core.farm.Farm;
import com.farmgame.core.farm.PlotState;
import com.farmgame.engine.scene.FarmSceneFactory;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

/**
 * Продвигает игровое время фермы и отображает грядки и культуры.
 *
 * <p>Логика — источник истины: каждый кадр берутся снимки {@link PlotState},
 * и сцена приводится в соответствие с ними.
 */
public class FarmRenderState extends BaseAppState {

    private final Farm farm;
    private final FarmSceneFactory factory;
    private final Node parent;
    private final Node plotsNode = new Node("plots");

    private Geometry[][] plotGeometries;
    private Geometry[][] cropGeometries;

    public FarmRenderState(Farm farm, FarmSceneFactory factory, Node parent) {
        this.farm = farm;
        this.factory = factory;
        this.parent = parent;
    }

    @Override
    protected void initialize(Application app) {
        plotGeometries = new Geometry[farm.width()][farm.height()];
        cropGeometries = new Geometry[farm.width()][farm.height()];
        for (int x = 0; x < farm.width(); x++) {
            for (int y = 0; y < farm.height(); y++) {
                Geometry plot = factory.createPlot(new GridPosition(x, y));
                plotGeometries[x][y] = plot;
                plotsNode.attachChild(plot);
            }
        }
    }

    @Override
    protected void onEnable() {
        parent.attachChild(plotsNode);
    }

    @Override
    protected void onDisable() {
        plotsNode.removeFromParent();
    }

    @Override
    protected void cleanup(Application app) {
        plotsNode.detachAllChildren();
    }

    @Override
    public void update(float tpf) {
        farm.tick(tpf);
        for (int x = 0; x < farm.width(); x++) {
            for (int y = 0; y < farm.height(); y++) {
                syncPlot(farm.plotAt(new GridPosition(x, y)));
            }
        }
    }

    private void syncPlot(PlotState state) {
        int x = state.position().x();
        int y = state.position().y();
        factory.setWatered(plotGeometries[x][y], state.watered());

        Geometry crop = cropGeometries[x][y];
        if (state.isEmpty()) {
            if (crop != null) {
                crop.removeFromParent();
                cropGeometries[x][y] = null;
            }
            return;
        }
        if (crop == null) {
            crop = factory.createCrop(state.position(), state.crop().type());
            cropGeometries[x][y] = crop;
            plotsNode.attachChild(crop);
        }
        // Семечко маленькое, созревшая культура — в полный размер и чуть приподнята.
        float scale = 0.15f + 0.85f * (float) state.growthProgress();
        factory.setGrowth(crop, scale, state.isRipe() ? 1.3f : 1f);
    }
}
