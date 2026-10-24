package dev.seer.client;

import com.nstut.openui.api.Ui;
import com.nstut.openui.controls.VirtualGrid;
import com.nstut.openui.runtime.NativeWidgetHost;
import com.nstut.openui.runtime.UiRuntime;
import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import dev.seer.Seer;
import dev.seer.core.SeerIndex;
import dev.seer.core.SeerStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Оверлей SEER на OpenUI: панель предметов и кнопка настроек у контейнерных экранов.
 * SEER overlay on OpenUI: item panel and a settings button for container screens.
 * ЛКМ по предмету - рецепты, ПКМ - применения. / LMB on a stack - recipes, RMB - uses.
 * Клавиши / Keys: Ctrl+O - показать/скрыть / toggle, R/U - то же для предмета под курсором / same for the hovered stack.
 */
@Mod.EventBusSubscriber(modid = Seer.MOD_ID, value = Dist.CLIENT)
public final class SeerOverlay {
    private static final int CELL = 18;
    private static final int MARGIN = 8;
    private static final int MIN_WIDTH = CELL * 2 + 12;
    private static final int BUTTON_SIZE = 20;

    /** Свободная область для панели. / Free area for the panel. */
    private record Region(int x, int y, int w, int h) {}

    // Рантайм сам обрабатывает ввод в TextField, виджеты в экран не добавляем.
    // The runtime handles TextField input itself, so no widgets are added to the screen.
    private static final NativeWidgetHost NO_HOST = new NativeWidgetHost() {
        @Override public void add(AbstractWidget widget) { }
        @Override public void remove(AbstractWidget widget) { }
    };

    // Состояние живёт между экранами. / State lives across screens.
    private static final Signal<String> query = Signals.of("");
    private static final Signal<List<SeerStack>> results = Signals.of(List.<SeerStack>of());
    private static String lastQuery = "";
    private static boolean visible = true;

    // Позиция списка предметов: живёт между экранами. / The item list position: lives across screens.
    private static VirtualGrid<SeerStack> grid = null;
    private static double savedScroll = 0;


    static {
        query.subscribe(text -> {
            lastQuery = text;
            results.set(SeerIndex.search(text));
            savedScroll = 0;
            if (grid != null) grid.resetScroll();
        });
        SeerState.VISIBLE.subscribe(value -> visible = value);
    }

    private static UiRuntime panelRuntime = null;
    private static UiRuntime buttonRuntime = null;
    private static Region region = null;
    private static SeerStack hovered = null;
    private static String lastError = null;

    // Для какого экрана и размера построены рантаймы. / Which screen and size the runtimes were built for.
    private static Screen builtFor = null;
    private static int builtW, builtH;
    private SeerOverlay() {}

    static void setHovered(SeerStack stack) { hovered = stack; }

    private static boolean supports(Screen screen) {
        return screen instanceof AbstractContainerScreen<?>;
    }

    private static boolean panelActive() {
        return panelRuntime != null && region != null && visible;
    }

    private static boolean inPanel(double x, double y) {
        return panelActive() && x >= region.x() && x < region.x() + region.w()
                && y >= region.y() && y < region.y() + region.h();
    }

    private static void closeRuntimes() {
        if (grid != null) savedScroll = grid.scrollOffset();
        grid = null;
        if (panelRuntime != null) panelRuntime.close();
        if (buttonRuntime != null) buttonRuntime.close();
        panelRuntime = null;
        buttonRuntime = null;
        region = null;
        hovered = null;
    }

    // --------------------------------------------------------------- layout

