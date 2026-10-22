package dev.seer.api;

import dev.seer.core.SeerCategory;
import dev.seer.core.SeerRecipe;
import dev.seer.core.SeerStack;

/**
 * Реестр, в который плагины добавляют свои данные.
 * The registry plugins use to contribute their data.
 */
public interface SeerRegistry {
    /** Новая категория рецептов. / A new recipe category. */
    void addCategory(SeerCategory category);

    /** Станок для категории (встроенной или своей). / A workstation for a category (built-in or custom). */
    void addWorkstation(SeerCategory category, SeerStack workstation);

    /** Рецепт. / A recipe. */
    void addRecipe(SeerRecipe recipe);

    /** Дополнительный стек в индекс (например, зелье с NBT). / An extra index stack (e.g. a potion with NBT). */
    void addStack(SeerStack stack);
}
