package dev.seer.client;


import com.nstut.openui.api.UIComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Статичная иконка фиксированного размера. / A static icon of a fixed size.
 */
final class IconView extends UIComponent {
    private final int fixedWidth;
    private final int fixedHeight;
    private final BevelButton.Painter painter;

    IconView(int width, int height, BevelButton.Painter painter) {
        this.fixedWidth = width;
        this.fixedHeight = height;
        this.painter = painter;
    }

    @Override public int preferredWidth(Font font) { return fixedWidth; }
    @Override public int preferredHeight(Font font) { return fixedHeight; }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (visible) painter.paint(g, x, y, width, height, false);
    }
}