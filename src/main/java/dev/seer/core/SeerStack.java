package dev.seer.core;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SeerStack {
    public enum Kind { ITEM, FLUID }

    private final Kind kind;
    private final ItemStack item;
    private final FluidStack fluid;
    private final ResourceLocation id;
    private final int hash;

    private String name, mod, tooltip;
    private List<String> tags;

    private SeerStack(Kind kind, ItemStack item, FluidStack fluid, ResourceLocation id) {
        this.kind = kind;
        this.item = item;
        this.fluid = fluid;
        this.id = id;
        this.hash = Objects.hash(kind, id, nbt());
    }

    public static SeerStack of(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return new SeerStack(Kind.ITEM, copy, FluidStack.EMPTY, ForgeRegistries.ITEMS.getKey(copy.getItem()));
    }

    public static SeerStack of(FluidStack stack) {
        FluidStack copy = stack.copy();
        copy.setAmount(1000);
        return new SeerStack(Kind.FLUID, ItemStack.EMPTY, copy, ForgeRegistries.FLUIDS.getKey(copy.getFluid()));
    }

    public Kind kind() { return kind; }
    public ResourceLocation id() { return id; }
    public ItemStack itemStack() { return item.copy(); }
    public FluidStack fluidStack() { return fluid.copy(); }

    public Component displayName() {
        return kind == Kind.ITEM ? item.getHoverName() : fluid.getDisplayName();
    }

    // ---- поля для поиска (лениво, в нижнем регистре) ----

    public String searchName() {
        if (name == null) name = displayName().getString().toLowerCase(Locale.ROOT);
        return name;
    }

    public String searchMod() {
        if (mod == null) {
            String ns = id.getNamespace();
            String shown = ModList.get().getModContainerById(ns)
                    .map(c -> c.getModInfo().getDisplayName()).orElse(ns);
            mod = (ns + " " + shown).toLowerCase(Locale.ROOT);
        }
        return mod;
    }

    public String searchTooltip() {
        if (tooltip == null) {
            try {
                if (kind == Kind.ITEM) {
                    StringBuilder sb = new StringBuilder();
                    for (Component c : item.getTooltipLines(Minecraft.getInstance().player, TooltipFlag.NORMAL)) {
                        sb.append(c.getString()).append('\n');
                    }
                    tooltip = sb.toString().toLowerCase(Locale.ROOT);
                } else {
                    tooltip = searchName();
                }
            } catch (Throwable t) {
                tooltip = searchName(); // кривые тултипы чужих модов не должны ронять поиск
            }
        }
        return tooltip;
    }

    public List<String> searchTags() {
        if (tags == null) {
            tags = (kind == Kind.ITEM
                    ? item.getTags()
                    : fluid.getFluid().builtInRegistryHolder().tags())
                    .map(t -> t.location().toString().toLowerCase(Locale.ROOT))
                    .toList();
        }
        return tags;
    }

    private CompoundTag nbt() {
        return kind == Kind.ITEM ? item.getTag() : fluid.getTag();
    }

    @Override public boolean equals(Object o) {
        return o instanceof SeerStack s && s.kind == kind && s.id.equals(id) && Objects.equals(nbt(), s.nbt());
    }

    @Override public int hashCode() { return hash; }
}
