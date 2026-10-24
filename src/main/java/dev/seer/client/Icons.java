package dev.seer.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Иконки, нарисованные пикселями. / Icons drawn with pixels.
 */
final class Icons {
    private static final String[] GEAR = {
            "....XXX....",
            ".XX.XXX.XX.",
            ".XXXXXXXXX.",
            "..XXX.XXX..",
            "XXXX...XXXX",
            "XXX.....XXX",
            "XXXX...XXXX",
            "..XXX.XXX..",
            ".XXXXXXXXX.",
            ".XX.XXX.XX.",
            "....XXX...."
    };

    private Icons() {}

    static void gear(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        int ox = x + (w - 11) / 2;
        int oy = y + (h - 11) / 2;
        paintGear(g, ox + 1, oy + 1, 0xFF202020); // тень / shadow
        paintGear(g, ox, oy, hover ? 0xFFFFFFFF : 0xFFE0E0E0);
    }

    private static void paintGear(GuiGraphics g, int ox, int oy, int color) {
        for (int row = 0; row < GEAR.length; row++) {
            for (int col = 0; col < GEAR[row].length(); col++) {
                if (GEAR[row].charAt(col) == 'X') g.fill(ox + col, oy + row, ox + col + 1, oy + row + 1, color);
            }
        }
    }

    static void arrowLeft(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        paintArrow(g, x + w / 2 - 2, y + h / 2, true, hover);
    }

    static void arrowRight(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        paintArrow(g, x + w / 2 - 2, y + h / 2, false, hover);
    }

    private static void paintArrow(GuiGraphics g, int ox, int cy, boolean left, boolean hover) {
        int color = hover ? 0xFFFFFFFF : 0xFFE0E0E0;
        for (int i = 0; i < 5; i++) {
            int column = left ? ox + i : ox + (4 - i);
            g.fill(column + 1, cy - i + 1, column + 2, cy + i + 2, 0xFF202020); // тень / shadow
            g.fill(column, cy - i, column + 1, cy + i + 1, color);
        }
    }
}
