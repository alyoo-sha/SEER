package dev.seer.client;


import com.nstut.openui.api.Ui;
import com.nstut.openui.layout.Alignment;
import com.nstut.openui.runtime.UiRuntime;
import dev.seer.Seer;
import dev.seer.core.SeerIndex;
import dev.seer.core.SeerStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Оверлей SEER. Справа: все предметы. Слева: избранное или то, что можно скрафтить сейчас
 * (переключается кнопкой с верстаком или кнопкой книги рецептов). Внизу слева: настройки.
 * SEER overlay. Right: all items. Left: favorites or what can be crafted right now
 * (switched by the crafting-table button or the recipe book button). Bottom left: settings.
 * ЛКМ по предмету - рецепты, ПКМ - применения. / LMB on a stack - recipes, RMB - uses.
 * Клавиши / Keys: Ctrl+O - показать/скрыть / toggle, R/U - рецепты/применения, A - избранное / favorite.
 */
@Mod.EventBusSubscriber(modid = Seer.MOD_ID, value = Dist.CLIENT)
public final class SeerOverlay {
    private static final int MARGIN = 8;
    private static final int BUTTON_SIZE = 20;

    private record Regions(SeerPanel.Rect left, SeerPanel.Rect right) {}

    private static boolean visible = true;
    private static boolean craftableMode = false;

    static {
        SeerState.VISIBLE.subscribe(value -> visible = value);
    }

    private static final SeerPanel right = new SeerPanel(SeerIndex::search, SeerOverlay::setHovered);
    private static final SeerPanel left = new SeerPanel(
            text -> SeerIndex.filter(craftableMode ? SeerCraftable.list() : SeerFavorites.list(), text),
            SeerOverlay::setHovered);
    private static final List<SeerPanel> panels = List.of(left, right);

    private static UiRuntime buttonRuntime = null;
    private static ImageButton bookButton = null;
    private static SeerStack hovered = null;
    private static String lastError = null;

    // Для какого экрана и размера всё построено. / Which screen and size everything was built for.
    private static Screen builtFor = null;
    private static int builtW, builtH;

    private SeerOverlay() {}

    static void setHovered(SeerStack stack) { hovered = stack; }

    private static boolean supports(Screen screen) {
        return screen instanceof AbstractContainerScreen<?>;
    }

    private static boolean inAnyPanel(double x, double y) {
        for (SeerPanel panel : panels) {
            if (panel.contains(x, y)) return true;
        }
        return false;
    }

    private static SeerPanel focusedPanel() {
        for (SeerPanel panel : panels) {
            if (panel.hasTextInputFocus()) return panel;
        }
        return null;
    }

    private static void closeRuntimes() {
        for (SeerPanel panel : panels) panel.close();
        if (buttonRuntime != null) buttonRuntime.close();
        buttonRuntime = null;
        bookButton = null;
        hovered = null;
    }

    // --------------------------------------------------------------- layout

    /**
     * Считается каждый кадр: учитывает GUI, выступающие виджеты и зоны плагинов.
     * Computed every frame: accounts for the GUI, protruding widgets and plugin zones.
     */
    private static Regions computeRegions(Screen screen) {
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

        int height = screen.height - 2 * MARGIN;
        int rightX = maxRight + MARGIN;
        int rightW = screen.width - rightX - MARGIN;
        int leftW = minLeft - 2 * MARGIN;
        return new Regions(
                // Внизу слева стоит кнопка настроек. / The settings button sits at the bottom left.
                leftW >= SeerPanel.MIN_WIDTH
                        ? new SeerPanel.Rect(MARGIN, MARGIN, leftW, height - BUTTON_SIZE - MARGIN) : null,
                rightW >= SeerPanel.MIN_WIDTH
                        ? new SeerPanel.Rect(rightX, MARGIN, rightW, height) : null);
    }

