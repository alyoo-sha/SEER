package dev.seer.client;

import dev.seer.core.SeerStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * Отрисовка предмета или жидкости в квадрате 16x16.
 * Draws an item or fluid in a 16x16 square.
 */
final class StackRenderer {
    private StackRenderer() {}

    static void render(GuiGraphics gg, SeerStack stack, int x, int y) {
        if (stack.kind() == SeerStack.Kind.ITEM) {
            ItemStack item = stack.itemStack();
            gg.renderItem(item, x, y);
            if (stack.amount() > 1) {
                gg.renderItemDecorations(Minecraft.getInstance().font, item, x, y);
            }
        } else {
            renderFluid(gg, stack.fluidStack(), x, y);
        }
    }

    private static void renderFluid(GuiGraphics gg, FluidStack fluid, int x, int y) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = ext.getStillTexture(fluid);
        if (texture == null) return;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int c = ext.getTintColor(fluid);
        float a = ((c >>> 24) & 255) / 255f;
        gg.blit(x, y, 0, 16, 16, sprite,
                ((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, a == 0f ? 1f : a);
    }

    /** Тултип для предмета или жидкости. / Tooltip for an item or fluid. */
    static void tooltip(GuiGraphics gg, SeerStack stack, int mx, int my) {
        Font font = Minecraft.getInstance().font;
        if (stack.kind() == SeerStack.Kind.ITEM) {
            gg.renderTooltip(font, stack.itemStack(), mx, my);
        } else {
            gg.renderComponentTooltip(font, List.of(stack.displayName()), mx, my);
        }
    }
}