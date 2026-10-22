package dev.seer.core;

import dev.seer.Seer;
import dev.seer.api.SeerEntrypoint;
import dev.seer.api.SeerPlugin;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

public final class SeerPluginLoader {
    private static final Type ANNOTATION = Type.getType(SeerEntrypoint.class);

    private SeerPluginLoader() {}

    /**
     * Сканирует данные всех модов и создаёт плагины.
     * Scans all mods' scan data and instantiates the plugins.
     */
    public static List<SeerPlugin> discover() {
        List<SeerPlugin> plugins = new ArrayList<>();
        for (ModFileScanData data : ModList.get().getAllScanData()) {
            for (ModFileScanData.AnnotationData annotation : data.getAnnotations()) {
                if (!ANNOTATION.equals(annotation.annotationType())) continue;
                String className = annotation.clazz().getClassName();
                try {
                    Class<?> type = Class.forName(className);
                    if (SeerPlugin.class.isAssignableFrom(type)) {
                        plugins.add((SeerPlugin) type.getDeclaredConstructor().newInstance());
                    } else {
                        Seer.LOGGER.warn("SEER: {} has @SeerEntrypoint but is not a SeerPlugin", className);
                    }
                } catch (Throwable t) {
                    Seer.LOGGER.error("SEER: failed to load plugin {}", className, t);
                }
            }
        }
        return plugins;
    }
}