    // Кнопка ванильной книги рецептов: ImageButton 20x18. / The vanilla recipe book button: a 20x18 ImageButton.
    private static ImageButton findBookButton(Screen screen) {
        if (!(screen instanceof RecipeUpdateListener)) return null;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof ImageButton button && button.getWidth() == 20 && button.getHeight() == 18) {
                return button;
            }
        }
        return null;
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

            bookButton = findBookButton(screen);
            Regions regions = computeRegions(screen);
            right.setRegion(regions.right());
            left.setRegion(regions.left());

            // Иконка верстака создаётся здесь, а не при загрузке класса: реестры к тому моменту пусты.
            // The crafting table icon is created here, not at class load: the registries are empty by then.
            SeerStack table = SeerStack.of(new ItemStack(Items.CRAFTING_TABLE));

            right.build(panel -> Ui.textField(panel.query()).placeholder("Search...").maxLength(128));
            left.build(panel -> Ui.row(
                    new IconView(12, 18, Icons::magnifier),
                    Ui.textField(panel.query()).placeholder("Search...").maxLength(128).flex(),
                    new BevelButton(20, 20, (g, bx, by, bw, bh, hover) -> {
                        if (craftableMode) g.fill(bx + 2, by + 2, bx + bw - 2, by + bh - 2, Palette.ACCENT);
                        StackRenderer.render(g, table, bx + 2, by + 2);
                    }, SeerOverlay::toggleCraftable)
            ).gap(4).align(Alignment.CENTER));

            buttonRuntime = new UiRuntime(Minecraft.getInstance().font, SeerPanel.NO_HOST);
            buttonRuntime.setViewport(MARGIN, screen.height - MARGIN - BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);
            buttonRuntime.setRoot(new BevelButton(BUTTON_SIZE, BUTTON_SIZE, Icons::gear, SeerOverlay::openSettings));
        } catch (Throwable t) {
            Seer.LOGGER.error("SEER: failed to build the overlay", t);
            lastError = t.toString();
            closeRuntimes();
        }
    }

    @SubscribeEvent
    public static void onClosing(ScreenEvent.Closing e) {
        closeRuntimes();
        builtFor = null;
    }

    // Список "можно скрафтить" обновляется при изменении инвентаря. / The "craftable" list updates when the inventory changes.
    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || buttonRuntime == null || !craftableMode) return;
        if (SeerCraftable.update(false)) left.refresh();
    }

    // Ванильные иконки эффектов не рисуем: вместо них свои (SeerEffects), над GUI.
    // Vanilla effect icons are not drawn: SEER draws its own (SeerEffects) above the GUI.
    @SubscribeEvent
    public static void onVanillaEffects(ScreenEvent.RenderInventoryMobEffects e) {
        e.setCanceled(true);
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
        if (screen instanceof EffectRenderingInventoryScreen<?> effectsGui) {
            var effectsTooltip = SeerEffects.render(gg, effectsGui, e.getMouseX(), e.getMouseY());
            if (effectsTooltip != null) gg.renderComponentTooltip(font, effectsTooltip, e.getMouseX(), e.getMouseY());
        }
        if (buttonRuntime == null) return;

        int mx = e.getMouseX();
        int my = e.getMouseY();
        Regions regions = computeRegions(screen);
        right.setRegion(regions.right());
        left.setRegion(regions.left());
        hovered = null; // ячейки заново отметят себя / cells re-mark themselves while rendering

        buttonRuntime.render(gg, mx, my, e.getPartialTick());
        boolean anyPanel = false;
        if (visible) {
            for (SeerPanel panel : panels) {
                if (!panel.active()) continue;
                panel.render(gg, mx, my, e.getPartialTick());
                anyPanel = true;
            }
            if (!anyPanel) {
                // Подсказка, почему панелей нет. / A hint explaining why there are no panels.
                gg.drawString(font, "no room for the panels", MARGIN + BUTTON_SIZE + 6,
                        screen.height - MARGIN - BUTTON_SIZE + 6, 0xAAAAAA);
            }
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

        // Кнопка книги: вместо ванильной панели переключаем левую панель SEER.
        // The book button: toggles the SEER left panel instead of the vanilla one.
        if (bookButton != null && button == 0 && bookButton.isMouseOver(mx, my)) {
            BevelButton.playClick();
            toggleCraftable();
            e.setCanceled(true);
            return;
        }
        // ЛКМ - рецепты, ПКМ - применения. / LMB - recipes, RMB - uses.
        if (visible && hovered != null && (button == 0 || button == 1) && inAnyPanel(mx, my)) {
            openRecipes(hovered, button == 1, e.getScreen());
            e.setCanceled(true);
            return;
        }

        boolean handled = buttonRuntime.mouseClicked(mx, my, button);
        if (visible) {
            // Клик получают все панели, чтобы снять фокус с чужого поля. / Every panel gets the click so a foreign field loses focus.
            for (SeerPanel panel : panels) handled |= panel.mouseClicked(mx, my, button);
        }
        // Клик по панели не должен "выбрасывать" предмет с курсора.
        // A click on a panel must not drop the cursor item.
        if (handled || (visible && inAnyPanel(mx, my))) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRelease(ScreenEvent.MouseButtonReleased.Pre e) {
        if (buttonRuntime == null) return;
        boolean handled = buttonRuntime.mouseReleased(e.getMouseX(), e.getMouseY(), e.getButton());
        if (visible) {
            for (SeerPanel panel : panels) handled |= panel.mouseReleased(e.getMouseX(), e.getMouseY(), e.getButton());
        }
        if (handled) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDrag(ScreenEvent.MouseDragged.Pre e) {
        if (!visible) return;
        boolean handled = false;
        for (SeerPanel panel : panels) {
            handled |= panel.mouseDragged(e.getMouseX(), e.getMouseY(), e.getMouseButton(), e.getDragX(), e.getDragY());
        }
        if (handled) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (!visible) return;
        for (SeerPanel panel : panels) {
            if (panel.contains(e.getMouseX(), e.getMouseY())) {
                panel.mouseScrolled(e.getMouseX(), e.getMouseY(), e.getScrollDelta());
                e.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (buttonRuntime == null) return;
        int key = e.getKeyCode();

        // Ctrl+O работает всегда, даже при фокусе в поиске. / Ctrl+O always works, even with a search focused.
        if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_O) {
            SeerState.VISIBLE.set(!visible);
            e.setCanceled(true);
            return;
        }
        if (!visible) return;

        SeerPanel focused = focusedPanel();
        if (focused != null) {
            boolean handled = focused.keyPressed(key, e.getScanCode(), e.getModifiers());
            // Всё, кроме Esc, остаётся в поле: иначе "E" закроет инвентарь.
            // Everything except Esc stays in the field: otherwise "E" would close the inventory.
            if (handled || key != GLFW.GLFW_KEY_ESCAPE) e.setCanceled(true);
            return;
        }

        // Не перехватываем буквы, пока пишем в чужое текстовое поле (креатив, наковальня).
        // Do not intercept letters while typing into another text field (creative, anvil).
        if (e.getScreen().getFocused() instanceof EditBox box && box.isFocused()) return;

        if (key == GLFW.GLFW_KEY_R || key == GLFW.GLFW_KEY_U || key == GLFW.GLFW_KEY_A) {
            SeerStack target = hoveredStack(e.getScreen());
            if (target != null) {
                if (key == GLFW.GLFW_KEY_A) toggleFavorite(target);
                else openRecipes(target, key == GLFW.GLFW_KEY_U, e.getScreen());
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onChar(ScreenEvent.CharacterTyped.Pre e) {
        if (!visible) return;
        SeerPanel focused = focusedPanel();
        if (focused == null) return;
        focused.charTyped(e.getCodePoint(), e.getModifiers());
        e.setCanceled(true);
    }

    // -------------------------------------------------------------- actions

    // Предмет под курсором: в панели или в слоте экрана. / Stack under the cursor: in a panel or a screen slot.
    private static SeerStack hoveredStack(Screen screen) {
        if (hovered != null) return hovered;
        Slot slot = ((AbstractContainerScreen<?>) screen).getSlotUnderMouse();
        if (slot != null && slot.hasItem()) {
            // Сначала точный предмет (зелья, книги), иначе без NBT (повреждённые или зачарованные вещи).
            // The exact stack first (potions, books), otherwise without NBT (damaged or enchanted gear).
            SeerStack exact = SeerStack.of(slot.getItem());
            return SeerIndex.contains(exact) ? exact : SeerStack.of(new ItemStack(slot.getItem().getItem()));
        }
        return null;
    }

    private static void toggleFavorite(SeerStack target) {
        SeerFavorites.toggle(target);
        left.refresh();
    }

    // Переключение левой панели: избранное / можно скрафтить. / Switch the left panel: favorites / craftable.
    private static void toggleCraftable() {
        if (!visible) {
            SeerState.VISIBLE.set(true);
            craftableMode = true;
        } else {
            craftableMode = !craftableMode;
        }
        if (craftableMode) SeerCraftable.update(true);
        left.refresh();
        left.resetScroll();
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