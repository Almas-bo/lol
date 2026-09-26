package com.farmgame.engine.state;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;
import com.farmgame.core.farm.PlotState;
import com.farmgame.engine.scene.FarmSceneFactory;
import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
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

    private Node[][] plotNodes;
    private Node[][] cropNodes;
    private CropType[][] cropTypes;
    private float time;

    public FarmRenderState(Farm farm, FarmSceneFactory factory, Node parent) {
        this.farm = farm;
        this.factory = factory;
        this.parent = parent;
    }

    @Override
    protected void initialize(Application app) {
        plotNodes = new Node[farm.width()][farm.height()];
        cropNodes = new Node[farm.width()][farm.height()];
        cropTypes = new CropType[farm.width()][farm.height()];
        for (int x = 0; x < farm.width(); x++) {
            for (int y = 0; y < farm.height(); y++) {
                Node plot = factory.createPlot(new GridPosition(x, y));
                plotNodes[x][y] = plot;
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
        time += tpf;
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
        factory.setWatered(plotNodes[x][y], state.watered());

        Node crop = cropNodes[x][y];
        CropType type = state.isEmpty() ? null : state.crop().type();
        if (crop != null && type != cropTypes[x][y]) {
            crop.removeFromParent();
            crop = null;
            cropNodes[x][y] = null;
        }
        if (type == null) {
            return;
        }
        if (crop == null) {
            crop = factory.createCrop(state.position(), type);
            cropNodes[x][y] = crop;
            cropTypes[x][y] = type;
            plotsNode.attachChild(crop);
        }
        factory.setGrowth(crop, state.growthProgress(), time);
    }
}
