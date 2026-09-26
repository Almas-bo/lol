package com.farmgame.core.farm;

import com.farmgame.core.FarmRuleException;
import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.Crop;
import com.farmgame.core.crop.CropType;
import com.farmgame.core.entity.Robot;
import com.farmgame.core.resource.Inventory;

import java.util.Map;

/**
 * Ферма — корневой объект игровой логики: сетка грядок, робот и склад.
 *
 * <p>Все изменения состояния проходят через методы этого класса, которые проверяют
 * игровые правила. Методы {@code synchronized}: код игрока выполняется в отдельном
 * потоке, а движок читает состояние из потока рендеринга.
 */
public class Farm {

    private final int width;
    private final int height;
    private final Plot[][] plots;
    private final Robot robot;
    private final Inventory inventory = new Inventory();

    /**
     * @param width  число столбцов (ось x)
     * @param height число строк (ось y)
     */
    public Farm(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Размер фермы должен быть положительным: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        this.plots = new Plot[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                plots[x][y] = new Plot(new GridPosition(x, y));
            }
        }
        this.robot = new Robot("robot-1", GridPosition.ORIGIN);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public boolean isInside(GridPosition p) {
        return p.x() >= 0 && p.y() >= 0 && p.x() < width && p.y() < height;
    }

    // ------------------------------------------------------------------ сброс

    /** Возвращает ферму в исходное состояние: пустые грядки, робот в (0, 0), пустой склад. */
    public synchronized void reset() {
        for (Plot[] column : plots) {
            for (Plot plot : column) {
                plot.clear();
            }
        }
        inventory.clear();
        resetRobot();
    }

    /** Ставит робота в (0, 0) и обнуляет счётчик шагов, не трогая грядки. */
    public synchronized void resetRobot() {
        robot.placeAt(GridPosition.ORIGIN);
    }

    // ------------------------------------------------------------------ робот

    public synchronized GridPosition robotPosition() {
        return robot.position();
    }

    public synchronized int robotSteps() {
        return robot.stepsTaken();
    }

    /** Перемещает робота в указанную клетку. */
    public synchronized void moveRobot(GridPosition target) {
        requireInside(target);
        robot.moveTo(target);
    }

    /** Сажает культуру в клетку, где стоит робот. */
    public synchronized void plantAtRobot(Crop crop) {
        if (crop == null) {
            throw new FarmRuleException("Нельзя посадить null: создайте культуру через Crop.builder()");
        }
        Plot plot = plotUnderRobot();
        if (!plot.isEmpty()) {
            throw new FarmRuleException("Грядка " + robot.position() + " уже занята");
        }
        plot.plant(crop);
    }

    /** Поливает грядку под роботом. */
    public synchronized void waterAtRobot() {
        Plot plot = plotUnderRobot();
        if (plot.isEmpty()) {
            throw new FarmRuleException("На грядке " + robot.position() + " ничего не растёт — поливать нечего");
        }
        plot.water();
    }

    /**
     * Собирает урожай под роботом.
     *
     * @return количество собранных единиц; 0, если культура ещё не созрела или грядка пуста
     */
    public synchronized int harvestAtRobot() {
        Plot plot = plotUnderRobot();
        if (!plot.isRipe()) {
            return 0;
        }
        Crop crop = plot.clear();
        int amount = crop.expectedYield();
        inventory.add(crop.type(), amount);
        return amount;
    }

    // ------------------------------------------------------------------ время и чтение

    /** Продвигает игровое время: культуры растут. */
    public synchronized void tick(double seconds) {
        for (Plot[] column : plots) {
            for (Plot plot : column) {
                plot.grow(seconds);
            }
        }
    }

    public synchronized PlotState plotAt(GridPosition p) {
        requireInside(p);
        return plots[p.x()][p.y()].snapshot();
    }

    public synchronized Map<CropType, Integer> inventorySnapshot() {
        return inventory.snapshot();
    }

    private Plot plotUnderRobot() {
        GridPosition p = robot.position();
        return plots[p.x()][p.y()];
    }

    private void requireInside(GridPosition p) {
        if (!isInside(p)) {
            throw new FarmRuleException("Клетка " + p + " за пределами фермы " + width + "x" + height);
        }
    }
}
