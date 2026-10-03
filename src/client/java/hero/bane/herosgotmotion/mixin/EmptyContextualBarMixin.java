package hero.bane.herosgotmotion.mixin;

import hero.bane.herosgotmotion.render.SprintBarRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.contextualbar.ContextualBar$1")
public class EmptyContextualBarMixin {
    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void hero$drawVelocityBarInEmptySlot(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        SprintBarRenderer.render(graphics, deltaTracker);
    }
}
