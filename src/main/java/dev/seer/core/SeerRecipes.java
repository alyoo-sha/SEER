package dev.seer.core;

import dev.seer.api.PluginRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.*;

/**
 * Все рецепты, категории и станки с поиском по выходу (R) и по входу (U).
 * All recipes, categories and workstations with lookups by output (R) and input (U).
 */
public final class SeerRecipes {
    private static volatile List<SeerRecipe> all = List.of();
    private static volatile List<SeerCategory> categories = SeerCategory.ALL;
    private static volatile Map<SeerCategory, List<SeerStack>> workstations = Map.of();
    private static volatile Map<SeerStack, List<SeerRecipe>> byOutput = Map.of();
    private static volatile Map<SeerStack, List<SeerRecipe>> byInput = Map.of();
    private static volatile Map<SeerCategory, List<SeerRecipe>> byCategory = Map.of();
    private static volatile Map<SeerStack, List<SeerCategory>> servedBy = Map.of();

    private SeerRecipes() {}

    public static List<SeerRecipe> all() { return all; }
    public static List<SeerCategory> categories() { return categories; }

    /** Станки категории, включая добавленные плагинами. / Category workstations, including plugin-added ones. */
    public static List<SeerStack> workstations(SeerCategory category) {
        return workstations.getOrDefault(category, category.workstations());
    }

    /** Рецепты, дающие этот предмет (клавиша R). / Recipes that produce this stack (R key). */
    public static List<SeerRecipe> recipesFor(SeerStack stack) {
        return byOutput.getOrDefault(stack, List.of());
    }

    /** Рецепты, где этот предмет используется (клавиша U). / Recipes that use this stack (U key). */
    public static List<SeerRecipe> usesOf(SeerStack stack) {
        return byInput.getOrDefault(stack, List.of());
    }

    /** Все рецепты категории (что делается на станке). / All recipes of a category (what a machine makes). */
    public static List<SeerRecipe> recipesIn(SeerCategory category) {
        return byCategory.getOrDefault(category, List.of());
    }

    /** Категории, для которых этот предмет - станок. / Categories this stack is a workstation for. */
    public static List<SeerCategory> categoriesServedBy(SeerStack stack) {
        return servedBy.getOrDefault(stack, List.of());
    }

    /** Вызывать после SeerIndex.rebuild: кузнечный стол использует индекс. / Call after SeerIndex.rebuild: smithing uses the index. */
    public static void rebuild(RecipeManager manager, RegistryAccess access, PluginRegistry plugins) {
        List<SeerRecipe> list = new ArrayList<>(VanillaRecipeLoader.load(manager, access, SeerIndex.all()));
        list.addAll(plugins.recipes());

        List<SeerCategory> cats = new ArrayList<>(SeerCategory.ALL);
        cats.addAll(plugins.categories());

        Map<SeerCategory, List<SeerStack>> ws = new HashMap<>();
        for (SeerCategory c : cats) ws.put(c, new ArrayList<>(c.workstations()));
        plugins.workstations().forEach((c, extra) -> ws.computeIfAbsent(c, k -> new ArrayList<>()).addAll(extra));

        Map<SeerStack, List<SeerRecipe>> out = new HashMap<>();
        Map<SeerStack, List<SeerRecipe>> in = new HashMap<>();
        for (SeerRecipe r : list) {
            Set<SeerStack> seenOut = new HashSet<>();
            for (SeerStack s : r.outputs()) {
                if (seenOut.add(s)) out.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
            }
            // Один рецепт не должен попасть в список входа дважды.
            // A recipe must not appear twice in an input's list.
            Set<SeerStack> seenIn = new HashSet<>();
            for (SeerIngredient ing : r.inputs()) {
                for (SeerStack s : ing.alternatives()) {
                    if (seenIn.add(s)) in.computeIfAbsent(s, k -> new ArrayList<>()).add(r);
                }
            }
        }

        Map<SeerCategory, List<SeerRecipe>> inCategory = new HashMap<>();
        for (SeerRecipe r : list) inCategory.computeIfAbsent(r.category(), k -> new ArrayList<>()).add(r);
        Map<SeerStack, List<SeerCategory>> served = new HashMap<>();
        for (SeerCategory c : cats) {
            for (SeerStack station : ws.get(c)) served.computeIfAbsent(station, k -> new ArrayList<>()).add(c);
        }

        all = List.copyOf(list);
        categories = List.copyOf(cats);
        workstations = ws;
        byOutput = out;
        byInput = in;
        byCategory = inCategory;
        servedBy = served;
    }
}