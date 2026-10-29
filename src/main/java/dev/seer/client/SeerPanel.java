package dev.seer.client;


import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.controls.VirtualGrid;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import dev.seer.Seer;
import dev.seer.core.SeerStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Панель с полем поиска и сеткой стеков на OpenUI. Запрос, результаты и позиция списка живут в панели.
 * A search field plus a stack grid panel on OpenUI. The query, results and list position live in the panel.
 */
final class SeerPanel {
    record Rect(int x, int y, int w, int h) {}

    static final int CELL = 18;
    static final int MIN_WIDTH = CELL * 2 + 12;

    // Рантайм сам обрабатывает ввод в TextField, виджеты в экран не добавляем.
    // The runtime handles TextField input itself, so no widgets are added to the screen.
    static final NativeWidgetHost NO_HOST = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) { }
        @Override public void remove(AbstractWidget widget) { }
    };

    private final Function<String, List<SeerStack>> finder;
    private final Consumer<SeerStack> hoverSink;
    private final Signal<String> query = Signals.of("");
    private final Signal<List<SeerStack>> results = Signals.of(List.<SeerStack>of());
    private String lastQuery = "";
    private double savedScroll = 0;
    private VirtualGrid<SeerStack> grid = null;
    private UiRuntime runtime = null;
    private Rect region = null;

    SeerPanel(Function<String, List<SeerStack>> finder, Consumer<SeerStack> hoverSink) {
        this.finder = finder;
        this.hoverSink = hoverSink;
        query.subscribe(text -> {
            lastQuery = text;
            results.set(finder.apply(text));
            resetScroll();
        });
    }

    Signal<String> query() { return query; }

    boolean active() { return runtime != null && region != null; }

    boolean contains(double px, double py) {
        return active() && px >= region.x() && px < region.x() + region.w()
                && py >= region.y() && py < region.y() + region.h();
    }

    boolean hasTextInputFocus() { return active() && runtime.hasTextInputFocus(); }

    // ---------------------------------------------------------------- build

    void build(Function<SeerPanel, UIComponent> header) {
        close();
        results.set(finder.apply(lastQuery));
        grid = Ui.virtualGrid(results, stack -> new StackCell(stack, hoverSink))
                .key(stack -> stack)
                .minCellWidth(CELL)
                .cellHeight(CELL)
                .gap(0);
        restoreScroll(grid, savedScroll);

        runtime = new UiRuntime(Minecraft.getInstance().font, NO_HOST);
        if (region != null) runtime.setViewport(region.x(), region.y(), region.w(), region.h());
        runtime.setRoot(new SeerFrame(Ui.column(header.apply(this), grid.flex()).gap(4)));
    }

    void close() {
        if (grid != null) savedScroll = grid.scrollOffset();
        grid = null;
        if (runtime != null) runtime.close();
        runtime = null;
    }

    void setRegion(Rect rect) {
        region = rect;
        if (runtime != null && rect != null) runtime.setViewport(rect.x(), rect.y(), rect.w(), rect.h());
    }

    /** Пересчитать результаты, позиция списка сохраняется. / Recompute the results; the list position is kept. */
    void refresh() {
        results.set(finder.apply(lastQuery));
    }

    void resetScroll() {
        savedScroll = 0;
        if (grid != null) grid.resetScroll();
    }

    // Выставляем позицию до первой раскладки: она сама ограничит значение по размеру списка.
    // Set the position before the first layout: the layout clamps the value to the list size.
    private static void restoreScroll(VirtualGrid<?> target, double offset) {
        if (offset <= 0) return;
        try {
            Field field = VirtualGrid.class.getDeclaredField("scrollOffset");
            field.setAccessible(true);
            field.setDouble(target, offset);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Seer.LOGGER.warn("SEER: could not restore the item list position", e);
        }
    }

    // ---------------------------------------------------------------- input

    void render(GuiGraphics g, int mx, int my, float pt) {
        if (active()) runtime.render(g, mx, my, pt);
    }

    boolean mouseClicked(double mx, double my, int button) {
        return active() && runtime.mouseClicked(mx, my, button);
    }

    boolean mouseReleased(double mx, double my, int button) {
        return active() && runtime.mouseReleased(mx, my, button);
    }

    boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return active() && runtime.mouseDragged(mx, my, button, dx, dy);
    }

    boolean mouseScrolled(double mx, double my, double delta) {
        return active() && runtime.mouseScrolled(mx, my, delta);
    }

    boolean keyPressed(int key, int scanCode, int modifiers) {
        return active() && runtime.keyPressed(key, scanCode, modifiers);
    }

    boolean charTyped(char codePoint, int modifiers) {
        return active() && runtime.charTyped(codePoint, modifiers);
    }
}