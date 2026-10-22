package dev.seer.core;

import dev.seer.Seer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Категория рецептов (вкладка на экране рецептов) со списком станков.
 * A recipe category (a tab on the recipe screen) with its workstations.
 */
public final class SeerCategory {
    public static final SeerCategory CRAFTING = new SeerCategory("crafting", "block.minecraft.crafting_table", Items.CRAFTING_TABLE);
    public static final SeerCategory SMELTING = new SeerCategory("smelting", "block.minecraft.furnace", Items.FURNACE);
    public static final SeerCategory BLASTING = new SeerCategory("blasting", "block.minecraft.blast_furnace", Items.BLAST_FURNACE);
    public static final SeerCategory SMOKING = new SeerCategory("smoking", "block.minecraft.smoker", Items.SMOKER);
    public static final SeerCategory CAMPFIRE = new SeerCategory("campfire", "block.minecraft.campfire", Items.CAMPFIRE);
    public static final SeerCategory STONECUTTING = new SeerCategory("stonecutting", "block.minecraft.stonecutter", Items.STONECUTTER);
    public static final SeerCategory SMITHING = new SeerCategory("smithing", "block.minecraft.smithing_table", Items.SMITHING_TABLE);

    public static final List<SeerCategory> ALL = List.of(CRAFTING, SMELTING, BLASTING, SMOKING, CAMPFIRE, STONECUTTING, SMITHING);

    private final ResourceLocation id;
    private final Component name;
    private final SeerStack icon;
    private final List<SeerStack> workstations;

    // Внутренний конструктор для встроенных категорий.
    // Internal constructor for built-in categories.
    private SeerCategory(String path, String nameKey, ItemLike icon) {
        this(Seer.makeId(path), Component.translatable(nameKey), SeerStack.of(new ItemStack(icon)));
    }

    // Публичные конструкторы для плагинов (станок по умолчанию - сама иконка).
    // Public constructors for plugins (the default workstation is the icon itself).
    public SeerCategory(ResourceLocation id, Component name, SeerStack icon) {
        this(id, name, icon, List.of(icon));
    }

    public SeerCategory(ResourceLocation id, Component name, SeerStack icon, List<SeerStack> workstations) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.workstations = List.copyOf(workstations);
    }

    public ResourceLocation id() { return id; }
    public Component name() { return name; }
    public SeerStack icon() { return icon; }
    public List<SeerStack> workstations() { return workstations; }
}
