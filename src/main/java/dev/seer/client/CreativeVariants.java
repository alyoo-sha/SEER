package dev.seer.client;

import dev.seer.Seer;
import dev.seer.core.SeerStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Все предметы творческих вкладок вместе с вариантами NBT: зелья, зачарованные книги и другое.
 * Порядок совпадает с порядком вкладок креатива.
 * All creative tab items including NBT variants: potions, enchanted books and more.
 * The order matches the creative tab order.
 */
final class CreativeVariants {
    private CreativeVariants() {}

    static List<SeerStack> collect() {
        Minecraft mc = Minecraft.getInstance();
        var connection = mc.getConnection();
        var player = mc.player;
        if (connection == null || player == null) return List.of();
        try {
            // Содержимое вкладок собирается с учётом включённых функций и прав игрока (вызывает события модов).
            // Tab contents are built with the enabled features and the player's permissions (fires mod events).
            CreativeModeTabs.tryRebuildTabContents(
                    connection.enabledFeatures(), player.canUseGameMasterBlocks(), connection.registryAccess());
            List<SeerStack> result = new ArrayList<>();
            for (ItemStack stack : CreativeModeTabs.searchTab().getSearchTabDisplayItems()) {
                if (!stack.isEmpty()) result.add(SeerStack.of(stack));
            }
            return result;
        } catch (Throwable t) {
            Seer.LOGGER.warn("SEER: could not read the creative tabs, falling back to the registry", t);
            return List.of();
        }
    }
}