package dev.seer.client;


import com.nstut.openui.api.UIComponent;
import com.nstut.openui.controls.Card;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Рамка SEER с настоящей прозрачностью: подложка рисуется один раз на пиксель (без наложения слоёв)
 * и без тени под собой, поэтому сквозь неё виден мир.
 * A SEER frame with real transparency: the surface is drawn once per pixel (no overlapping layers)
 * and without a shadow beneath, so the world shows through.
 */
final class SeerFrame extends Card {
    private static final int RADIUS = 5;

    SeerFrame(UIComponent child) {
        super(child);
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        roundedFill(g, x, y, width, height, RADIUS, Palette.FRAME);
        roundedBorder(g, x, y, width, height, RADIUS, Palette.FRAME_EDGE);
        renderChildren(g, font, mx, my, pt);
    }

    // Смещение скругления в строке row. / The corner inset of the row.
    private static int inset(int radius, int row) {
        double dy = radius - row - 0.5D;
        return Math.max(0, (int) Math.ceil(radius - Math.sqrt(radius * radius - dy * dy)));
    }

    private static int clampRadius(int radius, int w, int h) {
        return Math.max(0, Math.min(radius, Math.min(w, h) / 2));
    }

    // Прямоугольники не пересекаются: средняя полоса, затем строки скруглений сверху и снизу.
    // The rectangles never overlap: the middle band, then the rounded rows at the top and the bottom.
    private static void roundedFill(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        int radius = clampRadius(r, w, h);
        g.fill(x, y + radius, x + w, y + h - radius, color);
        for (int row = 0; row < radius; row++) {
            int inset = inset(radius, row);
            g.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
            g.fill(x + inset, y + h - row - 1, x + w - inset, y + h - row, color);
        }
    }

    // Контур в один пиксель, тоже без пересечений. / A one pixel outline, also without overlaps.
    private static void roundedBorder(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w < 3 || h < 3) return;
        int radius = clampRadius(r, w, h);
        g.fill(x + radius, y, x + w - radius, y + 1, color);
        g.fill(x + radius, y + h - 1, x + w - radius, y + h, color);
        g.fill(x, y + radius, x + 1, y + h - radius, color);
        g.fill(x + w - 1, y + radius, x + w, y + h - radius, color);
        for (int row = 0; row < radius; row++) {
            int from = inset(radius, row);
            int to = Math.max(from + 1, row == 0 ? radius : inset(radius, row - 1));
            g.fill(x + from, y + row, x + to, y + row + 1, color);
            g.fill(x + w - to, y + row, x + w - from, y + row + 1, color);
            g.fill(x + from, y + h - row - 1, x + to, y + h - row, color);
            g.fill(x + w - to, y + h - row - 1, x + w - from, y + h - row, color);
        }
    }
}