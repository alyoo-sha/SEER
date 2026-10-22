package dev.seer.core;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Рецепт в модели SEER. Пустые слоты сетки хранятся как SeerIngredient.EMPTY, чтобы сохранить позиции.
 * A recipe in the SEER model. Empty grid slots are stored as SeerIngredient.EMPTY to keep positions.
 *
 * @param gridWidth  ширина сетки, 0 если рецепт не сеточный / grid width, 0 if the recipe is not grid-based
 * @param cookTime   время готовки в тиках, 0 если неприменимо / cooking time in ticks, 0 if n/a
 * @param experience опыт за результат, 0 если неприменимо / experience for the result, 0 if n/a
 */
public record SeerRecipe(
        ResourceLocation id,
        SeerCategory category,
        List<SeerIngredient> inputs,
        List<SeerStack> outputs,
        int gridWidth,
        int gridHeight,
        int cookTime,
        float experience) {

    public SeerRecipe {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }
}
