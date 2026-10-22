package dev.seer.core;

import dev.seer.Seer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Преобразует ванильные рецепты в модель SEER. Остальные типы пока пропускаются.
 * Converts vanilla recipes into the SEER model. Other types are skipped for now.
 */
final class VanillaRecipeLoader {
    private VanillaRecipeLoader() {}

    static List<SeerRecipe> load(RecipeManager manager, RegistryAccess access, List<SeerStack> candidates) {
        List<SeerRecipe> result = new ArrayList<>();
        for (Recipe<?> recipe : manager.getRecipes()) {
            try {
                SeerRecipe converted = convert(recipe, access, candidates);
                if (converted != null) result.add(converted);
            } catch (Throwable t) {
                Seer.LOGGER.warn("SEER: failed to convert recipe {}", recipe.getId(), t);
            }
        }
        return result;
    }

    private static SeerRecipe convert(Recipe<?> recipe, RegistryAccess access, List<SeerStack> candidates) {
        ItemStack result = recipe.getResultItem(access);
        if (result.isEmpty()) return null; // спецрецепты без фиксированного результата / special recipes without a fixed result
        List<SeerStack> outputs = List.of(SeerStack.of(result));

        if (recipe instanceof ShapedRecipe shaped) {
            return new SeerRecipe(recipe.getId(), SeerCategory.CRAFTING, ingredients(shaped.getIngredients()),
                    outputs, shaped.getWidth(), shaped.getHeight(), 0, 0f);
        }
        if (recipe instanceof ShapelessRecipe) {
            return new SeerRecipe(recipe.getId(), SeerCategory.CRAFTING, ingredients(recipe.getIngredients()),
                    outputs, 0, 0, 0, 0f);
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            RecipeType<?> type = recipe.getType();
            SeerCategory category = type == RecipeType.BLASTING ? SeerCategory.BLASTING
                    : type == RecipeType.SMOKING ? SeerCategory.SMOKING
                      : type == RecipeType.CAMPFIRE_COOKING ? SeerCategory.CAMPFIRE
                        : SeerCategory.SMELTING;
            return new SeerRecipe(recipe.getId(), category, ingredients(recipe.getIngredients()),
                    outputs, 0, 0, cooking.getCookingTime(), cooking.getExperience());
        }
        if (recipe instanceof StonecutterRecipe) {
            return new SeerRecipe(recipe.getId(), SeerCategory.STONECUTTING, ingredients(recipe.getIngredients()),
                    outputs, 0, 0, 0, 0f);
        }
        if (recipe instanceof SmithingTransformRecipe smithing) {
            return new SeerRecipe(recipe.getId(), SeerCategory.SMITHING, smithingInputs(smithing, candidates),
                    outputs, 3, 1, 0, 0f);
        }
        return null;
    }

    // Входы кузнечного стола недоступны напрямую, поэтому проверяем каждый предмет индекса.
    // Smithing inputs are not directly accessible, so we test every item in the index.
    private static List<SeerIngredient> smithingInputs(SmithingTransformRecipe recipe, List<SeerStack> candidates) {
        List<SeerStack> templates = new ArrayList<>();
        List<SeerStack> bases = new ArrayList<>();
        List<SeerStack> additions = new ArrayList<>();
        for (SeerStack s : candidates) {
            if (s.kind() != SeerStack.Kind.ITEM) continue;
            ItemStack stack = s.itemStack();
            if (recipe.isTemplateIngredient(stack)) templates.add(s);
            if (recipe.isBaseIngredient(stack)) bases.add(s);
            if (recipe.isAdditionIngredient(stack)) additions.add(s);
        }
        return List.of(slot(templates), slot(bases), slot(additions));
    }

    private static SeerIngredient slot(List<SeerStack> list) {
        return list.isEmpty() ? SeerIngredient.EMPTY : new SeerIngredient(list, 1f);
    }

    private static List<SeerIngredient> ingredients(List<Ingredient> source) {
        List<SeerIngredient> list = new ArrayList<>(source.size());
        for (Ingredient i : source) list.add(SeerIngredient.of(i));
        return list;
    }
}