package dev.seer.client;

import com.nstut.openui.api.UIComponent;
import dev.seer.core.SeerIngredient;
import dev.seer.core.SeerRecipe;
import dev.seer.core.SeerStack;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/**
 * Страница карточек рецептов. Число колонок и строк считается по размеру компонента каждый кадр,
 * смена страницы не пересоздаёт интерфейс.
 * A page of recipe cards. Columns and rows are computed from the component size every frame;
 * changing the page never rebuilds the UI.
 */
final class RecipePageView extends UIComponent {
    private static final int SLOT = 18;
    private static final int GAP = 4;
    private static final int PAD = 6;
    private static final int ARROW = 12;
    private static final int EXTRA_LINE = 10;

    private final Consumer<SeerStack> hoverSink;

    private List<SeerRecipe> recipes = List.of();
    private int page = 0;
    private int maxInputCols = 1;
    private int maxInputRows = 1;
    private int maxOutputRows = 1;
    private boolean extras = false;
    private int cardW = 0;
    private int cardH = 0;
    private int perPage = 1;

    RecipePageView(Consumer<SeerStack> hoverSink) {
        this.hoverSink = hoverSink;
    }

    /** Новый список; размер карточки считается сразу по самому большому рецепту. / New list; card size is computed up front from the biggest recipe. */
    void setRecipes(List<SeerRecipe> list, int startPage) {
        recipes = list;
        page = Math.max(0, startPage);
        maxInputCols = 1;
        maxInputRows = 1;
        maxOutputRows = 1;
        extras = false;
        for (SeerRecipe r : list) {
            int cols = inputColumns(r);
            maxInputCols = Math.max(maxInputCols, cols);
            maxInputRows = Math.max(maxInputRows, rowsFor(r.inputs().size(), cols));
            maxOutputRows = Math.max(maxOutputRows, r.outputs().size());
            extras |= r.cookTime() > 0;
        }
        cardW = PAD * 2 + maxInputCols * SLOT + GAP + ARROW + GAP + SLOT;
        cardH = PAD * 2 + Math.max(maxInputRows, maxOutputRows) * SLOT + (extras ? EXTRA_LINE : 0);
    }

    int page() { return page; }

    int pageCount() { return Math.max(1, (recipes.size() + perPage - 1) / perPage); }

    void step(int delta) {
        int count = pageCount();
        page = (((page + delta) % count) + count) % count;
    }

    private static int inputColumns(SeerRecipe r) {
        return r.gridWidth() > 0 ? r.gridWidth() : Math.max(1, r.inputs().size());
    }

    private static int rowsFor(int count, int columns) {
        return Math.max(1, (count + columns - 1) / columns);
    }

    private static SeerStack pick(SeerIngredient ingredient) {
        List<SeerStack> alternatives = ingredient.alternatives();
        if (alternatives.isEmpty()) return null;
        // Раз в секунду меняем вариант (как теги в EMI). / Switch the alternative every second (like tags in EMI).
        return alternatives.get((int) ((System.currentTimeMillis() / 1000) % alternatives.size()));
    }

    @Override public int preferredWidth(Font font) { return 100; }
    @Override public int preferredHeight(Font font) { return 40; }

    @Override
    public void render(GuiGraphics g, Font font, int mx, int my, float pt) {
        if (!visible) return;
        if (recipes.isEmpty()) {
            g.drawCenteredString(font, "No recipes", x + width / 2, y + height / 2 - 4, Palette.MUTED);
            return;
        }

        int columns = Math.max(1, Math.min(4, (width + GAP) / (cardW + GAP)));
        int rows = Math.max(1, (height + GAP) / (cardH + GAP));
        perPage = columns * rows;
        page = Math.min(page, pageCount() - 1);

        int cellW = (width - (columns - 1) * GAP) / columns;
        int from = page * perPage;
        for (int i = 0; i < perPage && from + i < recipes.size(); i++) {
            int cx = x + (i % columns) * (cellW + GAP);
            int cy = y + (i / columns) * (cardH + GAP);
            drawCard(g, font, recipes.get(from + i), cx, cy, cellW, mx, my);
        }
    }

