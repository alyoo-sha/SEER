package dev.seer.network;


import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Раскладка рецепта по сетке штатной логикой Minecraft (с проверкой типа рецепта и меню).
 * Placing a recipe into the grid with Minecraft's own logic (with a recipe/menu type check).
 */
public final class RecipeTransfer {
    private RecipeTransfer() {}

    /** Подходит ли рецепт этому меню. Используется и на клиенте, и на сервере. / Whether the recipe fits this menu. Used by both the client and the server. */
    public static boolean supports(AbstractContainerMenu menu, Recipe<?> recipe) {
        if (menu instanceof CraftingMenu) return recipe.getType() == RecipeType.CRAFTING;
        if (menu instanceof InventoryMenu) {
            return recipe.getType() == RecipeType.CRAFTING && recipe.canCraftInDimensions(2, 2);
        }
        if (menu instanceof FurnaceMenu) return recipe.getType() == RecipeType.SMELTING;
        if (menu instanceof BlastFurnaceMenu) return recipe.getType() == RecipeType.BLASTING;
        if (menu instanceof SmokerMenu) return recipe.getType() == RecipeType.SMOKING;
        return false;
    }

    static void apply(ServerPlayer player, ResourceLocation id, boolean max) {
        AbstractContainerMenu menu = player.containerMenu;
        if (player.isSpectator() || !(menu instanceof RecipeBookMenu<?> bookMenu) || !menu.stillValid(player)) return;

        Recipe<?> recipe = player.server.getRecipeManager().byKey(id).orElse(null);
        if (recipe == null || !supports(menu, recipe)) return;

        // Штатная логика кладёт только изученные рецепты: открываем рецепт на время вызова.
        // The vanilla logic only places unlocked recipes: unlock the recipe for the duration of the call.
        var book = player.getRecipeBook();
        boolean known = book.contains(recipe);
        if (!known) book.add(recipe);
        try {
            bookMenu.handlePlacement(max, recipe, player);
        } finally {
            if (!known) book.remove(recipe);
        }
    }
}