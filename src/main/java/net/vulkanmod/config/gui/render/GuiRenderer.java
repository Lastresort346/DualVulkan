package net.vulkanmod.config.gui.render;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.List;

public abstract class GuiRenderer {

    public static Minecraft minecraft;
    public static GuiGraphics guiGraphics;
    public static PoseStack pose;
    public static BufferBuilder bufferBuilder;

    public static void enableScissor(int i, int j, int k, int l) {
        guiGraphics.enableScissor(i, j, k, l);
    }

    public static void disableScissor() {
        guiGraphics.disableScissor();
    }

    public static void fillBox(int x0, int y0, int width, int height, int color) {
        fill(x0, y0, x0 + width, y0 + height, 0, color);
    }

    public static void fill(int x0, int y0, int x1, int y1, int color) {
        fill(x0, y0, x1, y1, 0, color);
    }

    public static void fill(int x0, int y0, int x1, int y1, @SuppressWarnings("unused") int z, int color) {
        guiGraphics.fill(x0, y0, x1, y1, color);
    }

    @SuppressWarnings("unused")
    public static void fillGradient(int x0, int y0, int x1, int y1, int color1, int color2) {
        fillGradient(x0, y0, x1, y1, 0, color1, color2);
    }

    public static void fillGradient(int x0, int y0, int x1, int y1, @SuppressWarnings("unused") int z, int color1, int color2) {
        guiGraphics.fillGradient(x0, y0, x1, y1, color1, color2);
    }

    public static void renderBoxBorder(int x0, int y0, int width, int height, int borderWidth, int color) {
        renderBorder(x0, y0, x0 + width, y0 + height, borderWidth, color);
    }

    public static void renderBorder(int x0, int y0, int x1, int y1, int width, int color) {
        GuiRenderer.fill(x0, y0, x1, y0 + width, color);
        GuiRenderer.fill(x0, y1 - width, x1, y1, color);

        GuiRenderer.fill(x0, y0 + width, x0 + width, y1 - width, color);
        GuiRenderer.fill(x1 - width, y0 + width, x1, y1 - width, color);
    }

    public static void drawString(Font font, Component component, int x, int y, int color) {
        drawString(font, component.getVisualOrderText(), x, y, color);
    }

    public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color) {
        guiGraphics.drawString(font, formattedCharSequence, x, y, color);
    }

    public static void drawString(Font font, Component component, int x, int y, int color, boolean shadow) {
        drawString(font, component.getVisualOrderText(), x, y, color, shadow);
    }

    public static void drawString(Font font, FormattedCharSequence formattedCharSequence, int x, int y, int color, boolean shadow) {
        guiGraphics.drawString(font, formattedCharSequence, x, y, color, shadow);
    }

    public static void drawCenteredString(Font font, Component component, int x, int y, int color) {
        FormattedCharSequence formattedCharSequence = component.getVisualOrderText();
        guiGraphics.drawString(font, formattedCharSequence, x - font.width(formattedCharSequence) / 2, y, color);
    }

    public static void drawScrollingString(Font font, Component component, int x, int y, int maxWidth, int color) {
        int textWidth = font.width(component);
        if (textWidth <= maxWidth) {
            drawCenteredString(font, component, x, y, color);
        } else {
            int x0 = x - maxWidth / 2, x1 = x + maxWidth / 2;
            int scrollAmount = textWidth - maxWidth;
            double currentTimeInSeconds = (double) Util.getMillis() / 1000.0;
            double scrollSpeed = Math.max(scrollAmount * 0.5, 3.0);
            double scrollingOffset = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * currentTimeInSeconds / scrollSpeed)) / 2.0 + 0.5;
            double horizontalScroll = Mth.lerp(scrollingOffset, 0.0, scrollAmount);

            enableScissor(x0 - 1, 0, x1, Minecraft.getInstance().getWindow().getScreenHeight());
            drawString(font, component, (int) (x0 - horizontalScroll), y, color);
            disableScissor();
        }
    }

    public static int getMaxTextWidth(Font font, List<FormattedCharSequence> list) {
        int maxWidth = 0;
        for (var text : list) {
            int width = font.width(text);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        return maxWidth;
    }

    /**
     * Render a filled polygon by decomposing it into triangles drawn via GuiGraphics.fill().
     * In 1.21.1, there is no guiRenderState/TextureSetup/GuiElementRenderState API.
     */
    public static void submitPolygon(float[][] vertices, int color) {
        if (vertices.length < 3) return;
        // Fan triangulate: draw as a series of filled quads if quad-shaped, otherwise fan from [0]
        for (int i = 1; i + 1 < vertices.length; i++) {
            float x0 = vertices[0][0], y0 = vertices[0][1];
            float x1 = vertices[i][0], y1 = vertices[i][1];
            float x2 = vertices[i + 1][0], y2 = vertices[i + 1][1];
            int minX = (int) Math.min(x0, Math.min(x1, x2));
            int minY = (int) Math.min(y0, Math.min(y1, y2));
            int maxX = (int) Math.ceil(Math.max(x0, Math.max(x1, x2)));
            int maxY = (int) Math.ceil(Math.max(y0, Math.max(y1, y2)));
            guiGraphics.fill(minX, minY, maxX, maxY, color);
        }
    }
}
