package com.farmgame.engine.ui;

import com.jme3.asset.AssetManager;
import com.jme3.scene.Spatial;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.image.ColorSpace;
import com.jme3.ui.Picture;
import com.jme3.util.BufferUtils;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Панель интерфейса, которая рисуется средствами Java2D и показывается как текстура.
 *
 * <p>Зачем: стандартный растровый шрифт jME содержит только латиницу, а Java2D использует
 * системные шрифты — с кириллицей, сглаживанием и любыми размерами. Перерисовка
 * выполняется только когда содержимое изменилось.
 */
public final class HudPanel {

    private final int width;
    private final int height;
    private final BufferedImage canvas;
    private final ByteBuffer pixels;
    private final Image image;
    private final Picture picture;
    private String lastKey;

    public HudPanel(String name, AssetManager assetManager, int width, int height) {
        this.width = width;
        this.height = height;
        this.canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        this.pixels = BufferUtils.createByteBuffer(width * height * 4);
        this.image = new Image(Image.Format.RGBA8, width, height, pixels, ColorSpace.sRGB);
        Texture2D texture = new Texture2D(image);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        this.picture = new Picture(name);
        picture.setTexture(assetManager, texture, true);
        picture.setWidth(width);
        picture.setHeight(height);
    }

    /** Перерисовать при следующем {@link #redraw}, даже если ключ содержимого не изменился. */
    public void invalidate() {
        lastKey = null;
    }

    /**
     * Попадает ли точка экрана (в пикселях, от левого нижнего угла) на панель.
     * Возвращает координаты внутри панели от ЛЕВОГО ВЕРХНЕГО угла или {@code null}.
     */
    public int[] toLocal(float screenX, float screenY) {
        if (!isVisible()) {
            return null;
        }
        float px = picture.getWorldTranslation().x;
        float py = picture.getWorldTranslation().y;
        int lx = (int) (screenX - px);
        int ly = (int) (height - (screenY - py));
        return lx >= 0 && ly >= 0 && lx < width && ly < height ? new int[]{lx, ly} : null;
    }

    /** Панель видна: она прикреплена к сцене и ни она, ни её родители не скрыты. */
    public boolean isVisible() {
        if (picture.getParent() == null) {
            return false;
        }
        for (Spatial s = picture; s != null; s = s.getParent()) {
            if (s.getCullHint() == Spatial.CullHint.Always) {
                return false;
            }
        }
        return true;
    }

    /** Узел для {@code guiNode}. Позиция задаётся в пикселях от левого нижнего угла экрана. */
    public Picture picture() {
        return picture;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /**
     * Перерисовывает панель, если {@code contentKey} отличается от предыдущего.
     *
     * @param contentKey строка, однозначно описывающая содержимое (для пропуска лишних перерисовок)
     * @param painter    код рисования на чистом прозрачном холсте
     */
    public void redraw(String contentKey, Consumer<Graphics2D> painter) {
        if (contentKey.equals(lastKey)) {
            return;
        }
        lastKey = contentKey;
        Graphics2D g = canvas.createGraphics();
        try {
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(0, 0, width, height);
            g.setComposite(AlphaComposite.SrcOver);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            painter.accept(g);
        } finally {
            g.dispose();
        }
        upload();
    }

    /** Копирует ARGB-пиксели Java2D в RGBA-буфер jME (с переворотом по вертикали). */
    private void upload() {
        int[] argbPixels = ((DataBufferInt) canvas.getRaster().getDataBuffer()).getData();
        pixels.clear();
        for (int y = height - 1; y >= 0; y--) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                int argb = argbPixels[row + x];
                pixels.put((byte) (argb >> 16)).put((byte) (argb >> 8)).put((byte) argb).put((byte) (argb >>> 24));
            }
        }
        pixels.flip();
        image.setData(pixels);
        image.setUpdateNeeded();
    }
}
