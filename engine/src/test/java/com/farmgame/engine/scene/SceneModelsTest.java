package com.farmgame.engine.scene;

import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.CropType;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.scene.Node;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Строит все модели сцены без окна и видеокарты: ловит ошибки в геометрии
 * (неверные размеры примитивов, отсутствующие материалы) ещё на этапе сборки.
 */
class SceneModelsTest {

    private static Materials materials;
    private static AssetManager assetManager;
    private final FarmCoordinates coords = new FarmCoordinates(8, 6);

    @BeforeAll
    static void setUp() {
        assetManager = new DesktopAssetManager(true);
        materials = new Materials(assetManager);
    }

    @Test
    void everyCropTypeHasModelWithFruit() {
        CropModelFactory factory = new CropModelFactory(materials);
        for (CropType type : CropType.values()) {
            Node crop = factory.create(type);
            assertFalse(crop.getChildren().isEmpty(), type.name());
            assertNotNull(crop.getChild(CropModelFactory.FRUIT), type + " должен иметь узел плодов");
        }
    }

    @Test
    void environmentRobotAndSkyAreBuilt() {
        Node env = new EnvironmentFactory(materials, new ModelLibrary(assetManager), coords).create();
        assertTrue(env.getQuantity() > 5);
        assertTrue(new RobotModel(materials).node().getQuantity() > 0);
        assertNotNull(SkyDome.create(materials));
    }

    @Test
    void plotsAndLabelsAreBuilt() {
        FarmSceneFactory factory = new FarmSceneFactory(assetManager, materials, coords);
        Node plot = factory.createPlot(new GridPosition(1, 2));
        factory.setWatered(plot, true);
        Node crop = factory.createCrop(new GridPosition(1, 2), CropType.PUMPKIN);
        factory.setGrowth(crop, 1.0, 0f);
        assertTrue(factory.createAxisLabels().getQuantity() == coords.width() + coords.height());
        assertNotNull(factory.createGrid());
    }

    @Test
    void downloadedModelsLoadWithConvertedMaterialsAndAnimations() throws Exception {
        ModelLibrary library = new ModelLibrary(assetManager);
        for (var field : ModelLibrary.class.getFields()) {
            if (field.getType() == String.class) {
                String path = (String) field.get(null);
                Node model = library.load(path, 2f);
                model.updateGeometricState();
                var box = (com.jme3.bounding.BoundingBox) model.getWorldBound();
                assertEquals(2f, box.getYExtent() * 2f, 0.05f, path + " должна быть высотой 2");
                assertEquals(0f, box.getCenter().y - box.getYExtent(), 0.05f, path + " должна стоять на земле");
                model.depthFirstTraversal(s -> {
                    if (s instanceof com.jme3.scene.Geometry g) {
                        assertFalse(g.getMaterial().getMaterialDef().getAssetName().contains("PBR"), path);
                    }
                });
            }
        }
        assertNotNull(ModelLibrary.animations(library.load(ModelLibrary.FOX, 1f)).getAnimClip("Walk"));
        assertNotNull(ModelLibrary.animations(library.load(ModelLibrary.FARMER, 1f)).getAnimClip("jump"));
    }
}
