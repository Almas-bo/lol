package com.farmgame.sandbox.compiler;

import com.farmgame.sandbox.api.FarmProgram;

import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Компилирует исходный код игрока прямо в памяти (без временных файлов) и создаёт
 * экземпляр {@link FarmProgram}.
 *
 * <pre>{@code
 * CompilationResult result = new PlayerCodeCompiler().compile(sourceCode);
 * switch (result) {
 *     case CompilationResult.Success s -> runner.start(s.program(), robot, farm);
 *     case CompilationResult.Failure f -> console.print(f.summary());
 * }
 * }</pre>
 *
 * <p>Требуется запуск на JDK (не JRE), так как используется {@code javax.tools}.
 */
public final class PlayerCodeCompiler {

    private static final Pattern PUBLIC_CLASS =
            Pattern.compile("public\\s+(?:final\\s+)?class\\s+([A-Za-z_$][A-Za-z0-9_$]*)");
    private static final Pattern PACKAGE =
            Pattern.compile("^\\s*package\\s+([A-Za-z0-9_.]+)\\s*;", Pattern.MULTILINE);

    private final JavaCompiler compiler;

    public PlayerCodeCompiler() {
        this.compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Компилятор Java недоступен: запустите игру на JDK, а не на JRE");
        }
    }

    /**
     * Компилирует исходник, содержащий публичный класс, реализующий {@link FarmProgram}.
     *
     * @param source полный текст .java-файла
     */
    public CompilationResult compile(String source) {
        Matcher classMatcher = PUBLIC_CLASS.matcher(source);
        if (!classMatcher.find()) {
            return failure("Не найден публичный класс: объявите 'public class MyProgram implements FarmProgram'");
        }
        String simpleName = classMatcher.group(1);
        Matcher packageMatcher = PACKAGE.matcher(source);
        String className = packageMatcher.find() ? packageMatcher.group(1) + "." + simpleName : simpleName;

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, Locale.getDefault(),
                StandardCharsets.UTF_8);
        InMemoryFileManager fileManager = new InMemoryFileManager(standard);

        List<String> options = List.of(
                "-classpath", System.getProperty("java.class.path"),
                "-proc:none",
                "-Xlint:none");
        JavaFileObject unit = new SourceFile(className, source);

        boolean ok = compiler.getTask(null, fileManager, diagnostics, options, null, List.of(unit)).call();
        if (!ok) {
            List<CompilationResult.Diagnostic> errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR)
                    .map(d -> new CompilationResult.Diagnostic(d.getLineNumber(), d.getMessage(Locale.getDefault())))
                    .toList();
            return new CompilationResult.Failure(errors);
        }
        return instantiate(className, fileManager.classBytes());
    }

    private CompilationResult instantiate(String className, Map<String, byte[]> classes) {
        SandboxClassLoader loader = new SandboxClassLoader(classes, FarmProgram.class.getClassLoader());
        try {
            Class<?> type = loader.loadClass(className);
            if (!FarmProgram.class.isAssignableFrom(type)) {
                return failure("Класс " + className + " должен реализовывать интерфейс FarmProgram");
            }
            Object instance = type.getDeclaredConstructor().newInstance();
            return new CompilationResult.Success((FarmProgram) instance);
        } catch (NoSuchMethodException e) {
            return failure("У класса " + className + " должен быть публичный конструктор без параметров");
        } catch (InvocationTargetException e) {
            return failure("Ошибка при создании программы: " + describe(e.getCause()));
        } catch (ReflectiveOperationException | LinkageError e) {
            return failure("Не удалось загрузить программу: " + describe(e));
        }
    }

    /** Находит самую информативную причину: для запрещённого класса это сообщение песочницы. */
    private static String describe(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root instanceof ClassNotFoundException ? root.getMessage() : String.valueOf(error);
    }

    private static CompilationResult failure(String message) {
        return new CompilationResult.Failure(List.of(new CompilationResult.Diagnostic(-1, message)));
    }

    /** Исходный код из строки. */
    private static final class SourceFile extends SimpleJavaFileObject {
        private final String code;

        SourceFile(String className, String code) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

    /** Байт-код, который компилятор пишет в память. */
    private static final class ClassFile extends SimpleJavaFileObject {
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        ClassFile(String className) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.CLASS.extension), Kind.CLASS);
        }

        @Override
        public OutputStream openOutputStream() {
            return bytes;
        }
    }

    /** Перехватывает вывод .class-файлов и складывает их в память. */
    private static final class InMemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, ClassFile> outputs = new HashMap<>();

        InMemoryFileManager(JavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
                                                   FileObject sibling) {
            ClassFile file = new ClassFile(className);
            outputs.put(className, file);
            return file;
        }

        Map<String, byte[]> classBytes() {
            Map<String, byte[]> result = new HashMap<>();
            outputs.forEach((name, file) -> result.put(name, file.bytes.toByteArray()));
            return result;
        }
    }
}
