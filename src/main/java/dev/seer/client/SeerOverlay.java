package dev.seer.client;

import dev.seer.Seer;
import dev.seer.core.SeerIndex;
import dev.seer.core.SeerRecipes;
import dev.seer.core.SeerStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Оверлей справа от контейнерных экранов: поиск, сетка предметов, страницы.
 * Overlay to the right of container screens: search, item grid, pages.
 * Клавиши / Keys: Ctrl+O - показать/скрыть / toggle, Ctrl+F - поиск / focus search,
 * R/U - рецепты/применения предмета под курсором / recipes/uses of the hovered stack.
 */
@Mod.EventBusSubscriber(modid = Seer.MOD_ID, value = Dist.CLIENT)
public final class SeerOverlay {
    private static final int CELL = 18;
    private static final int HEADER = 24;
    private static final int FOOTER = 16;

    private static boolean visible = true;
    private static boolean active = false;
    private static String lastQuery = "";
    private static List<SeerStack> results = List.of();
    private static EditBox searchBox = null;
    private static int page = 0;
    private static int panelX, panelY, panelW, panelH, gridX, gridY, cols, rows;

    private SeerOverlay() {}

    private static boolean supports(Screen screen) {
        return screen instanceof AbstractContainerScreen<?> && !(screen instanceof CreativeModeInventoryScreen);
    }

