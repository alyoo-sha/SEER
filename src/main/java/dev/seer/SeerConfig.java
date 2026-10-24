package dev.seer;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Клиентский конфиг SEER. / SEER client config.
 */
public final class SeerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_VISIBLE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("overlay");
        OVERLAY_VISIBLE = builder
                .comment("Show the SEER item panel / Показывать панель предметов SEER")
                .define("visible", true);
        builder.pop();
        SPEC = builder.build();
    }

    private SeerConfig() {}
}
