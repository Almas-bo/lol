package com.farmgame.engine;

import com.jme3.system.AppSettings;

/**
 * Точка входа: настраивает окно и запускает {@link FarmGameApp}.
 *
 * <p>Запуск: {@code ./gradlew :engine:run}
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Java Farm — учимся программировать на ферме");
        settings.setResolution(1280, 720);
        settings.setSamples(4);        // сглаживание
        settings.setVSync(true);
        settings.setGammaCorrection(true);
        // Звука пока нет: без аудио-рендерера игра запускается и на машинах без звуковой карты.
        settings.setAudioRenderer(null);

        FarmGameApp app = new FarmGameApp();
        app.setSettings(settings);
        app.setShowSettings(false);    // без стартового диалога jME
        app.setPauseOnLostFocus(false);
        app.start();
    }
}
