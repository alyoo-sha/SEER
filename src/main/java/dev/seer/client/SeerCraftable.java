package dev.seer.client;

import dev.seer.core.SeerIngredient;
import dev.seer.core.SeerRecipe;
import dev.seer.core.SeerRecipes;
import dev.seer.core.SeerStack;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Предметы, которые можно сделать из инвентаря игрока (все категории рецептов).
 * Items that can be made from the player's inventory (all recipe categories).
 * Пока без учёта NBT и количества больше 1 на ингредиент. / For now NBT and per-ingredient amounts above 1 are ignored.
 */
final class SeerCraftable {
    private static volatile List<SeerStack> craftable = List.of();
    private static volatile Map<Item, Integer> currentCounts = Map.of();
    private static int fingerprint = 0;

    private SeerCraftable() {}

    static List<SeerStack> list() { return craftable; }

    /**
     * Пересчитывает список, если инвентарь изменился (или force). Возвращает true, если список поменялся.
     * Recomputes the list if the inventory changed (or force). Returns true if the list changed.
     */
    static boolean update(boolean force) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return false;
        Inventory inventory = player.getInventory();

        int print = 17;
        Map<Item, Integer> counts = new HashMap<>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            print = print * 31 + stack.getItem().hashCode() * 64 + stack.getCount();
        }
        currentCounts = counts;
        if (!force && print == fingerprint) return false;
        fingerprint = print;

        Set<SeerStack> result = new LinkedHashSet<>();
        if (!counts.isEmpty()) {
            for (SeerRecipe recipe : SeerRecipes.all()) {
                if (!canCraft(recipe, counts)) continue;
                for (SeerStack output : recipe.outputs()) result.add(output.withAmount(1));
            }
        }
        List<SeerStack> updated = List.copyOf(result);
        boolean changed = !updated.equals(craftable);
        craftable = updated;
        return changed;
    }

    /** Хватает ли ингредиентов в инвентаре для этого рецепта. / Whether the inventory has the ingredients for this recipe. */
    static boolean canCraft(SeerRecipe recipe) {
        return recipe.inputs().stream().anyMatch(ingredient -> !ingredient.isEmpty())
                && canCraft(recipe, currentCounts);
    }

    private static boolean canCraft(SeerRecipe recipe, Map<Item, Integer> counts) {
        Map<Item, Integer> remaining = new HashMap<>(counts);
        for (SeerIngredient ingredient : recipe.inputs()) {
            if (ingredient.isEmpty()) continue;
            boolean found = false;
            for (SeerStack alternative : ingredient.alternatives()) {
                if (alternative.kind() != SeerStack.Kind.ITEM) continue;
                Item item = alternative.itemType();
                int have = remaining.getOrDefault(item, 0);
                if (have > 0) {
                    remaining.put(item, have - 1);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}