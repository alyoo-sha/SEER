package dev.seer.core;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

/**
 * Слот входа рецепта: список взаимозаменяемых вариантов (например, содержимое тега).
 * A recipe input slot: a list of interchangeable alternatives (e.g. the contents of a tag).
 */
public final class SeerIngredient {
    public static final SeerIngredient EMPTY = new SeerIngredient(List.of(), 1f);

    private final List<SeerStack> alternatives;
    private final float chance; // шанс расхода / consumption chance

    public SeerIngredient(List<SeerStack> alternatives, float chance) {
        this.alternatives = List.copyOf(alternatives);
        this.chance = chance;
    }

    public static SeerIngredient of(SeerStack stack) {
        return new SeerIngredient(List.of(stack), 1f);
    }

    public static SeerIngredient of(Ingredient ingredient) {
        if (ingredient.isEmpty()) return EMPTY;
        List<SeerStack> list = new ArrayList<>();
        for (ItemStack s : ingredient.getItems()) {
            if (!s.isEmpty()) list.add(SeerStack.of(s));
        }
        return list.isEmpty() ? EMPTY : new SeerIngredient(list, 1f);
    }

    public boolean isEmpty() { return alternatives.isEmpty(); }
    public List<SeerStack> alternatives() { return alternatives; }
    public float chance() { return chance; }

    /** Количество не учитывается. / The amount is ignored. */
    public boolean contains(SeerStack stack) { return alternatives.contains(stack); }
}
