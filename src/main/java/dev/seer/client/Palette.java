package dev.seer.client;

/**
 * Общая палитра интерфейса SEER. Фоны зависят от прозрачности и пересчитываются в setOpacity.
 * Shared SEER UI palette. Backgrounds depend on the opacity and are recomputed in setOpacity.
 */
final class Palette {
    // Базовые непрозрачные цвета. / Base opaque colors.
    private static final int BASE_CARD = 0xFF1A1B21;
    private static final int BASE_CARD_TOP = 0xFF24252D;
    private static final int BASE_CARD_BOTTOM = 0xFF15161B;
    private static final int BASE_CARD_SHADOW = 0xFF0E0E12;
    private static final int BASE_SLOT = 0xFF121317;
    private static final int BASE_BUTTON_FACE = 0xFF353849;
    private static final int BASE_BUTTON_HOVER = 0xFF444A6B;
    // рядом с остальными BASE_*: / next to the other BASE_* constants:
    private static final int BASE_FRAME = 0xFF2A2B33;

    // Итоговые цвета: читаются во время отрисовки, поэтому не final. / Resulting colors: read while drawing, hence not final.
    static int CARD;
    static int CARD_TOP;
    static int CARD_BOTTOM;
    static int CARD_SHADOW;
    static int SLOT;
    static int BUTTON_FACE;
    static int BUTTON_HOVER;
    // рядом с остальными изменяемыми цветами: / next to the other mutable colors:
    static int FRAME;

    // Не зависят от прозрачности. / Independent of the opacity.
    static final int CARD_EDGE = 0xFF34353F;
    static final int CARD_LIGHT = 0xFF3C3E4B;
    static final int CARD_DARK_EDGE = 0xFF0B0B0F;
    static final int SLOT_EDGE = 0xFF454759;
    static final int SLOT_SHADOW = 0xFF0A0B0E;
    static final int SLOT_LIGHT = 0xFF3F4152;
    static final int BUTTON_OUTLINE = 0xFF0C0D12;
    static final int BUTTON_LIGHT = 0xFF5D6388;
    static final int BUTTON_DARK = 0xFF1C1E2C;
    static final int ACCENT = 0xFF6B5BD6;
    static final int TEXT = 0xFFFFFFFF;
    static final int MUTED = 0xFFA0A3B5;
    // рядом с остальными final-цветами (край рамки почти непрозрачный): / next to the other final colors (the frame edge is almost opaque):
    static final int FRAME_EDGE = 0xCC4A4D5E;

    private static float opacity = 0.85f;

    static {
        setOpacity(opacity);
    }

    private Palette() {}

    static float opacity() { return opacity; }

    /** Прозрачность от 0.3 до 1.0. / Opacity from 0.3 to 1.0. */
    static void setOpacity(float value) {
        opacity = Math.max(0.3f, Math.min(1f, value));
        CARD = fade(BASE_CARD, opacity);
        CARD_TOP = fade(BASE_CARD_TOP, opacity);
        CARD_BOTTOM = fade(BASE_CARD_BOTTOM, opacity);
        CARD_SHADOW = fade(BASE_CARD_SHADOW, opacity);
        SLOT = fade(BASE_SLOT, opacity);
        BUTTON_FACE = fade(BASE_BUTTON_FACE, opacity);
        BUTTON_HOVER = fade(BASE_BUTTON_HOVER, opacity);
        FRAME = fade(BASE_FRAME, opacity);
    }

    static int fade(int argb, float factor) {
        int alpha = Math.round(((argb >>> 24) & 255) * factor);
        return (alpha << 24) | (argb & 0xFFFFFF);
    }
}