package dev.seer.client;

import com.nstut.openui.api.UIComponent;
import dev.seer.core.SeerCategory;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Вкладки категорий (иконки станков). Состояние читается из поставщиков, интерфейс не пересоздаётся.
 * Category tabs (workstation icons). State is read from suppliers, the UI is never rebuilt.
 * ЛКМ по другой вкладке - выбрать; ЛКМ по выбранной или ПКМ - все рецепты станка.
 * LMB on another tab - select; LMB on the selected one or RMB - all recipes of the machine.
 */
final class TabsBar extends UIComponent {
    private static final int MAX_TAB = 24;
    private static final int GAP = 4;

    private final int maxWidth;
    private final Supplier<List<SeerCategory>> categories;
    private final IntSupplier selected;
    private final IntConsumer onSelect;
    private final IntConsumer onMachine;
    private final Consumer<List<Component>> hoverSink;

    TabsBar(int maxWidth, Supplier<List<SeerCategory>> categories, IntSupplier selected,
            IntConsumer onSelect, IntConsumer onMachine, Consumer<List<Component>> hoverSink) {
        this.maxWidth = maxWidth;
        this.categories = categories;
        this.selected = selected;
        this.onSelect = onSelect;
        this.onMachine = onMachine;
        this.hoverSink = hoverSink;
    }

    /** Список вкладок изменился: пересчитать ширину. / The tab list changed: recompute the width. */
    void refresh() {
        invalidateLayout();
    }

    @Override
    public int preferredWidth(Font font) {
        int count = categories.get().size();
        return count == 0 ? 0 : Math.min(maxWidth, count * (MAX_TAB + GAP) - GAP);
    }

    @Override public int preferredHeight(Font font) { return MAX_TAB; }

    private int tabSize(int count) {
        if (count == 0) return MAX_TAB;
        return Math.max(14, Math.min(MAX_TAB, (width + GAP) / count - GAP));
    }

    private int indexAt(double mx, double my) {
        int count = categories.get().size();
        if (count == 0) return -1;
        int size = tabSize(count);
        if (mx < x || my < y || my >= y + size) return -1;
        int index = (int) ((mx - x) / (size + GAP));
        return index < count && mx < x + index * (size + GAP) + size ? index : -1;
    }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        List<SeerCategory> list = categories.get();
        int size = tabSize(list.size());
        int current = selected.getAsInt();
        int hovered = indexAt(mx, my);

        for (int i = 0; i < list.size(); i++) {
            int tx = x + i * (size + GAP);
            boolean isSelected = i == current;
            g.fill(tx, y, tx + size, y + size, isSelected ? Palette.ACCENT : Palette.SLOT);
            if (i == hovered) g.fill(tx, y, tx + size, y + size, 0x30FFFFFF);
            g.renderOutline(tx, y, size, size, isSelected ? 0xFFFFFFFF : Palette.SLOT_EDGE);
            StackRenderer.render(g, list.get(i).icon(), tx + (size - 16) / 2, y + (size - 16) / 2);
            if (list.get(i).id().getPath().equals("get")) {
                // Метка "как получить": плюс поверх иконки. / The "how to get" marker: a plus above the icon.
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                int mx0 = tx + size - 7;
                int my0 = y + size - 7;
                g.fill(mx0 + 3, my0 + 1, mx0 + 5, my0 + 7, 0xFF000000);
                g.fill(mx0 + 1, my0 + 3, mx0 + 7, my0 + 5, 0xFF000000);
                g.fill(mx0 + 2, my0, mx0 + 4, my0 + 6, 0xFFFFC84A);
                g.fill(mx0, my0 + 2, mx0 + 6, my0 + 4, 0xFFFFC84A);
                g.pose().popPose();
            }
        }
        if (hovered >= 0) {
            SeerCategory category = list.get(hovered);
            hoverSink.accept(hovered == current
                    ? List.of(category.name(), Component.literal("Click: all recipes of this machine").withStyle(ChatFormatting.GRAY))
                    : List.of(category.name()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = indexAt(mouseX, mouseY);
        if (index < 0) return false;
        if (button == 1 || (button == 0 && index == selected.getAsInt())) {
            BevelButton.playClick();
            onMachine.accept(index);
            return true;
        }
        if (button == 0) {
            BevelButton.playClick();
            onSelect.accept(index);
            return true;
        }
        return false;
    }
}