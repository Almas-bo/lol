package com.farmgame.engine.scene;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Текстуры, сгенерированные кодом (шум), — без файлов картинок.
 * Бесшовные: их можно повторять на большой поверхности.
 */
public final class ProceduralTextures {

    private ProceduralTextures() {
    }

    /** Трава: пятна светлой и тёмной зелени с «травинками». */
    public static Texture2D grass(int size) {
        float[][] low = tileableNoise(size, 8, 1);
        float[][] mid = tileableNoise(size, 32, 2);
        Random random = new Random(3);
        ByteBuffer data = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float n = low[x][y] * 0.6f + mid[x][y] * 0.4f;
                float blade = random.nextFloat() < 0.06f ? 0.08f : 0f;
                float r = 0.30f + 0.10f * n + blade * 0.5f;
                float g = 0.52f + 0.16f * n + blade;
                float b = 0.20f + 0.05f * n;
                put(data, r, g, b);
            }
        }
        return texture(data, size);
    }

    /** Почва грядок: коричневая с мелкими камушками и комочками. */
    public static Texture2D soil(int size) {
        float[][] low = tileableNoise(size, 6, 4);
        float[][] fine = tileableNoise(size, 48, 5);
        Random random = new Random(7);
        ByteBuffer data = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float n = low[x][y] * 0.5f + fine[x][y] * 0.5f;
                float pebble = random.nextFloat() < 0.012f ? 0.25f : 0f;
                float v = 0.82f + 0.30f * n + pebble;
                put(data, v, v, v); // оттенок задаёт цвет материала (сухая/мокрая почва)
            }
        }
        return texture(data, size);
    }

    private static void put(ByteBuffer data, float r, float g, float b) {
        data.put(toByte(r)).put(toByte(g)).put(toByte(b)).put((byte) 255);
    }

    private static byte toByte(float v) {
        return (byte) Math.round(Math.max(0f, Math.min(1f, v)) * 255f);
    }

    private static Texture2D texture(ByteBuffer data, int size) {
        data.flip();
        Texture2D texture = new Texture2D(new Image(Image.Format.RGBA8, size, size, data, ColorSpace.sRGB));
        texture.setWrap(Texture.WrapMode.Repeat);
        texture.setMinFilter(Texture.MinFilter.Trilinear);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setAnisotropicFilter(8);
        return texture;
    }

    /**
     * Бесшовный «value noise»: случайные значения на решётке {@code cells × cells},
     * плавно интерполированные; решётка замыкается по краям.
     *
     * @return значения в диапазоне -1..1
     */
    static float[][] tileableNoise(int size, int cells, long seed) {
        Random random = new Random(seed);
        float[][] lattice = new float[cells][cells];
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                lattice[i][j] = random.nextFloat() * 2f - 1f;
            }
        }
        float[][] out = new float[size][size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float fx = (float) x * cells / size;
                float fy = (float) y * cells / size;
                int x0 = (int) fx;
                int y0 = (int) fy;
                float tx = smooth(fx - x0);
                float ty = smooth(fy - y0);
                int x1 = (x0 + 1) % cells;
                int y1 = (y0 + 1) % cells;
                float top = lerp(lattice[x0][y0], lattice[x1][y0], tx);
                float bottom = lerp(lattice[x0][y1], lattice[x1][y1], tx);
                out[x][y] = lerp(top, bottom, ty);
            }
        }
        return out;
    }

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
