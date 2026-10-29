package dev.seer.client;


import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

import java.util.List;

/**
 * Иконки активных эффектов в инвентаре: свой вид и положение (ряд над GUI) вместо ванильного списка справа.
 * Active effect icons in the inventory: our own look and position (a row above the GUI) instead of the vanilla list on the right.
 */
final class SeerEffects {
    private static final int CREATIVE_TABS = 28; // высота ряда вкладок креатива / height of the creative tab row
    private static final int SLOT = 22;
    private static final int GAP = 2;
    private static final int TEXT_HEIGHT = 10;
    private static final int MARGIN = 2;
    private static final int HARMFUL_EDGE = 0xFFB04A4A;
    private static final int EXPIRING_TEXT = 0xFFE0A050;

    private SeerEffects() {}

    private static List<MobEffectInstance> visibleEffects() {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        return player.getActiveEffects().stream()
                .filter(effect -> IClientMobEffectExtensions.of(effect).isVisibleInInventory(effect))
                .sorted()
                .toList();
    }

    private static String levelText(int amplifier) {
        // Римские цифры как в ваниле для 2-10, дальше обычные числа. / Roman numerals as in vanilla for 2-10, plain numbers after that.
        return amplifier >= 1 && amplifier <= 9
                ? Component.translatable("enchantment.level." + (amplifier + 1)).getString()
                : String.valueOf(amplifier + 1);
    }

    private static boolean fits(EffectRenderingInventoryScreen<?> gui, int y, int rowHeight) {
        return y >= MARGIN && y + rowHeight <= gui.height - MARGIN;
    }

    /**
     * Рисует ряд эффектов. Возвращает строки подсказки для эффекта под курсором или null.
     * Draws the effect row. Returns the tooltip lines for the effect under the cursor, or null.
     */
    static List<Component> render(GuiGraphics g, EffectRenderingInventoryScreen<?> gui, int mx, int my) {
        List<MobEffectInstance> effects = visibleEffects();
        if (effects.isEmpty()) return null;

        int rowHeight = SLOT + TEXT_HEIGHT;
        // В креативе у GUI есть ряды вкладок сверху и снизу: обходим их, а сам ряд опускаем вниз.
        // Creative has tab rows above and below the GUI: step around them and lower our row.
        boolean creative = gui instanceof CreativeModeInventoryScreen;
        int tabs = creative ? CREATIVE_TABS : 0;
        int above = gui.getGuiTop() - tabs - rowHeight - MARGIN;
        int below = gui.getGuiTop() + gui.getYSize() + tabs + MARGIN;
        int preferred = creative ? below : above;
        int fallback = creative ? above : below;
        int y;
        if (fits(gui, preferred, rowHeight)) y = preferred;
        else if (fits(gui, fallback, rowHeight)) y = fallback;
        else return null; // места нет нигде / no room anywhere          // места нет нигде / no room anywhere

        var font = Minecraft.getInstance().font;
        int count = effects.size();
        int step = Math.max(14, Math.min(SLOT + GAP, gui.getXSize() / count));
        boolean labels = step >= SLOT;
        List<Component> tooltip = null;

        for (int i = 0; i < count; i++) {
            MobEffectInstance effect = effects.get(i);
            int x = gui.getGuiLeft() + i * step;
            int hitWidth = i == count - 1 ? SLOT : Math.min(step, SLOT);
            boolean over = mx >= x && mx < x + hitWidth && my >= y && my < y + SLOT;

            g.fill(x, y, x + SLOT, y + SLOT, Palette.SLOT);
            g.renderOutline(x, y, SLOT, SLOT, effect.getEffect().isBeneficial() ? Palette.SLOT_EDGE : HARMFUL_EDGE);
            if (over) g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0x30FFFFFF);

            // Моды могут рисовать иконку сами, иначе берём ванильный спрайт. / Mods may draw the icon themselves, otherwise use the vanilla sprite.
            if (!IClientMobEffectExtensions.of(effect).renderInventoryIcon(effect, gui, g, x + 2, y + 2, 0)) {
                TextureAtlasSprite sprite = Minecraft.getInstance().getMobEffectTextures().get(effect.getEffect());
                g.blit(x + 2, y + 2, 0, 18, 18, sprite);
            }

            String level = effect.getAmplifier() > 0 ? levelText(effect.getAmplifier()) : null;
            if (level != null) g.drawString(font, level, x + SLOT - font.width(level) - 1, y + SLOT - 9, Palette.TEXT, true);

            if (labels) {
                String time = MobEffectUtil.formatDuration(effect, 1.0F).getString();
                boolean expiring = !effect.isInfiniteDuration() && effect.getDuration() <= 200;
                g.drawCenteredString(font, time, x + SLOT / 2, y + SLOT + 1, expiring ? EXPIRING_TEXT : Palette.TEXT);
            }

            if (over) {
                MutableComponent name = Component.empty()
                        .append(effect.getEffect().getDisplayName())
                        .withStyle(effect.getEffect().getCategory().getTooltipFormatting());
                if (level != null) name.append(" ").append(level);
                tooltip = List.of(name, MobEffectUtil.formatDuration(effect, 1.0F));
            }
        }
        return tooltip;
    }
}