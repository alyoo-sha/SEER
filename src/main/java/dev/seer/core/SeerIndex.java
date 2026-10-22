package dev.seer.core;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class SeerIndex {
    private static volatile List<SeerStack> all = List.of();

    private SeerIndex() {}

    public static List<SeerStack> all() { return all; }

    public static void rebuild() {
        List<SeerStack> list = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS) {
            if (item != Items.AIR) list.add(SeerStack.of(new ItemStack(item)));
        }
        for (Fluid fluid : ForgeRegistries.FLUIDS) {
            if (fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState())) {
                list.add(SeerStack.of(new FluidStack(fluid, 1000)));
            }
        }
        all = List.copyOf(list);
    }

    public static List<SeerStack> search(String text) {
        SearchQuery q = SearchQuery.parse(text);
        if (q.isEmpty()) return all;
        return all.stream().filter(q::test).toList();
    }
}
