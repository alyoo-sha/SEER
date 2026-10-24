package dev.seer.client;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

/**
 * Текст с выравниванием и обрезкой по ширине (с многоточием).
 * Text with alignment and width truncation (with an ellipsis).
 */
final class TextLabel extends UIComponent {
    private final Supplier<String> text;
    private final int color;
    private final boolean center;

    TextLabel(String text, int color, boolean center) {
        this(() -> text, color, center);
    }

    TextLabel(Supplier<String> text, int color, boolean center) {
        this.text = text;
        this.color = color;
        this.center = center;
    }

    @Override public int preferredWidth(Font font) { return font.width(text.get()); }
    @Override public int preferredHeight(Font font) { return font.lineHeight; }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        String s = text.get();
        if (font.width(s) > width) {
            s = font.plainSubstrByWidth(s, Math.max(0, width - font.width("..."))) + "...";
        }
        int tx = center ? x + (width - font.width(s)) / 2 : x;
        g.drawString(font, s, tx, y + (height - font.lineHeight) / 2 + 1, color, true);
    }
}