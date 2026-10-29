package dev.seer.network;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Клиент просит разложить рецепт в открытом контейнере. / The client asks to place a recipe into the open container.
 *
 * @param max Shift: класть максимум, сколько позволяют предметы / Shift: place as many as the items allow
 */
public record TransferPacket(ResourceLocation recipeId, boolean max) {
    public void encode(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(recipeId);
        buffer.writeBoolean(max);
    }

    public static TransferPacket decode(FriendlyByteBuf buffer) {
        return new TransferPacket(buffer.readResourceLocation(), buffer.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) RecipeTransfer.apply(player, recipeId, max);
        });
        context.setPacketHandled(true);
    }
}