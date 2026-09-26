package com.farmgame.sandbox.command;

import com.farmgame.core.farm.Farm;

import java.util.Objects;

/** Мгновенно применяет команды к ферме в вызывающем потоке. Для тестов и headless-режима. */
public final class DirectCommandSink implements CommandSink {

    private final Farm farm;

    public DirectCommandSink(Farm farm) {
        this.farm = Objects.requireNonNull(farm, "farm");
    }

    @Override
    public int execute(RobotCommand command) {
        return command.applyTo(farm);
    }
}
