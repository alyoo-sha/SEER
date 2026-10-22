package dev.seer;

import dev.seer.api.SeerEntrypoint;
import dev.seer.api.SeerPlugin;
import dev.seer.api.SeerRegistry;
import dev.seer.core.SeerCategory;
import dev.seer.core.SeerStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

// Тестовый плагин: добавляет бочку как станок для крафта.
// Test plugin: adds a barrel as a crafting workstation.
@SeerEntrypoint
public final class TestPlugin implements SeerPlugin {
    @Override
    public void register(SeerRegistry registry) {
        registry.addWorkstation(SeerCategory.CRAFTING, SeerStack.of(new ItemStack(Items.BARREL)));
    }
}
