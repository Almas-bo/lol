package com.farmgame.engine.state;

import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;
import com.farmgame.engine.scene.FarmSceneFactory;
import com.farmgame.engine.ui.GameConsole;
import com.farmgame.engine.ui.HudPanel;
import com.farmgame.engine.ui.HudStyle;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Map;

/**
 * Экранный интерфейс: панель фермы (робот, склад), консоль программы игрока
 * и подсказка по управлению. {@code H} — скрыть/показать интерфейс.
 */
public class HudState extends BaseAppState implements ActionListener {

    private static final String TOGGLE = "Hud_Toggle";
    private static final int MARGIN = 14;

    private final Farm farm;
    private final GameConsole console;
    private final Node hudNode = new Node("hud");

    private HudPanel farmPanel;
    private HudPanel consolePanel;
    private HudPanel helpPanel;

    public HudState(Farm farm, GameConsole console) {
        this.farm = farm;
        this.console = console;
    }

    @Override
    protected void initialize(Application app) {
        int screenW = app.getCamera().getWidth();
        int screenH = app.getCamera().getHeight();

        farmPanel = new HudPanel("hud-farm", app.getAssetManager(), 300, 176);
        farmPanel.picture().setPosition(MARGIN, screenH - MARGIN - farmPanel.height());

        consolePanel = new HudPanel("hud-console", app.getAssetManager(), 460, 214);
        consolePanel.picture().setPosition(MARGIN, MARGIN);

        helpPanel = new HudPanel("hud-help", app.getAssetManager(), 320, 176);
        helpPanel.picture().setPosition(screenW - MARGIN - helpPanel.width(), screenH - MARGIN - helpPanel.height());
        helpPanel.redraw("static", this::paintHelp);

        hudNode.attachChild(farmPanel.picture());
        hudNode.attachChild(consolePanel.picture());
        hudNode.attachChild(helpPanel.picture());
    }

    @Override
    public void update(float tpf) {
        Map<CropType, Integer> inventory = farm.inventorySnapshot();
        String farmKey = farm.robotPosition() + "|" + farm.robotSteps() + "|" + inventory;
        farmPanel.redraw(farmKey, g -> paintFarm(g, inventory));

        List<String> lines = console.lines();
        consolePanel.redraw(String.join("\n", lines), g -> paintConsole(g, lines));
    }

    private void paintFarm(Graphics2D g, Map<CropType, Integer> inventory) {
        HudStyle.background(g, farmPanel.width(), farmPanel.height());
        HudStyle.title(g, "Ферма", 16, 28);

        g.setFont(HudStyle.TEXT);
        g.setColor(HudStyle.TEXT_COLOR);
        g.drawString("Робот: x = " + farm.robotPosition().x() + ", y = " + farm.robotPosition().y(), 16, 54);
        g.setColor(HudStyle.MUTED);
        g.drawString("Пройдено клеток: " + farm.robotSteps(), 16, 74);

        g.setColor(HudStyle.TEXT_COLOR);
        g.drawString("Склад:", 16, 100);
        int row = 0;
        for (CropType type : CropType.values()) {
            int x = 16 + (row % 2) * 140;
            int y = 124 + (row / 2) * 26;
            ColorRGBA c = FarmSceneFactory.colorOf(type);
            g.setColor(new Color(c.r, c.g, c.b));
            g.fillRoundRect(x, y - 12, 14, 14, 5, 5);
            g.setColor(HudStyle.TEXT_COLOR);
            g.drawString(type.displayName() + ": " + inventory.getOrDefault(type, 0), x + 22, y);
            row++;
        }
    }

    private void paintConsole(Graphics2D g, List<String> lines) {
        HudStyle.background(g, consolePanel.width(), consolePanel.height());
        HudStyle.title(g, "Консоль программы", 16, 28);
        g.setFont(HudStyle.MONO);
        int y = 54;
        for (String line : lines) {
            for (String part : line.split("\n")) {
                String lower = part.toLowerCase();
                g.setColor(lower.contains("ошибк") || lower.contains("error") || lower.contains("failed")
                        ? HudStyle.ERROR
                        : HudStyle.TEXT_COLOR);
                g.drawString("> " + part, 16, y);
                y += 19;
                if (y > consolePanel.height() - 8) {
                    return;
                }
            }
        }
    }

    private void paintHelp(Graphics2D g) {
        HudStyle.background(g, helpPanel.width(), helpPanel.height());
        HudStyle.title(g, "Управление", 16, 28);
        String[][] keys = {
                {"Мышь (зажать)", "вращать камеру"},
                {"Колесо", "приблизить"},
                {"W A S D", "сдвинуть обзор"},
                {"Q / E", "повернуть"},
                {"F", "следить за роботом"},
                {"R / G / H", "сброс / сетка / HUD"},
        };
        int y = 54;
        for (String[] k : keys) {
            g.setFont(HudStyle.KEY);
            g.setColor(HudStyle.ACCENT);
            g.drawString(k[0], 16, y);
            g.setFont(HudStyle.SMALL);
            g.setColor(HudStyle.TEXT_COLOR);
            g.drawString(k[1], 150, y);
            y += 20;
        }
    }

    @Override
    public void onAction(String name, boolean pressed, float tpf) {
        if (pressed && TOGGLE.equals(name)) {
            hudNode.setCullHint(hudNode.getCullHint() == Node.CullHint.Always
                    ? Node.CullHint.Inherit : Node.CullHint.Always);
        }
    }

    @Override
    protected void cleanup(Application app) {
        hudNode.detachAllChildren();
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(hudNode);
        getApplication().getInputManager().addMapping(TOGGLE, new KeyTrigger(KeyInput.KEY_H));
        getApplication().getInputManager().addListener(this, TOGGLE);
    }

    @Override
    protected void onDisable() {
        hudNode.removeFromParent();
        getApplication().getInputManager().removeListener(this);
        getApplication().getInputManager().deleteMapping(TOGGLE);
    }
}