    private void drawCard(GuiGraphics g, Font font, SeerRecipe r, int cx, int cy, int cellW, int mx, int my) {
        // Объём: тень под плашкой, градиент, блики сверху и слева, тёмный край снизу и справа.
        // Volume: a drop shadow, a gradient, highlights on top and left, a dark edge at the bottom and right.
        g.fill(cx + 1, cy + 2, cx + cellW + 1, cy + cardH + 2, Palette.CARD_SHADOW);
        g.fillGradient(cx, cy, cx + cellW, cy + cardH, Palette.CARD_TOP, Palette.CARD_BOTTOM);
        g.fill(cx, cy, cx + cellW, cy + 1, Palette.CARD_LIGHT);
        g.fill(cx, cy, cx + 1, cy + cardH, Palette.CARD_LIGHT);
        g.fill(cx, cy + cardH - 1, cx + cellW, cy + cardH, Palette.CARD_DARK_EDGE);
        g.fill(cx + cellW - 1, cy, cx + cellW, cy + cardH, Palette.CARD_DARK_EDGE);

        int inputCols = inputColumns(r);
        int inputsW = inputCols * SLOT;
        int inputsH = rowsFor(r.inputs().size(), inputCols) * SLOT;
        int blockH = Math.max(maxInputRows, maxOutputRows) * SLOT;
        int contentW = inputsW + GAP + ARROW + GAP + SLOT;
        int x0 = cx + (cellW - contentW) / 2;
        int y0 = cy + PAD;

        int inputsY = y0 + (blockH - inputsH) / 2;
        for (int i = 0; i < r.inputs().size(); i++) {
            drawSlot(g, x0 + (i % inputCols) * SLOT, inputsY + (i / inputCols) * SLOT, pick(r.inputs().get(i)), mx, my);
        }

        int arrowX = x0 + inputsW + GAP;
        drawArrow(g, arrowX, y0 + blockH / 2);

        int outputsH = r.outputs().size() * SLOT;
        int outputX = arrowX + ARROW + GAP;
        int outputY = y0 + (blockH - outputsH) / 2;
        for (int i = 0; i < r.outputs().size(); i++) {
            drawSlot(g, outputX, outputY + i * SLOT, r.outputs().get(i), mx, my);
        }

        if (r.cookTime() > 0) {
            g.drawString(font, String.format(Locale.ROOT, "%.1f s  %.2f XP", r.cookTime() / 20.0, r.experience()),
                    cx + PAD, y0 + blockH + 1, Palette.MUTED, false);
        }
    }

    private void drawSlot(GuiGraphics g, int sx, int sy, SeerStack stack, int mx, int my) {
        g.fill(sx, sy, sx + SLOT, sy + SLOT, Palette.SLOT);
        g.fill(sx, sy, sx + SLOT, sy + 1, Palette.SLOT_SHADOW);                // тень сверху / top shadow
        g.fill(sx, sy, sx + 1, sy + SLOT, Palette.SLOT_SHADOW);                // тень слева / left shadow
        g.fill(sx, sy + SLOT - 1, sx + SLOT, sy + SLOT, Palette.SLOT_LIGHT);   // блик снизу / bottom light
        g.fill(sx + SLOT - 1, sy, sx + SLOT, sy + SLOT, Palette.SLOT_LIGHT);   // блик справа / right light
        if (stack == null) return;
        StackRenderer.render(g, stack, sx + 1, sy + 1);
        if (mx >= sx && mx < sx + SLOT && my >= sy && my < sy + SLOT) {
            g.fillGradient(RenderType.guiOverlay(), sx + 1, sy + 1, sx + SLOT - 1, sy + SLOT - 1, 0x80FFFFFF, 0x80FFFFFF, 0);
            hoverSink.accept(stack);
        }
    }

    // Стрелка вправо 12x9 пикселей. / A 12x9 pixel right arrow.
    private static void drawArrow(GuiGraphics g, int ax, int cy) {
        g.fill(ax, cy - 1, ax + 7, cy + 1, Palette.MUTED);
        for (int i = 0; i < 5; i++) {
            int half = 4 - i;
            g.fill(ax + 7 + i, cy - half, ax + 8 + i, cy + half + 1, Palette.MUTED);
        }
    }
}