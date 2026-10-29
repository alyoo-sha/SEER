package dev.seer.mixin;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public class RecipeBookComponentMixin {
    // Данные книги (в том числе присланные сервером) больше не открывают её ни при init, ни в tick.
    // The book data (including what the server sends) no longer opens it, neither in init nor in tick.
    @Inject(method = "isVisibleAccordingToBookData", at = @At("HEAD"), cancellable = true)
    private void seer$alwaysClosed(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }

    // Страховка: открыть книгу вручную тоже нельзя, закрыть можно. / A safeguard: the book cannot be opened by hand either, but it can be closed.
    @Inject(method = "setVisible", at = @At("HEAD"), cancellable = true)
    private void seer$neverOpen(boolean visible, CallbackInfo ci) {
        if (visible) ci.cancel();
    }
}
