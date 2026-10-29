package dev.seer.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Предмет или жидкость с количеством. Идентичность (equals/hashCode) не учитывает количество.
 * An item or fluid with an amount. Identity (equals/hashCode) ignores the amount.
 */
public final class SeerStack {
    public enum Kind { ITEM, FLUID }

    private final Kind kind;
    private final ItemStack item;   // всегда count = 1 / always count = 1
    private final FluidStack fluid; // всегда amount = 1 / always amount = 1
    private final ResourceLocation id;
    private final long amount;
    private final int hash;

    private SeerStack(Kind kind, ItemStack item, FluidStack fluid, ResourceLocation id, long amount) {
        this.kind = kind;
        this.item = item;
        this.fluid = fluid;
        this.id = id;
        this.amount = amount;
        this.hash = Objects.hash(kind, id, nbt());
    }

    /** Количество берётся из stack.getCount(). / Amount is taken from stack.getCount(). */
    public static SeerStack of(ItemStack stack) {
        ItemStack one = stack.copy();
        one.setCount(1);
        return new SeerStack(Kind.ITEM, one, FluidStack.EMPTY, ForgeRegistries.ITEMS.getKey(one.getItem()), stack.getCount());
    }

    /** Количество берётся из stack.getAmount() (в mB). / Amount is taken from stack.getAmount() (in mB). */
    public static SeerStack of(FluidStack stack) {
        FluidStack one = stack.copy();
        one.setAmount(1);
        return new SeerStack(Kind.FLUID, ItemStack.EMPTY, one, ForgeRegistries.FLUIDS.getKey(one.getFluid()), stack.getAmount());
    }

    public Kind kind() { return kind; }
    public ResourceLocation id() { return id; }
    public long amount() { return amount; }

    /** Копия с другим количеством. / A copy with a different amount. */
    public SeerStack withAmount(long newAmount) {
        return new SeerStack(kind, item, fluid, id, newAmount);
    }

    /** Новый ItemStack с count = amount. / A new ItemStack with count = amount. */
    public ItemStack itemStack() {
        ItemStack copy = item.copy();
        copy.setCount((int) Math.min(amount, Integer.MAX_VALUE));
        return copy;
    }

    /** Новый FluidStack с amount. / A new FluidStack with the amount. */
    public FluidStack fluidStack() {
        FluidStack copy = fluid.copy();
        copy.setAmount((int) Math.min(amount, Integer.MAX_VALUE));
        return copy;
    }

    /** Копия NBT или null. / A copy of the NBT or null. */
    public CompoundTag tag() {
        CompoundTag tag = nbt();
        return tag == null ? null : tag.copy();
    }

    /** Тип предмета без копирования стека. / The item type without copying the stack. */
    public Item itemType() {
        return item.getItem();
    }

    public Component displayName() {
        return kind == Kind.ITEM ? item.getHoverName() : fluid.getDisplayName();
    }

    private CompoundTag nbt() {
        return kind == Kind.ITEM ? item.getTag() : fluid.getTag();
    }

    @Override public boolean equals(Object o) {
        return o instanceof SeerStack s && s.kind == kind && s.id.equals(id) && Objects.equals(nbt(), s.nbt());
    }

    @Override public int hashCode() { return hash; }
}
