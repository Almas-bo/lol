package com.farmgame.sandbox.compiler;

import java.util.List;
import java.util.Map;

/**
 * Загрузчик классов для кода игрока.
 *
 * <p>Классы игрока определяются из байт-кода, полученного от компилятора, а все прочие
 * классы загружаются только если входят в белый список пакетов. Так код игрока не может
 * напрямую обратиться к внутренностям игры ({@code core.farm.Farm}, движок) или к опасным
 * API ({@code java.io}, {@code java.lang.reflect}, {@code Runtime}...).
 *
 * <p><b>Важно:</b> это учебный барьер, а не полноценная граница безопасности.
 */
public final class SandboxClassLoader extends ClassLoader {

    /** Пакеты (префиксы имён классов), разрешённые коду игрока. */
    static final List<String> ALLOWED_PREFIXES = List.of(
            "java.lang.",
            "java.util.",
            "com.farmgame.sandbox.api.",
            "com.farmgame.core.crop.");

    /** Исключения из белого списка. */
    static final List<String> DENIED_PREFIXES = List.of(
            "java.lang.reflect.",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder",
            "java.lang.Process",
            "java.lang.Thread",
            "java.lang.ClassLoader",
            "java.lang.System",
            "java.util.concurrent.");

    /**
     * Классы {@code java.lang.invoke}, без которых не работают лямбды и конкатенация строк:
     * JVM разрешает их через загрузчик кода игрока при связывании {@code invokedynamic}.
     */
    static final List<String> INVOKEDYNAMIC_SUPPORT = List.of(
            "java.lang.invoke.StringConcatFactory",
            "java.lang.invoke.LambdaMetafactory",
            "java.lang.invoke.MethodHandles$Lookup",
            "java.lang.invoke.MethodHandle",
            "java.lang.invoke.MethodType",
            "java.lang.invoke.CallSite");

    private final Map<String, byte[]> playerClasses;

    SandboxClassLoader(Map<String, byte[]> playerClasses, ClassLoader parent) {
        super("farm-sandbox", parent);
        this.playerClasses = Map.copyOf(playerClasses);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                if (playerClasses.containsKey(name)) {
                    loaded = findClass(name);
                } else if (isAllowed(name)) {
                    loaded = getParent().loadClass(name);
                } else {
                    throw new ClassNotFoundException("Класс " + name + " недоступен в песочнице");
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = playerClasses.get(name);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    static boolean isAllowed(String className) {
        if (INVOKEDYNAMIC_SUPPORT.contains(className)) {
            return true;
        }
        if (className.startsWith("java.lang.invoke.")) {
            return false;
        }
        for (String denied : DENIED_PREFIXES) {
            if (className.startsWith(denied)) {
                return false;
            }
        }
        for (String allowed : ALLOWED_PREFIXES) {
            if (className.startsWith(allowed)) {
                return true;
            }
        }
        return false;
    }
}
