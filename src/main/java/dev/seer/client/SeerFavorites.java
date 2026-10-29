package dev.seer.client;


import com.google.gson.*;
import dev.seer.Seer;
import dev.seer.core.SeerStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Избранное: порядок добавления сохраняется, список записывается в config/seer-favorites.json.
 * Favorites: insertion order is kept, the list is stored in config/seer-favorites.json.
 */
public final class SeerFavorites {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<SeerStack> favorites = new LinkedHashSet<>();
    private static List<SeerStack> snapshot = List.of();

    private SeerFavorites() {}

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("seer-favorites.json");
    }

    public static List<SeerStack> list() { return snapshot; }

    /** Количество не учитывается. / The amount is ignored. */
    public static boolean contains(SeerStack stack) { return favorites.contains(stack); }

    /** Добавляет или убирает. Возвращает true, если предмет добавлен. / Adds or removes. Returns true if added. */
    public static boolean toggle(SeerStack stack) {
        SeerStack key = stack.withAmount(1);
        boolean added = favorites.add(key);
        if (!added) favorites.remove(key);
        snapshot = List.copyOf(favorites);
        save();
        return added;
    }

    public static void load() {
        favorites.clear();
        Path file = file();
        if (Files.exists(file)) {
            try {
                JsonArray array = JsonParser.parseString(Files.readString(file)).getAsJsonArray();
                for (JsonElement element : array) {
                    SeerStack stack = decode(element.getAsString());
                    if (stack != null) favorites.add(stack);
                }
            } catch (Exception e) {
                Seer.LOGGER.warn("SEER: could not read the favorites file", e);
            }
        }
        snapshot = List.copyOf(favorites);
    }

    private static void save() {
        JsonArray array = new JsonArray();
        for (SeerStack stack : favorites) array.add(encode(stack));
        try {
            Files.writeString(file(), GSON.toJson(array));
        } catch (IOException e) {
            Seer.LOGGER.warn("SEER: could not save the favorites file", e);
        }
    }

    // Формат строки: ВИД|id|NBT. / Line format: KIND|id|NBT.
    private static String encode(SeerStack stack) {
        CompoundTag tag = stack.tag();
        return stack.kind().name() + "|" + stack.id() + "|" + (tag == null ? "" : tag.toString());
    }

    private static SeerStack decode(String line) {
        try {
            String[] parts = line.split("\\|", 3);
            if (parts.length < 2) return null;
            ResourceLocation id = ResourceLocation.tryParse(parts[1]);
            if (id == null) return null;
            CompoundTag tag = parts.length == 3 && !parts[2].isEmpty() ? TagParser.parseTag(parts[2]) : null;

            if (parts[0].equals(SeerStack.Kind.ITEM.name())) {
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item == null || item == Items.AIR) return null; // мод удалён / the mod was removed
                ItemStack stack = new ItemStack(item);
                if (tag != null) stack.setTag(tag);
                return SeerStack.of(stack);
            }
            if (parts[0].equals(SeerStack.Kind.FLUID.name())) {
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(id);
                if (fluid == null || fluid == Fluids.EMPTY) return null;
                FluidStack stack = new FluidStack(fluid, 1000);
                if (tag != null) stack.setTag(tag);
                return SeerStack.of(stack);
            }
        } catch (Exception e) {
            Seer.LOGGER.warn("SEER: skipped a broken favorite entry: {}", line);
        }
        return null;
    }
}