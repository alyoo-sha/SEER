package dev.seer;

import com.mojang.logging.LogUtils;
import dev.seer.client.SeerClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Seer.MOD_ID)
public final class Seer {

    public static final String MOD_ID = "seer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Seer() {
//        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SeerClient::init);
    }
}
