package com.farmgame.engine;

import java.util.Locale;

/**
 * Уровень качества графики. Выбирается аргументом запуска {@code --quality=low|medium|high}.
 *
 * <p>На слабых ноутбуках и встроенной графике стоит использовать {@link #LOW}.
 */
public enum GraphicsQuality {
    /** Без теней и пост-эффектов, без сглаживания. */
    LOW(0, 0, false, false),
    /** Тени среднего разрешения и FXAA. */
    MEDIUM(2, 1024, true, false),
    /** Мягкие тени высокого разрешения, SSAO, bloom, MSAA x4. */
    HIGH(4, 2048, true, true);

    private final int msaaSamples;
    private final int shadowMapSize;
    private final boolean postEffects;
    private final boolean ambientOcclusion;

    GraphicsQuality(int msaaSamples, int shadowMapSize, boolean postEffects, boolean ambientOcclusion) {
        this.msaaSamples = msaaSamples;
        this.shadowMapSize = shadowMapSize;
        this.postEffects = postEffects;
        this.ambientOcclusion = ambientOcclusion;
    }

    public int msaaSamples() {
        return msaaSamples;
    }

    /** Размер карты теней; 0 — тени выключены. */
    public int shadowMapSize() {
        return shadowMapSize;
    }

    public boolean shadows() {
        return shadowMapSize > 0;
    }

    public boolean postEffects() {
        return postEffects;
    }

    public boolean ambientOcclusion() {
        return ambientOcclusion;
    }

    /**
     * Ищет {@code --quality=...} среди аргументов командной строки.
     *
     * @return найденное значение или {@link #HIGH} по умолчанию
     */
    public static GraphicsQuality fromArgs(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--quality=")) {
                String value = arg.substring("--quality=".length()).toUpperCase(Locale.ROOT);
                try {
                    return valueOf(value);
                } catch (IllegalArgumentException e) {
                    System.err.println("Неизвестное качество '" + value + "', используется HIGH. "
                            + "Допустимо: low, medium, high");
                }
            }
        }
        return HIGH;
    }
}
