package dev.seer.api;

import dev.seer.core.SeerCategory;
import dev.seer.core.SeerRecipe;
import dev.seer.core.SeerStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Собирает данные, которые плагины добавили за один проход.
 * Collects the data plugins contributed during one pass.
 */
public final class PluginRegistry implements SeerRegistry {
    private final List<SeerCategory> categories = new ArrayList<>();
    private final Map<SeerCategory, List<SeerStack>> workstations = new HashMap<>();
    private final List<SeerRecipe> recipes = new ArrayList<>();
    private final List<SeerStack> stacks = new ArrayList<>();

    @Override public void addCategory(SeerCategory category) { categories.add(category); }

    @Override public void addWorkstation(SeerCategory category, SeerStack workstation) {
        workstations.computeIfAbsent(category, k -> new ArrayList<>()).add(workstation);
    }

    @Override public void addRecipe(SeerRecipe recipe) { recipes.add(recipe); }
    @Override public void addStack(SeerStack stack) { stacks.add(stack); }

    public List<SeerCategory> categories() { return categories; }
    public Map<SeerCategory, List<SeerStack>> workstations() { return workstations; }
    public List<SeerRecipe> recipes() { return recipes; }
    public List<SeerStack> stacks() { return stacks; }
}
