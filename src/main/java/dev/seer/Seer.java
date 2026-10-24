package dev.seer;

import com.mojang.logging.LogUtils;
import dev.seer.client.SeerClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Seer.MOD_ID)
public final class Seer {

    public static final String MOD_ID = "seer";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static IEventBus modBus;

    public Seer(FMLJavaModLoadingContext context) {
        modBus = context.getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SeerConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SeerClient::init);
    }
    public static ResourceLocation makeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