    // ---------------------------------------------------------------- layout

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post e) {
        Screen screen = e.getScreen();
        active = false;
        if (!supports(screen)) return;

        AbstractContainerScreen<?> gui = (AbstractContainerScreen<?>) screen;
        panelX = gui.getGuiLeft() + gui.getXSize() + 6;
        panelY = 4;
        panelW = screen.width - panelX - 4;
        panelH = screen.height - 8;
        cols = panelW / CELL;
        rows = (panelH - HEADER - FOOTER) / CELL;
        if (cols < 2 || rows < 1) return; // слишком мало места / not enough room

        gridX = panelX + (panelW - cols * CELL) / 2;
        gridY = panelY + HEADER;

        searchBox = new EditBox(Minecraft.getInstance().font, gridX, panelY + 2, cols * CELL - 1, 18,
                Component.literal("Search"));
        searchBox.setMaxLength(128);
        searchBox.setValue(lastQuery);
        searchBox.setResponder(SeerOverlay::onSearchChanged);

        results = SeerIndex.search(lastQuery);
        setPage(page);
        active = true;
    }

    private static void onSearchChanged(String text) {
        lastQuery = text;
        results = SeerIndex.search(text);
        page = 0;
    }

    private static int pageCount() {
        int perPage = cols * rows;
        return Math.max(1, (results.size() + perPage - 1) / perPage);
    }

    // Циклическая смена страницы. / Wrap-around page change.
    private static void setPage(int p) {
        int count = pageCount();
        page = ((p % count) + count) % count;
    }

    private static boolean inPanel(double x, double y) {
        return x >= panelX && x < panelX + panelW && y >= panelY && y < panelY + panelH;
    }

    // Индекс результата под курсором или -1. / Result index under the cursor or -1.
    private static int cellAt(double mx, double my) {
        if (mx < gridX || my < gridY) return -1;
        int col = (int) ((mx - gridX) / CELL);
        int row = (int) ((my - gridY) / CELL);
        if (col >= cols || row >= rows) return -1;
        int index = page * cols * rows + row * cols + col;
        return index < results.size() ? index : -1;
    }

    // ---------------------------------------------------------------- render

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        if (!active || !visible) return;
        GuiGraphics gg = e.getGuiGraphics();
        Font font = Minecraft.getInstance().font;
        int mx = e.getMouseX();
        int my = e.getMouseY();

        gg.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x55000000);
        searchBox.render(gg, mx, my, e.getPartialTick());

        int perPage = cols * rows;
        int start = page * perPage;
        for (int i = 0; i < perPage && start + i < results.size(); i++) {
            int x = gridX + (i % cols) * CELL + 1;
            int y = gridY + (i / cols) * CELL + 1;
            StackRenderer.render(gg, results.get(start + i), x, y);
        }

        int footerY = panelY + panelH - FOOTER + 4;
        gg.drawString(font, "<", gridX + 2, footerY, 0xFFFFFF);
        gg.drawString(font, ">", gridX + cols * CELL - 8, footerY, 0xFFFFFF);
        gg.drawCenteredString(font, (page + 1) + "/" + pageCount(), gridX + cols * CELL / 2, footerY, 0xFFFFFF);

        int hovered = cellAt(mx, my);
        if (hovered >= 0) {
            int local = hovered - start;
            int x = gridX + (local % cols) * CELL + 1;
            int y = gridY + (local / cols) * CELL + 1;
            gg.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
            SeerStack stack = results.get(hovered);
            if (stack.kind() == SeerStack.Kind.ITEM) {
                gg.renderTooltip(font, stack.itemStack(), mx, my);
            } else {
                gg.renderComponentTooltip(font, List.of(stack.displayName()), mx, my);
            }
        }
    }

    // ----------------------------------------------------------------- input

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed.Pre e) {
        if (!active || !visible) return;
        double mx = e.getMouseX();
        double my = e.getMouseY();
        if (!inPanel(mx, my)) {
            searchBox.setFocused(false);
            return;
        }

        if (searchBox.isMouseOver(mx, my)) {
            if (e.getButton() == 1) { // ПКМ очищает поиск / right click clears the search
                searchBox.setValue("");
                searchBox.setFocused(true);
            } else {
                searchBox.mouseClicked(mx, my, e.getButton());
            }
        } else {
            searchBox.setFocused(false);
            int footerY = panelY + panelH - FOOTER + 4;
            if (e.getButton() == 0 && my >= footerY - 2 && my < footerY + 10) {
                if (mx >= gridX && mx < gridX + 14) setPage(page - 1);
                else if (mx >= gridX + cols * CELL - 14 && mx < gridX + cols * CELL) setPage(page + 1);
            }
        }
        // Клик по панели не должен "выбрасывать" предмет с курсора.
        // A click on the panel must not drop the cursor item.
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre e) {
        if (!active || !visible || !inPanel(e.getMouseX(), e.getMouseY())) return;
        setPage(page + (e.getScrollDelta() > 0 ? -1 : 1));
        e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre e) {
        if (!active) return;
        int key = e.getKeyCode();

        if (searchBox.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) searchBox.setFocused(false);
            else searchBox.keyPressed(key, e.getScanCode(), e.getModifiers());
            e.setCanceled(true); // чтобы "E" не закрывал инвентарь / so "E" does not close the inventory
            return;
        }
        if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_O) {
            visible = !visible;
            searchBox.setFocused(false);
            e.setCanceled(true);
            return;
        }
        if (!visible) return;

        if (Screen.hasControlDown() && key == GLFW.GLFW_KEY_F) {
            searchBox.setFocused(true);
            e.setCanceled(true);
        } else if (key == GLFW.GLFW_KEY_R || key == GLFW.GLFW_KEY_U) {
            SeerStack target = hoveredStack(e.getScreen());
            if (target != null) {
                showRecipes(target, key == GLFW.GLFW_KEY_U);
                e.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onChar(ScreenEvent.CharacterTyped.Pre e) {
        if (active && visible && searchBox.isFocused()) {
            searchBox.charTyped(e.getCodePoint(), e.getModifiers());
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase == TickEvent.Phase.END && active && visible && Minecraft.getInstance().screen != null) {
            searchBox.tick(); // мигание курсора / cursor blink
        }
    }

    // ----------------------------------------------------------------- hover

    private static double mouseX() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
    }

    private static double mouseY() {
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();
    }

    // Предмет под курсором: в панели или в слоте экрана. / Stack under the cursor: in the panel or a screen slot.
    private static SeerStack hoveredStack(Screen screen) {
        int index = cellAt(mouseX(), mouseY());
        if (index >= 0) return results.get(index);

        Slot slot = ((AbstractContainerScreen<?>) screen).getSlotUnderMouse();
        if (slot != null && slot.hasItem()) {
            // Без NBT, чтобы совпасть с записью индекса. / Without NBT, to match the index entry.
            return SeerStack.of(new ItemStack(slot.getItem().getItem()));
        }
        return null;
    }

    // Временно: результат в чат, пока нет экрана рецептов. / Temporary: report to chat until the recipe screen exists.
    private static void showRecipes(SeerStack stack, boolean uses) {
        List<?> list = uses ? SeerRecipes.usesOf(stack) : SeerRecipes.recipesFor(stack);
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(
                    "SEER: " + (uses ? "uses " : "recipes ") + stack.id() + " = " + list.size()), false);
        }
    }
}
