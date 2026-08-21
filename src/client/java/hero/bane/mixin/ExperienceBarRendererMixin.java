package hero.bane.mixin;

import hero.bane.render.SprintBarRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.contextualbar.ExperienceBarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceBarRenderer.class)
public class ExperienceBarRendererMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void hero$replaceXpBar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (SprintBarRenderer.render(graphics, deltaTracker)) {
            ci.cancel();
        }
    }
}
