package com.farmgame.engine.state;

import com.farmgame.core.crop.CropType;
import com.farmgame.core.farm.Farm;
import com.farmgame.engine.scene.FarmSceneFactory;
import com.farmgame.engine.ui.GameConsole;
import com.farmgame.engine.ui.HudPanel;
import com.farmgame.engine.ui.HudStyle;
import com.farmgame.engine.ui.TextWrap;
import com.farmgame.engine.ui.UiLayout;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Экранный интерфейс: панель фермы (робот, склад) и консоль программы игрока.
 * {@code H} — скрыть/показать весь интерфейс.
 */
public class HudState extends BaseAppState implements ActionListener {

    private static final String TOGGLE = "Hud_Toggle";

    private final Farm farm;
    private final GameConsole console;
    private final UiLayout layout;
    private final Node hudNode = new Node("hud");

    private HudPanel farmPanel;
    private HudPanel consolePanel;

    public HudState(Farm farm, GameConsole console, UiLayout layout) {
        this.farm = farm;
        this.console = console;
        this.layout = layout;
    }

    @Override
    protected void initialize(Application app) {
        UiLayout.Rect f = layout.farm();
        if (f.width() >= 200) {
            farmPanel = new HudPanel("hud-farm", app.getAssetManager(), f.width(), f.height());
            farmPanel.picture().setPosition(f.x(), f.y());
            hudNode.attachChild(farmPanel.picture());
        }
        UiLayout.Rect c = layout.console();
        consolePanel = new HudPanel("hud-console", app.getAssetManager(), c.width(), c.height());
        consolePanel.picture().setPosition(c.x(), c.y());
        hudNode.attachChild(consolePanel.picture());
    }

    @Override
    public void update(float tpf) {
        if (farmPanel != null) {
            Map<CropType, Integer> inventory = farm.inventorySnapshot();
            String farmKey = farm.robotPosition() + "|" + farm.robotSteps() + "|" + inventory;
            farmPanel.redraw(farmKey, g -> paintFarm(g, inventory));
        }
        List<String> lines = console.lines();
        consolePanel.redraw(String.join("\n", lines), g -> paintConsole(g, lines));
    }

    private void paintFarm(Graphics2D g, Map<CropType, Integer> inventory) {
        HudStyle.background(g, farmPanel.width(), farmPanel.height());
        HudStyle.title(g, "Ферма", 16, 26);

        g.setFont(HudStyle.TEXT);
        g.setColor(HudStyle.TEXT_COLOR);
        g.drawString("Робот: x = " + farm.robotPosition().x() + ", y = " + farm.robotPosition().y(), 16, 54);
        g.setColor(HudStyle.MUTED);
        g.drawString("Пройдено клеток: " + farm.robotSteps(), 16, 74);

        g.setColor(HudStyle.TEXT_COLOR);
        g.drawString("Склад:", 16, 100);
        int row = 0;
        for (CropType type : CropType.values()) {
            int x = 16 + (row % 2) * (farmPanel.width() / 2 - 8);
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
        List<String[]> rows = new ArrayList<>();
        for (String line : lines) {
            for (String part : line.split("\n")) {
                String lower = part.toLowerCase();
                boolean error = lower.contains("ошибк") || part.startsWith("✗") || lower.contains("error");
                boolean ok = part.startsWith("✓");
                for (String wrapped : TextWrap.wrap(part, g.getFontMetrics(), consolePanel.width() - 44)) {
                    rows.add(new String[]{wrapped, error ? "e" : ok ? "o" : ""});
                }
            }
        }
        int lineH = 18;
        int capacity = (consolePanel.height() - 46) / lineH;
        int y = 52;
        for (String[] row : rows.subList(Math.max(0, rows.size() - capacity), rows.size())) {
            g.setColor("e".equals(row[1]) ? HudStyle.ERROR : "o".equals(row[1]) ? HudStyle.SUCCESS : HudStyle.TEXT_COLOR);
            g.drawString("> " + row[0], 16, y);
            y += lineH;
        }
    }

    @Override
    public void onAction(String name, boolean pressed, float tpf) {
        if (pressed && TOGGLE.equals(name)) {
            // Прячем весь интерфейс (включая урок и редактор), чтобы полюбоваться фермой.
            Node gui = ((SimpleApplication) getApplication()).getGuiNode();
            boolean hide = gui.getCullHint() != Node.CullHint.Always;
            gui.setCullHint(hide ? Node.CullHint.Always : Node.CullHint.Inherit);
            // Без панелей ферма снова по центру экрана.
            OrbitCameraState camera = getState(OrbitCameraState.class);
            if (camera != null) {
                camera.setViewCenter(hide ? 0f : layout.viewCenterNdc());
            }
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
