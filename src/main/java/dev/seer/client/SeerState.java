package dev.seer.client;

import com.nstut.openui.state.Signal;
import com.nstut.openui.state.Signals;
import dev.seer.SeerConfig;

/**
 * Общее состояние интерфейса, связанное с конфигом.
 * Shared UI state backed by the config.
 */
public final class SeerState {
    public static final Signal<Boolean> VISIBLE = Signals.of(true);

    // Пока конфиг не загружен, в него ничего не пишем.
    // Nothing is written to the config until it has been loaded.
    private static boolean ready = false;

    static {
        VISIBLE.subscribe(value -> {
            if (!ready) return;
            SeerConfig.OVERLAY_VISIBLE.set(value);
            SeerConfig.OVERLAY_VISIBLE.save();
        });
    }

    private SeerState() {}

    public static void init() {
        VISIBLE.set(SeerConfig.OVERLAY_VISIBLE.get());
        ready = true;
    }
}
