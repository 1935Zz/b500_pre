package render;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class FontHandler {

    private Font font;
    private Map<Character, Glyph> glyphs = new HashMap<>();
    private int fontHeight = 0;
    public Texture fontTexture;

    public FontHandler(Font font) {
        this.font = font;

        int texW = 0;

        Map<Character, BufferedImage> ims = new HashMap<>();
        for(int i = 32; i < 256; i++) {
            if(i == 127) { // control char
                continue;
            }
            BufferedImage chim = imageFromChar(font, (char) i, true);
            texW += chim.getWidth();
            fontHeight = Math.max(fontHeight, chim.getHeight());
            ims.put((char) i, chim);
        }

        BufferedImage fullTexture = new BufferedImage(texW, fontHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fullTexture.createGraphics();

        int currW = 0;
        // must do two loops because it is necessary to know the max char size before setting any glyph height
        for(int i = 32; i < 256; i++) {
            if(i == 127) { // control char
                continue;
            }
            BufferedImage chim = ims.get((char) i);
            Glyph gl = new Glyph(chim.getWidth(), chim.getHeight(), currW, fontHeight - chim.getHeight());
            glyphs.put((char) i, gl);
            currW += chim.getWidth();

            g.drawImage(chim, gl.x, 0, null);
        }
        fontTexture = fontTextureFromImage(fullTexture);
    }

    private Texture fontTextureFromImage(BufferedImage image) {
        /* Flip image Horizontal to get the origin to bottom left */
        AffineTransform transform = AffineTransform.getScaleInstance(1f, -1f);
        transform.translate(0, -image.getHeight());
        AffineTransformOp operation = new AffineTransformOp(transform,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        image = operation.filter(image, null);

        /* Get charWidth and charHeight of image */
        int width = image.getWidth();
        int height = image.getHeight();

        /* Get pixel data of image */
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        /* Put pixel data into a ByteBuffer */
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                /* Pixel as RGBA: 0xAARRGGBB */
                int pixel = pixels[i * width + j];
                /* Red component 0xAARRGGBB >> 16 = 0x0000AARR */
                buffer.put((byte) ((pixel >> 16) & 0xFF));
                /* Green component 0xAARRGGBB >> 8 = 0x00AARRGG */
                buffer.put((byte) ((pixel >> 8) & 0xFF));
                /* Blue component 0xAARRGGBB >> 0 = 0xAARRGGBB */
                buffer.put((byte) (pixel & 0xFF));
                /* Alpha component 0xAARRGGBB >> 24 = 0x000000AA */
                buffer.put((byte) ((pixel >> 24) & 0xFF));
            }
        }
        buffer.flip();

        System.out.println("Font texture is " + width +  " by " + height);

        Texture fontTexture = new Texture();
        fontTexture.generate(width, height, buffer);

        MemoryUtil.memFree(buffer);
        return fontTexture;
    }

    private BufferedImage imageFromChar(Font f, char c, boolean antialias) {
        BufferedImage im = new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();
        if (antialias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.dispose();
        int w = metrics.charWidth(c);
        int h = metrics.getHeight();
        im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        g = im.createGraphics();
        if (antialias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setFont(font);
        g.setPaint(Color.BLACK);
        g.drawString(String.valueOf(c), 0, metrics.getAscent());
        g.dispose();
        return im;
    }

    public void renderString(SpriteRenderer sr, String text, float x, float y, float scale, Matrix4f transform, boolean centre) {
        scale *= 200;
        int lines = 1;
        int currW = 0;
        int maxW = 0;
        for(int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if(c == '\n') {
                lines++;
                currW = 0;
            } else {
                currW += glyphs.get(c).w;
                maxW = Math.max(currW, maxW);
            }
        }
        int textHeight = lines * fontHeight;

        float initDrawX = x;
        if(centre) {
            initDrawX -= maxW / 2f / scale;
        }
        float drawY = y;
//        if(textHeight > fontHeight) {
//            drawY += (textHeight - fontHeight) / scale;
//        }

        sr.setBatchTransformer(transform);
        fontTexture.bind();
        sr.startBatch();
        float drawX = initDrawX;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') {
                drawY -= fontHeight/scale;
                drawX = initDrawX;
                continue;
            }
            if (ch == '\r') {
                /* Carriage return, just skip it */
                continue;
            }
            Glyph g = glyphs.get(ch);
            sr.drawRegion(fontTexture, new Vector2f(drawX, drawY), new Vector2f(g.x, g.y), new Vector2f(g.w, g.h), scale);
            drawX += g.w / scale;
        }
        sr.finishBatch();
    }

    private record Glyph(int w, int h, int x, int y) {}

}
