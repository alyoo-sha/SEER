package dev.seer;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Клиентский конфиг SEER. / SEER client config.
 */
public final class SeerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue OVERLAY_VISIBLE;
    public static final ForgeConfigSpec.IntValue OPACITY;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("overlay");
        OVERLAY_VISIBLE = builder
                .comment("Show the SEER item panel / Показывать панель предметов SEER")
                .define("visible", true);
        builder.pop();
        // внутри static-блока после OVERLAY_VISIBLE: / inside the static block after OVERLAY_VISIBLE:
        OPACITY = builder
                .comment("Panel opacity in percent / Прозрачность панелей в процентах")
                .defineInRange("opacity", 85, 30, 100);
        SPEC = builder.build();
    }

    private SeerConfig() {}
}
