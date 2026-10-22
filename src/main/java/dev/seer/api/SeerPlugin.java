package dev.seer.api;

/**
 * Точка входа плагина. Реализацию нужно пометить @SeerEntrypoint.
 * Plugin entry point. Implementations must be annotated with @SeerEntrypoint.
 */
public interface SeerPlugin {
    void register(SeerRegistry registry);
}
