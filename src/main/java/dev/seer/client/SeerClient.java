package dev.seer.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.seer.Seer;
import dev.seer.core.SeerIndex;
import dev.seer.core.SeerStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

public final class SeerClient {
    private SeerClient() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(SeerClient::onRecipesUpdated);
        MinecraftForge.EVENT_BUS.addListener(SeerClient::onCommands);
    }

    private static void onRecipesUpdated(RecipesUpdatedEvent e) {
        long t = System.nanoTime();
        SeerIndex.rebuild();
        Seer.LOGGER.info("SEER index: {} entries, {} ms",
                SeerIndex.all().size(), (System.nanoTime() - t) / 1_000_000);
    }

    private static void onCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("seer")
                .then(Commands.argument("query", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            List<SeerStack> r = SeerIndex.search(StringArgumentType.getString(ctx, "query"));
                            ctx.getSource().sendSuccess(() -> Component.literal("SEER: " + r.size() + " results"), false);
                            r.stream().limit(10).forEach(s -> ctx.getSource()
                                    .sendSuccess(() -> Component.literal(" - " + s.id()), false));
                            return r.size();
                        })
                )
        );
    }
}
