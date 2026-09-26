package com.farmgame.engine;

import com.jme3.system.AppSettings;

import java.util.Arrays;

/**
 * Точка входа: настраивает окно и запускает {@link FarmGameApp}.
 *
 * <p>Запуск: {@code ./gradlew :engine:run}. Аргументы:
 * <ul>
 *   <li>{@code --quality=low|medium|high} — качество графики (по умолчанию high);</li>
 *   <li>{@code --fullscreen} — полноэкранный режим;</li>
 *   <li>{@code --progress=путь} — файл прогресса (по умолчанию {@code ~/.javafarm/progress.properties});</li>
 *   <li>{@code --width=1920 --height=1080} — размер окна (по умолчанию 1440×810).</li>
 * </ul>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        // HUD рисуется через Java2D в текстуру; окно AWT не нужно (важно для macOS + GLFW).
        System.setProperty("java.awt.headless", "true");

        for (String arg : args) {
            if (arg.startsWith("--progress=")) {
                System.setProperty("farmgame.progress", arg.substring("--progress=".length()));
            }
        }
        GraphicsQuality quality = GraphicsQuality.fromArgs(args);
        boolean fullscreen = Arrays.asList(args).contains("--fullscreen");

        AppSettings settings = new AppSettings(true);
        settings.setTitle("Java Farm — учимся программировать на ферме");
        settings.setResolution(intArg(args, "--width=", 1440), intArg(args, "--height=", 810));
        settings.setFullscreen(fullscreen);
        settings.setSamples(quality.msaaSamples());
        settings.setVSync(true);
        settings.setGammaCorrection(true);
        // Звука пока нет: без аудио-рендерера игра запускается и на машинах без звуковой карты.
        settings.setAudioRenderer(null);

        FarmGameApp app = new FarmGameApp(quality);
        app.setSettings(settings);
        app.setShowSettings(false);    // без стартового диалога jME
        app.setPauseOnLostFocus(false);
        app.start();
    }

    private static int intArg(String[] args, String prefix, int fallback) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                try {
                    return Integer.parseInt(arg.substring(prefix.length()));
                } catch (NumberFormatException e) {
                    System.err.println("Некорректное значение " + arg + ", используется " + fallback);
                }
            }
        }
        return fallback;
    }
}
