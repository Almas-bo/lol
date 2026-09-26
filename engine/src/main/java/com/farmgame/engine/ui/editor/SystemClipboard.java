package com.farmgame.engine.ui.editor;

import com.jme3.app.Application;
import com.jme3.system.lwjgl.LwjglWindow;
import org.lwjgl.glfw.GLFW;

/**
 * Системный буфер обмена через GLFW (работает в Windows, macOS и Linux).
 * Если окно не на LWJGL3 (например, в тестах), используется внутренний буфер игры.
 * Вызывать только из потока рендеринга.
 */
public final class SystemClipboard {

    private final Application app;
    private String fallback = "";

    public SystemClipboard(Application app) {
        this.app = app;
    }

    public String get() {
        if (app.getContext() instanceof LwjglWindow window) {
            String text = GLFW.glfwGetClipboardString(window.getWindowHandle());
            return text != null ? text : "";
        }
        return fallback;
    }

    public void set(String text) {
        fallback = text;
        if (app.getContext() instanceof LwjglWindow window) {
            GLFW.glfwSetClipboardString(window.getWindowHandle(), text);
        }
    }
}
