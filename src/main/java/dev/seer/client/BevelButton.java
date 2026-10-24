package dev.seer.client;

import com.nstut.openui.api.UIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Слегка объёмная кнопка в палитре SEER. Размер фиксированный, внутри рисует Painter.
 * A slightly 3D button in the SEER palette. Fixed size, the Painter draws the content.
 */
final class BevelButton extends UIComponent {
    interface Painter {
        void paint(GuiGraphics g, int x, int y, int w, int h, boolean hover);
    }

    private final int fixedWidth;
    private final int fixedHeight;
    private final Painter painter;
    private final Runnable action;

    BevelButton(int width, int height, Painter painter, Runnable action) {
        this.fixedWidth = width;
        this.fixedHeight = height;
        this.painter = painter;
        this.action = action;
    }

    static BevelButton text(String label, int width, int height, Runnable action) {
        return new BevelButton(width, height, (g, x, y, w, h, hover) ->
                g.drawCenteredString(Minecraft.getInstance().font, label, x + w / 2, y + (h - 8) / 2, Palette.TEXT), action);
    }

    static void playClick() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override public int preferredWidth(Font font) { return fixedWidth; }
    @Override public int preferredHeight(Font font) { return fixedHeight; }

    private boolean over(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        boolean hover = over(mx, my);
        g.fill(x, y, x + width, y + height, Palette.BUTTON_OUTLINE);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, hover ? Palette.BUTTON_HOVER : Palette.BUTTON_FACE);
        g.fill(x + 1, y + 1, x + width - 1, y + 2, Palette.BUTTON_LIGHT);                    // блик сверху / top highlight
        g.fill(x + 1, y + 1, x + 2, y + height - 1, Palette.BUTTON_LIGHT);                   // блик слева / left highlight
        g.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, Palette.BUTTON_DARK);   // тень снизу / bottom shadow
        g.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, Palette.BUTTON_DARK);    // тень справа / right shadow
        painter.paint(g, x, y, width, height, hover);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !over(mouseX, mouseY)) return false;
        playClick();
        action.run();
        return true;
    }
}