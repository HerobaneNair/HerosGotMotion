package hero.bane.herosgotmotion.mixin;

import hero.bane.herosgotmotion.render.SprintBarRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBarRenderer.class)
public class LocatorBarRendererMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void hero$replaceLocatorBackground(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (SprintBarRenderer.render(graphics, deltaTracker)) {
            ci.cancel();
        }
    }
}
