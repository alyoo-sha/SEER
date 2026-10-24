package dev.seer.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Зоны, которые панель SEER не должна перекрывать (кнопки и панели других модов).
 * Zones the SEER panel must not cover (buttons and panels of other mods).
 */
public final class SeerExclusionZones {
    private record Entry(Class<?> type, Function<Screen, List<Rect2i>> provider) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    private SeerExclusionZones() {}

    @SuppressWarnings("unchecked")
    public static <T extends Screen> void register(Class<T> type, Function<T, List<Rect2i>> provider) {
        ENTRIES.add(new Entry(type, screen -> provider.apply((T) screen)));
    }

    static List<Rect2i> zonesFor(Screen screen) {
        List<Rect2i> zones = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.type().isInstance(screen)) zones.addAll(entry.provider().apply(screen));
        }
        return zones;
    }
}