    /**
     * Считается каждый кадр: учитывает GUI, выступающие виджеты, зоны плагинов и рецепт-книгу.
     * Computed every frame: accounts for the GUI, protruding widgets, plugin zones and the recipe book.
     */
    private static Region computeRegion(Screen screen) {
        AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) screen;
        int minLeft = gui.getGuiLeft();
        int maxRight = gui.getGuiLeft() + gui.getXSize();

        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget w && w.visible) {
                minLeft = Math.min(minLeft, w.getX());
                maxRight = Math.max(maxRight, w.getX() + w.getWidth());
            }
        }
        for (Rect2i zone : SeerExclusionZones.zonesFor(screen)) {
            minLeft = Math.min(minLeft, zone.getX());
            maxRight = Math.max(maxRight, zone.getX() + zone.getWidth());
        }

        boolean leftBlocked = screen instanceof RecipeUpdateListener listener
                && listener.getRecipeBookComponent().isVisible();

        int height = screen.height - 2 * MARGIN;
        int rightX = maxRight + MARGIN;
        int rightW = screen.width - rightX - MARGIN;
        if (rightW >= MIN_WIDTH) return new Region(rightX, MARGIN, rightW, height);

        int leftW = minLeft - 2 * MARGIN;
        if (!leftBlocked && leftW >= MIN_WIDTH) {
            // Внизу слева стоит кнопка настроек. / The settings button sits at the bottom left.
            return new Region(MARGIN, MARGIN, leftW, height - BUTTON_SIZE - MARGIN);
        }
        return null;
    }

    private static void applyViewport(Region r) {
        if (r != null) panelRuntime.setViewport(r.x(), r.y(), r.w(), r.h());
    }

    // ---------------------------------------------------------------- build

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        rebuild(e.getScreen());
    }

    private static void rebuild(Screen screen) {
        closeRuntimes();
        lastError = null;
        builtFor = screen;
        builtW = screen.width;
        builtH = screen.height;
        if (!supports(screen)) return;

        try {
            AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) screen;
            Seer.LOGGER.info("SEER overlay on {}: guiLeft={} xSize={} size={}x{}",
                    screen.getClass().getName(), gui.getGuiLeft(), gui.getXSize(), screen.width, screen.height);

            results.set(SeerIndex.search(lastQuery));
            grid = Ui.virtualGrid(results, stack -> new StackCell(stack, SeerOverlay::setHovered))
                    .key(stack -> stack)
                    .minCellWidth(CELL)
                    .cellHeight(CELL)
                    .gap(0);
            restoreScroll(grid, savedScroll);

            var font = Minecraft.getInstance().font;
            panelRuntime = new UiRuntime(font, NO_HOST);
            region = computeRegion(screen);
            applyViewport(region);
            panelRuntime.setRoot(Ui.card(Ui.column(
                    Ui.textField(query).placeholder("Search...").maxLength(128),
                    grid.flex()).gap(4)));

            buttonRuntime = new UiRuntime(font, NO_HOST);
            buttonRuntime.setViewport(MARGIN, screen.height - MARGIN - BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
            buttonRuntime.setRoot(new BevelButton(BUTTON_SIZE, BUTTON_SIZE, Icons::gear, SeerOverlay::openSettings));
        } catch (Throwable t) {
            Seer.LOGGER.error("SEER: failed to build the overlay", t);
            lastError = t.toString();
            closeRuntimes();
        }
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

    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing e) {
        closeRuntimes();
        builtFor = null;
    }

    // --------------------------------------------------------------- render

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        Screen screen = e.getScreen();
        if (screen != builtFor || screen.width != builtW || screen.height != builtH) rebuild(screen);

        GuiGraphics gg = e.getGuiGraphics();
        var font = Minecraft.getInstance().font;
        if (lastError != null && supports(screen)) {
            gg.drawString(font, "SEER overlay error: " + lastError, MARGIN, MARGIN, 0xFF5555);
        }
        if (buttonRuntime == null) return;

        int mx = e.getMouseX();
        int my = e.getMouseY();
        region = computeRegion(screen);
        hovered = null; // ячейки заново отметят себя / cells re-mark themselves while rendering

        buttonRuntime.render(gg, mx, my, e.getPartialTick());
        if (visible && region == null) {
            // Подсказка, почему панели нет. / A hint explaining why there is no panel.
            gg.drawString(font, "no room for the panel", MARGIN + BUTTON_SIZE + 6,
                    screen.height - MARGIN - BUTTON_SIZE + 6, 0xAAAAAA);
        }
        if (panelActive()) {
            applyViewport(region);
            panelRuntime.render(gg, mx, my, e.getPartialTick());
            if (hovered != null) StackRenderer.tooltip(gg, hovered, mx, my);
        }
    }

    // ---------------------------------------------------------------- input

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre e) {
        if (buttonRuntime == null) return;
        double mx = e.getMouseX();
        double my = e.getMouseY();
        int button = e.getButton();

        // ЛКМ - рецепты, ПКМ - применения. / LMB - recipes, RMB - uses.
        if (panelActive() && inPanel(mx, my) && hovered != null && (button == 0 || button == 1)) {
            openRecipes(hovered, button == 1, e.getScreen());
            e.setCanceled(true);
            return;
        }

        boolean handled = buttonRuntime.mouseClicked(mx, my, button);
        if (!handled && panelActive()) handled = panelRuntime.mouseClicked(mx, my, button);
        // Клик по панели не должен "выбрасывать" предмет с курсора.
        // A click on the panel must not drop the cursor item.
        if (handled || inPanel(mx, my)) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre e) {
        if (buttonRuntime == null) return;
        boolean handled = buttonRuntime.mouseReleased(e.getMouseX(), e.getMouseY(), e.getButton());
        if (!handled && panelActive()) handled = panelRuntime.mouseReleased(e.getMouseX(), e.getMouseY(), e.getButton());
        if (handled) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDrag(ScreenEvent.MouseDragged.Pre e) {
        if (!panelActive()) return;
        if (panelRuntime.mouseDragged(e.getMouseX(), e.getMouseY(), e.getMouseButton(), e.getDragX(), e.getDragY())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (!panelActive()) return;
        boolean handled = panelRuntime.mouseScrolled(e.getMouseX(), e.getMouseY(), e.getScrollDelta());
        if (handled || inPanel(e.getMouseX(), e.getMouseY())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (buttonRuntime == null) return;
        int key = e.getKeyCode();

        // Ctrl+O работает всегда, даже при фокусе в поиске. / Ctrl+O always works, even with the search focused.
        if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_O) {
            SeerState.VISIBLE.set(!visible);
            e.setCanceled(true);
            return;
        }
        if (!panelActive()) return;

        if (panelRuntime.hasTextInputFocus()) {
            boolean handled = panelRuntime.keyPressed(key, e.getScanCode(), e.getModifiers());
            // Всё, кроме Esc, остаётся в поле: иначе "E" закроет инвентарь.
            // Everything except Esc stays in the field: otherwise "E" would close the inventory.
            if (handled || key != GLFW.GLFW_KEY_ESCAPE) e.setCanceled(true);
            return;
        }
        // Не отбираем R/U у поиска креатива, наковальни и т.п.
        // Do not steal R/U from the creative / anvil search box, etc.
        if (e.getScreen().getFocused() instanceof EditBox box && box.isFocused()) return;
        if (key == GLFW.GLFW_KEY_R || key == GLFW.GLFW_KEY_U) {
            SeerStack target = hoveredStack(e.getScreen());
            if (target != null) {
                openRecipes(target, key == GLFW.GLFW_KEY_U, e.getScreen());
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onChar(ScreenEvent.CharacterTyped.Pre e) {
        if (!panelActive() || !panelRuntime.hasTextInputFocus()) return;
        panelRuntime.charTyped(e.getCodePoint(), e.getModifiers());
        e.setCanceled(true);
    }

    // -------------------------------------------------------------- actions

    // Предмет под курсором: в панели или в слоте экрана. / Stack under the cursor: in the panel or a screen slot.
    private static SeerStack hoveredStack(Screen screen) {
        if (hovered != null) return hovered;
        Slot slot = ((AbstractContainerScreen<?>) screen).getSlotUnderMouse();
        if (slot != null && slot.hasItem()) {
            // Без NBT, чтобы совпасть с записью индекса. / Without NBT, to match the index entry.
            return SeerStack.of(new ItemStack(slot.getItem().getItem()));
        }
        return null;
    }

    private static void openRecipes(SeerStack target, boolean uses, Screen from) {
        if (!SeerRecipeScreen.hasContent(target, uses)) return; // нечего показывать / nothing to show
        Minecraft mc = Minecraft.getInstance();
        // Откладываем: рантайм нельзя закрывать внутри его же обработчика.
        // Deferred: a runtime must not be closed inside its own handler.
        mc.tell(() -> mc.setScreen(new SeerRecipeScreen(target, uses, from)));
    }

    private static void openSettings() {
        Minecraft mc = Minecraft.getInstance();
        Screen from = mc.screen;
        mc.tell(() -> mc.setScreen(new SeerSettingsScreen(from)));
    }
}