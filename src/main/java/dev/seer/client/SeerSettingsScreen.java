package dev.seer.client;

import com.nstut.openui.api.UIComponent;
import com.nstut.openui.api.Ui;
import com.nstut.openui.minecraft.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран настроек SEER. Сюда добавляем остальные опции по мере появления функционала.
 * SEER settings screen. More options are added here as features appear.
 */
public final class SeerSettingsScreen extends UiScreen {
    private static final int W = 220;
    private static final int H = 150;

    private final Screen parent;

    public SeerSettingsScreen(Screen parent) {
        super(Component.literal("SEER settings"));
        this.parent = parent;
    }

    @Override protected int uiLeft() { return (width - W) / 2; }
    @Override protected int uiTop() { return (height - H) / 2; }
    @Override protected int uiWidth() { return W; }
    @Override protected int uiHeight() { return H; }

    @Override
    protected UIComponent buildUI() {
        return new SeerFrame(Ui.padding(8, Ui.column(
                Ui.heading("SEER"),
                Ui.checkbox("Show item panel", SeerState.VISIBLE),
                Ui.text("Panel opacity"),
                Ui.slider(SeerState.OPACITY, 30, 100),
                Ui.spacer().flex(),
                // Закрываем отложенно: нельзя закрывать экран внутри обработчика его рантайма.
                // Deferred close: a screen must not be closed inside its own runtime's handler.
                BevelButton.text("Done", 60, 20, () -> Minecraft.getInstance().tell(this::onClose))
        ).gap(8)));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
    }

    @Override
    public void removed() {
        super.removed();
        SeerState.flush();
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}