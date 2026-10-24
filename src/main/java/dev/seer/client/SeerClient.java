package dev.seer.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.seer.Seer;
import dev.seer.api.PluginRegistry;
import dev.seer.api.SeerPlugin;
import dev.seer.core.SeerIndex;
import dev.seer.core.SeerPluginLoader;
import dev.seer.core.SeerRecipes;
import dev.seer.core.SeerStack;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static dev.seer.Seer.modBus;

public final class SeerClient {
    // Плагины находятся один раз при старте клиента.
    // Plugins are discovered once at client startup.
    private static volatile List<SeerPlugin> plugins = List.of();

    private SeerClient() {}

    public static void init() {
        modBus.addListener(SeerClient::onClientSetup);
        MinecraftForge.EVENT_BUS.addListener(SeerClient::onRecipesUpdated);
        MinecraftForge.EVENT_BUS.addListener(SeerClient::onCommands);
    }

    private static void onClientSetup(FMLClientSetupEvent e) {
        plugins = List.copyOf(SeerPluginLoader.discover());
        Seer.LOGGER.info("SEER: {} plugin(s) discovered", plugins.size());
        e.enqueueWork(SeerState::init); // читаем конфиг после его загрузки / read the config after it is loaded
    }

    private static void onRecipesUpdated(RecipesUpdatedEvent e) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return;

        long t = System.nanoTime();
        PluginRegistry registry = new PluginRegistry();
        for (SeerPlugin plugin : plugins) {
            try {
                plugin.register(registry);
            } catch (Throwable ex) {
                Seer.LOGGER.error("SEER: plugin {} failed", plugin.getClass().getName(), ex);
            }
        }
        SeerIndex.rebuild(registry.stacks());
        long indexMs = (System.nanoTime() - t) / 1_000_000;
        SeerRecipes.rebuild(e.getRecipeManager(), connection.registryAccess(), registry);

        Seer.LOGGER.info("SEER: index {} entries ({} ms), recipes {}",
                SeerIndex.all().size(), indexMs, SeerRecipes.all().size());
    }

    // Тестовые команды, пока нет UI. / Test commands until the UI exists.
    private static void onCommands(RegisterClientCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("seer")
                .then(Commands.argument("query", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> suggestQuery(builder))
                        .executes(ctx -> {
                            List<SeerStack> r = SeerIndex.search(StringArgumentType.getString(ctx, "query"));
                            ctx.getSource().sendSuccess(() -> Component.literal("SEER: " + r.size() + " results"), false);
                            r.stream().limit(10).forEach(s -> ctx.getSource()
                                    .sendSuccess(() -> Component.literal(" - " + s.id()), false));
                            return r.size();
                        })));

        e.getDispatcher().register(Commands.literal("seerinfo")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                        .suggests((ctx, builder) ->
                                SharedSuggestionProvider.suggestResource(ForgeRegistries.ITEMS.getKeys(), builder))
                        .executes(ctx -> {
                            ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
                            Item item = ForgeRegistries.ITEMS.getValue(id);
                            if (item == null || item == Items.AIR) {
                                ctx.getSource().sendFailure(Component.literal("SEER: unknown item " + id));
                                return 0;
                            }
                            SeerStack stack = SeerStack.of(new ItemStack(item));
                            var recipes = SeerRecipes.recipesFor(stack);
                            var uses = SeerRecipes.usesOf(stack);
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "SEER: " + id + " recipes=" + recipes.size() + " uses=" + uses.size()), false);
                            recipes.stream().limit(5).forEach(r -> ctx.getSource().sendSuccess(
                                    () -> Component.literal(" - " + r.category().id().getPath() + " " + r.id()), false));
                            return recipes.size();
                        })));
    }

    // Подсказки по последнему слову запроса: @мод, #тег, либо префиксы.
    // Suggestions for the last word of the query: @mod, #tag, or the prefixes.
    private static CompletableFuture<Suggestions> suggestQuery(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int space = remaining.lastIndexOf(' ');
        String token = remaining.substring(space + 1).toLowerCase(Locale.ROOT);
        SuggestionsBuilder word = builder.createOffset(builder.getStart() + space + 1);

        if (token.startsWith("@")) {
            SharedSuggestionProvider.suggest(SeerIndex.mods().stream().map(m -> "@" + m), word);
        } else if (token.startsWith("#")) {
            SharedSuggestionProvider.suggest(SeerIndex.tags().stream().map(t -> "#" + t), word);
        } else if (token.isEmpty()) {
            SharedSuggestionProvider.suggest(List.of("@", "#", "$"), word);
        }
        return word.buildFuture();
    }
}
