package dev.seer.client;

import com.nstut.openui.api.UIComponent;
import dev.seer.core.SeerStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Вкладка с иконкой станка. ЛКМ выбирает категорию, при наведении показывается её название.
 * A tab with a workstation icon. LMB selects the category, hovering shows its name.
 */
final class IconTab extends UIComponent {
    private static final int SIZE = 24;
    private static final int COLOR_SELECTED = 0xCC6B5BD6;
    private static final int COLOR_IDLE = 0x40000000;
    private static final int COLOR_HOVER = 0x40FFFFFF;

    private final SeerStack icon;
    private final Component label;
    private final boolean selected;
    private final Runnable action;
    private final Consumer<Component> hoverSink;

    IconTab(SeerStack icon, Component label, boolean selected, Runnable action, Consumer<Component> hoverSink) {
        this.icon = icon;
        this.label = label;
        this.selected = selected;
        this.action = action;
        this.hoverSink = hoverSink;
    }

    @Override public int preferredWidth(Font font) { return SIZE; }
    @Override public int preferredHeight(Font font) { return SIZE; }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        boolean over = mx >= x && mx < x + width && my >= y && my < y + height;

        g.fill(x, y, x + width, y + height, selected ? COLOR_SELECTED : COLOR_IDLE);
        if (over) g.fill(x, y, x + width, y + height, COLOR_HOVER);
        if (selected) g.renderOutline(x, y, width, height, 0xFFFFFFFF);

        StackRenderer.render(g, icon, x + (width - 16) / 2, y + (height - 16) / 2);
        if (over) hoverSink.accept(label);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        action.run();
        return true;
    }
}
