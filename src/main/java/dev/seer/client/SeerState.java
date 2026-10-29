package dev.seer.client;


import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import dev.seer.Seer;
import dev.seer.SeerConfig;

/**
 * Общее состояние интерфейса, связанное с конфигом.
 * Shared UI state backed by the config.
 */
public final class SeerState {
    public static final Signal<Boolean> VISIBLE = Signals.of(true);
    /** Непрозрачность панелей в процентах (30-100). / Panel opacity in percent (30-100). */
    public static final Signal<Double> OPACITY = Signals.of(85.0);

    // Пока конфиг не загружен, в него ничего не пишем.
    // Nothing is written to the config until it has been loaded.
    private static boolean ready = false;

    static {
        VISIBLE.subscribe(value -> {
            if (!ready) return;
            SeerConfig.OVERLAY_VISIBLE.set(value);
            SeerConfig.OVERLAY_VISIBLE.save();
        });
        // Цвета обновляются сразу при каждом движении слайдера, а файл пишется один раз в flush().
        // Colors update on every slider move, the file is written once in flush().
        OPACITY.subscribe(value -> {
            Palette.setOpacity((float) (value / 100.0));
            if (ready) SeerConfig.OPACITY.set((int) Math.round(value));
        });
    }

    private SeerState() {}

    public static void init() {
        VISIBLE.set(SeerConfig.OVERLAY_VISIBLE.get());
        OPACITY.set((double) SeerConfig.OPACITY.get());
        ready = true;
        Seer.LOGGER.info("SEER: panel opacity {}%", SeerConfig.OPACITY.get());
    }

    /** Сохраняет прозрачность в файл (вызывается при закрытии настроек). / Saves the opacity to the file (called when the settings close). */
    public static void flush() {
        if (ready) SeerConfig.OPACITY.save();
    }
}