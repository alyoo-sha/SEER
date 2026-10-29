package dev.seer.client;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.layout.Alignment;
import com.nstut.openui.minecraft.UiScreen;
import dev.seer.Seer;
import dev.seer.core.SeerCategory;
import dev.seer.core.SeerRecipe;
import dev.seer.core.SeerRecipes;
import dev.seer.core.SeerStack;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * Экран рецептов. ЛКМ по предмету - рецепты (у станка - всё, что на нём делается), ПКМ - применения,
 * колесо - страницы, Backspace или боковая кнопка мыши - назад. Выбранная вкладка запоминается.
 * Recipes screen. LMB on a stack - recipes (for a workstation - everything it makes), RMB - uses,
 * wheel - pages, Backspace or the mouse side button - back. The selected tab is remembered.
 */
public final class SeerRecipeScreen extends UiScreen {
    private static final int MAX_W = 380;
    private static final int MAX_H = 300;
    private static final int MARGIN = 8;
    private static final ResourceLocation OBTAIN_ID = Seer.makeId("get");

    // Фиксация вкладки: живёт между экранами. / Tab pinning: lives across screens.
    private static ResourceLocation pinnedCategory = null;

    private record Target(SeerStack stack, boolean uses) {}
    private record Snapshot(Target target, ResourceLocation category, int page) {}
    private record Group(Map<SeerCategory, List<SeerRecipe>> map, boolean machine) {}

    private final Screen parent;
    private final Deque<Snapshot> history = new ArrayDeque<>();
    private Target current;
    private Map<SeerCategory, List<SeerRecipe>> grouped = new LinkedHashMap<>();
    private List<SeerCategory> categories = new ArrayList<>();
    private boolean machine = false;
    private int categoryIndex = 0;

    private RecipePageView view = null;
    private TabsBar tabs = null;
    private SeerStack hovered = null;
    private List<Component> tabHover = null;

    public SeerRecipeScreen(SeerStack stack, boolean uses, Screen parent) {
        super(Component.literal("SEER"));
        this.parent = parent;
        load(new Target(stack, uses));
    }

    // Размер подстраивается под окно. / The size adapts to the window.
    @Override protected int uiWidth() { return Math.min(MAX_W, width - 2 * MARGIN); }
    @Override protected int uiHeight() { return Math.min(MAX_H, height - 2 * MARGIN); }
    @Override protected int uiLeft() { return (width - uiWidth()) / 2; }
    @Override protected int uiTop() { return (height - uiHeight()) / 2; }

    // ----------------------------------------------------------------- data

    /** Есть ли что показать. / Whether there is anything to show. */
    static boolean hasContent(SeerStack stack, boolean uses) {
        return !group(new Target(stack, uses)).map().isEmpty();
    }

    private static Group group(Target target) {
        Map<SeerCategory, List<SeerRecipe>> result = new LinkedHashMap<>();

        // ЛКМ по станку: всё, что на нём делается, плюс вкладка "как получить сам станок".
        // LMB on a workstation: everything it makes, plus a tab "how to get the workstation itself".
        if (!target.uses()) {
            List<SeerCategory> served = SeerRecipes.categoriesServedBy(target.stack());
            if (!served.isEmpty()) {
                List<SeerRecipe> obtain = SeerRecipes.recipesFor(target.stack());
                if (!obtain.isEmpty()) {
                    result.put(new SeerCategory(OBTAIN_ID, Component.literal("How to get"), target.stack()), obtain);
                }
                for (SeerCategory category : served) {
                    List<SeerRecipe> all = SeerRecipes.recipesIn(category);
                    if (!all.isEmpty()) result.put(category, all);
                }
                if (!result.isEmpty()) return new Group(result, true);
            }
        }

        List<SeerRecipe> all = target.uses() ? SeerRecipes.usesOf(target.stack()) : SeerRecipes.recipesFor(target.stack());
        for (SeerCategory category : SeerRecipes.categories()) {
            List<SeerRecipe> inCategory = all.stream().filter(r -> r.category() == category).toList();
            if (!inCategory.isEmpty()) result.put(category, inCategory);
        }
        return new Group(result, false);
    }

    private void load(Target target) {
        current = target;
        Group group = group(target);
        grouped = group.map();
        machine = group.machine();
        categories = new ArrayList<>(grouped.keySet());
        categoryIndex = indexOf(pinnedCategory);
        if (categoryIndex < 0) categoryIndex = defaultIndex();
    }

