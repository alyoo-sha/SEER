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
    private static final String[] STAR = {
            "...X...",
            "..XXX..",
            "XXXXXXX",
            ".XXXXX.",
            "..XXX..",
            ".XX.XX.",
            ".X...X."
    };
    private static final String[] MINI_STAR = {
            "..X..",
            ".XXX.",
            "XXXXX",
            ".XXX.",
            ".X.X."
    };
    private static final String[] TREE = {
            "...XXX...",
            "...XXX...",
            "....X....",
            ".XXXXXXX.",
            ".X.....X.",
            "XXX...XXX",
            "XXX...XXX"
    };

    /** Дерево крафта: корневой узел и два дочерних. / The crafting tree: a root node and two children. */
    static void tree(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        int ox = x + (w - 9) / 2;
        int oy = y + (h - 7) / 2;
        paint(g, TREE, ox + 1, oy + 1, SHADOW);
        paint(g, TREE, ox, oy, hover ? 0xFFFFFFFF : 0xFFC8CADA);
    }

    private static final int SHADOW = 0xFF202020;

    private Icons() {}

    private static void paint(GuiGraphics g, String[] bitmap, int ox, int oy, int color) {
        for (int row = 0; row < bitmap.length; row++) {
            for (int col = 0; col < bitmap[row].length(); col++) {
                if (bitmap[row].charAt(col) == 'X') g.fill(ox + col, oy + row, ox + col + 1, oy + row + 1, color);
            }
        }
    }

    static void gear(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        int ox = x + (w - 11) / 2;
        int oy = y + (h - 11) / 2;
        paint(g, GEAR, ox + 1, oy + 1, SHADOW); // тень / shadow
        paint(g, GEAR, ox, oy, hover ? 0xFFFFFFFF : 0xFFE0E0E0);
    }

    /** Кнопка избранного: при активном режиме подложка акцентного цвета и золотая звезда. */
    /** The favorites button: with the mode on it gets an accent backing and a gold star. */
    static void starButton(GuiGraphics g, int x, int y, int w, int h, boolean hover, boolean active) {
        if (active) g.fill(x + 2, y + 2, x + w - 2, y + h - 2, Palette.ACCENT);
        int ox = x + (w - 7) / 2;
        int oy = y + (h - 7) / 2;
        paint(g, STAR, ox + 1, oy + 1, SHADOW);
        paint(g, STAR, ox, oy, active ? 0xFFFFD34A : hover ? 0xFFFFFFFF : 0xFFC8CADA);
    }

    /** Золотая метка избранного поверх иконки предмета. / The gold favorite mark above an item icon. */
    static void favoriteMark(GuiGraphics g, int x, int y) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 200); // выше предмета, иначе он закроет метку / above the item, otherwise it hides the mark
        paint(g, MINI_STAR, x + 1, y + 1, 0xFF000000);
        paint(g, MINI_STAR, x, y, 0xFFFFD34A);
        g.pose().popPose();
    }

    static void arrowLeft(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        paintArrow(g, x + w / 2 - 2, y + h / 2, true, hover);
    }

    static void arrowRight(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        paintArrow(g, x + w / 2 - 2, y + h / 2, false, hover);
    }

    // Сначала вся тень, потом сам значок: иначе тень соседнего столбца затирает блик.
    // All the shadow first, then the icon: otherwise a neighbouring column's shadow covers the highlight.
    private static void paintArrow(GuiGraphics g, int ox, int cy, boolean left, boolean hover) {
        int color = hover ? 0xFFFFFFFF : 0xFFE0E0E0;
        for (int pass = 0; pass < 2; pass++) {
            int shift = pass == 0 ? 1 : 0;
            int tint = pass == 0 ? SHADOW : color;
            for (int i = 0; i < 5; i++) {
                int column = left ? ox + i : ox + (4 - i);
                g.fill(column + shift, cy - i + shift, column + shift + 1, cy + i + 1 + shift, tint);
            }
        }
    }

    /** Лупа 11x11: кольцо и ручка. / An 11x11 magnifier: a ring and a handle. */
    static void magnifier(GuiGraphics g, int x, int y, int w, int h, boolean hover) {
        int ox = x + (w - 11) / 2;
        int oy = y + (h - 11) / 2;
        paintMagnifier(g, ox + 1, oy + 1, SHADOW);
        paintMagnifier(g, ox, oy, 0xFFC8CADA);
    }

    private static void paintMagnifier(GuiGraphics g, int ox, int oy, int color) {
        for (int dy = 0; dy < 9; dy++) {
            for (int dx = 0; dx < 9; dx++) {
                int d = (dx - 4) * (dx - 4) + (dy - 4) * (dy - 4);
                if (d >= 9 && d <= 17) g.fill(ox + dx, oy + dy, ox + dx + 1, oy + dy + 1, color);
            }
        }
        for (int i = 0; i < 4; i++) g.fill(ox + 7 + i, oy + 7 + i, ox + 9 + i, oy + 9 + i, color);
    }
}