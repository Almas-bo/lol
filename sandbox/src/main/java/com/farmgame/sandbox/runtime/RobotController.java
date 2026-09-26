package com.farmgame.sandbox.runtime;

import com.farmgame.core.FarmRuleException;
import com.farmgame.core.GridPosition;
import com.farmgame.core.crop.Crop;
import com.farmgame.core.farm.Farm;
import com.farmgame.sandbox.api.RobotActionException;
import com.farmgame.sandbox.api.RobotApi;
import com.farmgame.sandbox.command.CommandSink;
import com.farmgame.sandbox.command.RobotCommand;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Реализация {@link RobotApi}: превращает вызовы игрока в {@link RobotCommand}
 * и переводит внутренние ошибки в понятные игроку {@link RobotActionException}.
 *
 * <p>Игрок никогда не получает ссылку на {@link Farm} или {@code core.entity.Robot} —
 * только на этот фасад (инкапсуляция + паттерн Facade).
 */
public final class RobotController implements RobotApi {

    private final Farm farm;
    private final CommandSink sink;
    private final Consumer<String> console;

    public RobotController(Farm farm, CommandSink sink, Consumer<String> console) {
        this.farm = Objects.requireNonNull(farm, "farm");
        this.sink = Objects.requireNonNull(sink, "sink");
        this.console = Objects.requireNonNull(console, "console");
    }

    @Override
    public void moveTo(int x, int y) {
        send(new RobotCommand.MoveTo(new GridPosition(x, y)));
    }

    @Override
    public int getX() {
        return farm.robotPosition().x();
    }

    @Override
    public int getY() {
        return farm.robotPosition().y();
    }

    @Override
    public void plant(Crop crop) {
        send(new RobotCommand.Plant(crop));
    }

    @Override
    public void water() {
        send(new RobotCommand.Water());
    }

    @Override
    public int harvest() {
        return send(new RobotCommand.Harvest());
    }

    @Override
    public void pause(double seconds) {
        try {
            send(new RobotCommand.Pause(seconds));
        } catch (IllegalArgumentException e) {
            throw new RobotActionException(e.getMessage());
        }
    }

    @Override
    public void say(String message) {
        console.accept(String.valueOf(message));
    }

    private int send(RobotCommand command) {
        try {
            return sink.execute(command);
        } catch (FarmRuleException e) {
            throw new RobotActionException(e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProgramStoppedException();
        }
    }

    /**
     * Программа игрока была остановлена (таймаут или кнопка «Стоп»).
     * Наследуется от {@link Error}, чтобы {@code catch (Exception e)} в коде игрока
     * случайно не «проглотил» остановку.
     */
    static final class ProgramStoppedException extends Error {
        ProgramStoppedException() {
            super("Программа остановлена");
        }
    }
}
