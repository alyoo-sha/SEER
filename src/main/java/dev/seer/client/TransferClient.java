package dev.seer.client;


import dev.seer.core.SeerRecipe;
import dev.seer.network.RecipeTransfer;
import dev.seer.network.SeerNetwork;
import dev.seer.network.TransferPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.crafting.Recipe;

/**
 * Клиентская часть переноса рецепта. / The client side of recipe transfer.
 */
final class TransferClient {
    private TransferClient() {}

    static boolean serverHasSeer() {
        var connection = Minecraft.getInstance().getConnection();
        return connection != null && SeerNetwork.CHANNEL.isRemotePresent(connection.getConnection());
    }

    /** Можно ли отправить рецепт в контейнер, который открыт под экраном рецептов. / Whether the recipe can be sent to the container under the recipes screen. */
    static boolean available(Screen parent, SeerRecipe recipe) {
        if (recipe.id() == null || !(parent instanceof AbstractContainerScreen<?> gui) || !serverHasSeer()) return false;
        var level = Minecraft.getInstance().level;
        if (level == null) return false;
        Recipe<?> vanilla = level.getRecipeManager().byKey(recipe.id()).orElse(null);
        return vanilla != null && RecipeTransfer.supports(gui.getMenu(), vanilla);
    }

    static void send(SeerRecipe recipe, boolean max) {
        SeerNetwork.CHANNEL.sendToServer(new TransferPacket(recipe.id(), max));
    }
}