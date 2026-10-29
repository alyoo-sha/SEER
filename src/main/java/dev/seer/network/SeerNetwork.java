package dev.seer.network;

import dev.seer.Seer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Сетевой канал SEER. / The SEER network channel.
 */
public final class SeerNetwork {
    private static final String PROTOCOL = "1";

    // Мод необязателен на другой стороне: ванильный сервер или клиент без SEER тоже подключатся.
    // The mod is optional on the other side: a vanilla server or a client without SEER can still connect.
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Seer.makeId("main"),
            () -> PROTOCOL,
            NetworkRegistry.acceptMissingOr(PROTOCOL),
            NetworkRegistry.acceptMissingOr(PROTOCOL));

    private SeerNetwork() {}

    public static void init() {
        CHANNEL.registerMessage(0, TransferPacket.class,
                TransferPacket::encode, TransferPacket::decode, TransferPacket::handle);
    }
}