    private int indexOf(ResourceLocation id) {
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).id().equals(id)) return i;
        }
        return -1;
    }

    // У станка по умолчанию открываем первую категорию станка, а не "как получить".
    // For a workstation the first machine category opens by default, not "how to get".
    private int defaultIndex() {
        if (machine) {
            for (int i = 0; i < categories.size(); i++) {
                if (!categories.get(i).id().equals(OBTAIN_ID)) return i;
            }
        }
        return 0;
    }

    private ResourceLocation currentCategoryId() {
        return categories.isEmpty() ? null : categories.get(categoryIndex).id();
    }

    private List<SeerRecipe> currentRecipes() {
        return categories.isEmpty() ? List.of() : grouped.get(categories.get(categoryIndex));
    }

    // Передаём состояние дочерним компонентам, интерфейс не пересоздаётся.
    // Push the state into the child components; the UI is never rebuilt.
    private void sync(int page) {
        if (view == null) return;
        view.setRecipes(currentRecipes(), page);
        tabs.refresh();
    }

    private void open(SeerStack stack, boolean uses) {
        Target next = new Target(stack, uses);
        if (next.equals(current) || group(next).map().isEmpty()) return;
        history.push(new Snapshot(current, currentCategoryId(), view.page()));
        load(next);
        sync(0);
    }

    private void back() {
        if (history.isEmpty()) return;
        Snapshot snapshot = history.pop();
        load(snapshot.target());
        int index = indexOf(snapshot.category());
        if (index >= 0) categoryIndex = index;
        sync(snapshot.page());
    }

    private void selectCategory(int index) {
        categoryIndex = index;
        pinnedCategory = categories.get(index).id();
        sync(0);
    }

    // Иконка вкладки - это станок: открываем все его рецепты. / A tab icon is a workstation: open all its recipes.
    private void openMachine(int index) {
        open(categories.get(index).icon(), false);
    }

    // ------------------------------------------------------------------- UI

    @Override
    protected UIComponent buildUI() {
        int keepPage = view == null ? 0 : view.page();
        view = new RecipePageView(stack -> hovered = stack, lines -> tabHover = lines, actions());
        tabs = new TabsBar(uiWidth() * 6 / 10, () -> categories, () -> categoryIndex,
                this::selectCategory, this::openMachine, lines -> tabHover = lines);
        sync(keepPage);

        UIComponent header = Ui.row(
                new StackCell(() -> current.stack(), stack -> { }),
                new TextLabel(() -> current.stack().displayName().getString(), Palette.TEXT, false).flex(),
                tabs
        ).gap(8).align(Alignment.CENTER);

        UIComponent pager = Ui.row(
                new BevelButton(20, 20, Icons::arrowLeft, () -> view.step(-1)),
                new TextLabel(() -> "Page " + (view.page() + 1) + " / " + view.pageCount(), Palette.TEXT, true).flex(),
                new BevelButton(20, 20, Icons::arrowRight, () -> view.step(1))
        ).align(Alignment.CENTER);

        return new SeerFrame(Ui.padding(8, Ui.column(header, view.flex(), pager)
                .gap(6).align(Alignment.STRETCH)));
    }

    // Действия кнопок на карточке рецепта. / Actions of the recipe card buttons.
    private RecipePageView.Actions actions() {
        return new RecipePageView.Actions() {
            @Override public boolean transferAvailable(SeerRecipe recipe) { return TransferClient.available(parent, recipe); }
            @Override public boolean transferReady(SeerRecipe recipe) { return SeerCraftable.canCraft(recipe); }
            @Override public void transfer(SeerRecipe recipe, boolean max) {
                TransferClient.send(recipe, max);
                Minecraft.getInstance().tell(SeerRecipeScreen.this::onClose);
            }
            @Override public boolean isFavorite(SeerRecipe recipe) {
                return !recipe.outputs().isEmpty() && SeerFavorites.contains(recipe.outputs().get(0));
            }
            @Override public void toggleFavorite(SeerRecipe recipe) {
                if (!recipe.outputs().isEmpty()) SeerFavorites.toggle(recipe.outputs().get(0));
            }
            @Override public void openTree(SeerRecipe recipe) {
                var player = Minecraft.getInstance().player;
                if (player != null) player.displayClientMessage(Component.literal("SEER: crafting tree is coming soon"), true);
            }
        };
    }

    // ---------------------------------------------------------------- input

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        SeerCraftable.update(false);
        hovered = null; // компоненты заново отметят себя / components re-mark themselves while rendering
        tabHover = null;
        renderBackground(g);
        super.render(g, mx, my, pt);
        if (hovered != null) StackRenderer.tooltip(g, hovered, mx, my);
        else if (tabHover != null) g.renderComponentTooltip(font, tabHover, mx, my);
    }

    // Назад по истории, а если истории нет, к списку предметов.
    // Back through the history, or to the item list when the history is empty.
    private void goBack() {
        if (!history.isEmpty()) back();
        else onClose();
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 3) { // боковая кнопка мыши "назад" / the mouse "back" side button
            goBack();
            return true;
        }
        if (hovered != null && (button == 0 || button == 1)) {
            open(hovered, button == 1);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (view != null && view.pageCount() > 1) {
            view.step(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(x, y, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (hovered != null && key == GLFW.GLFW_KEY_A) {
            SeerFavorites.toggle(hovered);
            return true;
        }
        if (hovered != null && (key == GLFW.GLFW_KEY_R || key == GLFW.GLFW_KEY_U)) {
            open(hovered, key == GLFW.GLFW_KEY_U);
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            goBack();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}