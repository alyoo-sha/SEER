package dev.seer.client;

import com.nstut.openui.api.UIComponent;
import dev.seer.core.SeerIngredient;
import dev.seer.core.SeerStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Ячейка со стеком: иконка, подсветка при наведении, необязательный фон слота.
 * A stack cell: icon, hover highlight, optional slot background.
 */
final class StackCell extends UIComponent {
    private static final int SIZE = 18;

    private final Supplier<SeerStack> source;
    private final Consumer<SeerStack> hoverSink;
    private boolean slotBackground = false;

    StackCell(SeerStack stack, Consumer<SeerStack> hoverSink) {
        this(() -> stack, hoverSink);
    }

    StackCell(Supplier<SeerStack> source, Consumer<SeerStack> hoverSink) {
        this.source = source;
        this.hoverSink = hoverSink;
    }

    /** Источник, который раз в секунду меняет вариант ингредиента (как теги в EMI). */
    /** A source that switches the ingredient alternative every second (like tags in EMI). */
    static Supplier<SeerStack> cycling(SeerIngredient ingredient) {
        List<SeerStack> alternatives = ingredient.alternatives();
        return () -> alternatives.isEmpty()
                ? null
                : alternatives.get((int) ((System.currentTimeMillis() / 1000) % alternatives.size()));
    }

    StackCell slot() {
        slotBackground = true;
        return this;
    }

    @Override public int preferredWidth(Font font) { return SIZE; }
    @Override public int preferredHeight(Font font) { return SIZE; }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        if (slotBackground) {
            g.fill(x, y, x + width, y + height, 0x66000000);
            g.renderOutline(x, y, width, height, 0x33FFFFFF);
        }

        SeerStack stack = source.get();
        if (stack == null) return;

        int sx = x + (width - 16) / 2;
        int sy = y + (height - 16) / 2;
        StackRenderer.render(g, stack, sx, sy);
        if (mx >= x && mx < x + width && my >= y && my < y + height) {
            g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
            hoverSink.accept(stack);
        }
    }
}
