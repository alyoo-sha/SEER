package dev.seer.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает класс SeerPlugin для автоматического обнаружения.
 * Marks a SeerPlugin class for automatic discovery.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SeerEntrypoint {}
