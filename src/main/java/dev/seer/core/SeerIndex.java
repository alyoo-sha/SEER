package dev.seer.core;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.stream.Stream;

/**
 * Индекс всех предметов и жидкостей. Поисковые поля считаются сразу при сборке.
 * Index of all items and fluids. Search fields are computed up front at build time.
 */
public final class SeerIndex {

    /** Запись индекса с готовыми полями поиска. / Index entry with ready-made search fields. */
    public record Entry(SeerStack stack, String name, String mod, String tooltip, List<String> tags) {}

    private static volatile List<Entry> entries = List.of();
    private static volatile List<SeerStack> stacks = List.of();

    private SeerIndex() {}

    public static List<SeerStack> all() { return stacks; }

    private static volatile Set<String> mods = Set.of();
    private static volatile Set<String> tags = Set.of();

    /** Все modId для подсказок. / All mod ids for suggestions. */
    public static Set<String> mods() { return mods; }

    /** Все теги для подсказок. / All tags for suggestions. */
    public static Set<String> tags() { return tags; }

    /**
     * @param creative стеки творческих вкладок (варианты NBT, порядок креатива)
     *                 creative tab stacks (NBT variants, creative order)
     * @param extra    дополнительные стеки от плагинов / extra stacks from plugins
     */
    public static void rebuild(List<SeerStack> creative, List<SeerStack> extra) {
        List<Entry> list = new ArrayList<>();
        Set<SeerStack> seen = new HashSet<>();
        Set<Item> covered = new HashSet<>();

        // Сначала креатив: так порядок знакомый, а у зелий и книг есть все варианты.
        // Creative first: the order is familiar and potions and books get all their variants.
        for (SeerStack s : creative) {
            if (s.kind() == SeerStack.Kind.ITEM && seen.add(s)) {
                list.add(makeEntry(s));
                covered.add(s.itemType());
            }
        }
        // Предметы вне вкладок, а у покрытых предметов "пустой" вариант без NBT не нужен.
        // Items outside the tabs; covered items do not need a plain NBT-less variant.
        for (Item item : ForgeRegistries.ITEMS) {
            if (item == Items.AIR || covered.contains(item)) continue;
            SeerStack s = SeerStack.of(new ItemStack(item));
            if (seen.add(s)) list.add(makeEntry(s));
        }
        for (Fluid fluid : ForgeRegistries.FLUIDS) {
            if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) continue;
            SeerStack s = SeerStack.of(new FluidStack(fluid, 1000));
            if (seen.add(s)) list.add(makeEntry(s));
        }
        for (SeerStack s : extra) {
            if (seen.add(s)) list.add(makeEntry(s));
        }

        Set<String> modSet = new TreeSet<>();
        Set<String> tagSet = new TreeSet<>();
        Map<SeerStack, Entry> lookup = new HashMap<>();
        for (Entry e : list) {
            modSet.add(e.stack().id().getNamespace());
            tagSet.addAll(e.tags());
            lookup.put(e.stack(), e);
        }
        entries = List.copyOf(list);
        stacks = list.stream().map(Entry::stack).toList();
        mods = Collections.unmodifiableSet(modSet);
        tags = Collections.unmodifiableSet(tagSet);
        byStack = lookup;
    }

    /** Есть ли такой стек в индексе (с учётом NBT). / Whether the index has this stack (NBT included). */
    public static boolean contains(SeerStack stack) {
        return byStack.containsKey(stack);
    }

    public static List<SeerStack> search(String text) {
        SearchQuery q = SearchQuery.parse(text);
        if (q.isEmpty()) return stacks;
        return entries.stream().filter(q::test).map(Entry::stack).toList();
    }

    private static Entry makeEntry(SeerStack s) {
        String name = s.displayName().getString().toLowerCase(Locale.ROOT);

        String ns = s.id().getNamespace();
        String shown = ModList.get().getModContainerById(ns)
                .map(c -> c.getModInfo().getDisplayName()).orElse(ns);
        String mod = (ns + " " + shown).toLowerCase(Locale.ROOT);

        String tooltip = name;
        List<String> tags;
        if (s.kind() == SeerStack.Kind.ITEM) {
            ItemStack stack = s.itemStack();
            tooltip = tooltipOf(stack, name);
            tags = tagNames(stack.getTags());
        } else {
            tags = tagNames(s.fluidStack().getFluid().builtInRegistryHolder().tags());
        }
        return new Entry(s, name, mod, tooltip, tags);
    }

    private static volatile Map<SeerStack, Entry> byStack = Map.of();

    /** Поиск внутри заданного набора. Пустой запрос возвращает набор целиком. / Search within a given set. An empty query returns the whole set. */
    public static List<SeerStack> filter(Collection<SeerStack> subset, String text) {
        SearchQuery q = SearchQuery.parse(text);
        if (q.isEmpty()) return List.copyOf(subset);
        Map<SeerStack, Entry> lookup = byStack;
        return subset.stream().filter(stack -> {
            Entry entry = lookup.get(stack);
            return entry != null && q.test(entry);
        }).toList();
    }

    private static String tooltipOf(ItemStack stack, String fallback) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Component c : stack.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.NORMAL)) {
                sb.append(c.getString()).append('\n');
            }
            return sb.toString().toLowerCase(Locale.ROOT);
        } catch (Throwable t) {
            // Кривой тултип чужого мода не должен ронять индекс.
            // A broken tooltip from another mod must not crash the index.
            return fallback;
        }
    }

    private static List<String> tagNames(Stream<? extends TagKey<?>> tags) {
        return tags.map(t -> t.location().toString().toLowerCase(Locale.ROOT)).toList();
    }
}
