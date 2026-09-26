package com.farmgame.engine.state;

import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;
import com.farmgame.engine.ui.GameConsole;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;

import java.util.Map;

/** Экранный интерфейс: склад фермы, позиция робота и последние сообщения консоли. */
public class HudState extends BaseAppState {

    private final Farm farm;
    private final GameConsole console;
    private BitmapText text;

    public HudState(Farm farm, GameConsole console) {
        this.farm = farm;
        this.console = console;
    }

    @Override
    protected void initialize(Application app) {
        BitmapFont font = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        text = new BitmapText(font);
        text.setSize(font.getCharSet().getRenderedSize());
        text.setColor(ColorRGBA.White);
        text.setLocalTranslation(10, app.getCamera().getHeight() - 10, 0);
    }

    @Override
    public void update(float tpf) {
        StringBuilder sb = new StringBuilder();
        sb.append("Robot: ").append(farm.robotPosition())
                .append("   steps: ").append(farm.robotSteps()).append('\n');
        sb.append("Barn: ");
        Map<CropType, Integer> inventory = farm.inventorySnapshot();
        for (CropType type : CropType.values()) {
            sb.append(type).append('=').append(inventory.getOrDefault(type, 0)).append("  ");
        }
        sb.append("\n\n");
        for (String line : console.lines()) {
            sb.append("> ").append(line).append('\n');
        }
        text.setText(sb);
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(text);
    }

    @Override
    protected void onDisable() {
        text.removeFromParent();
    }
}
