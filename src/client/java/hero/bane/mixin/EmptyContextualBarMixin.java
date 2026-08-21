package hero.bane.mixin;

import hero.bane.render.SprintBarRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.contextualbar.ContextualBarRenderer$1")
public class EmptyContextualBarMixin {
    @Inject(method = "renderBackground", at = @At("HEAD"))
    private void hero$drawVelocityBarInEmptySlot(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SprintBarRenderer.render(graphics, deltaTracker);
    }
}
