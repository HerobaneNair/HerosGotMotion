package hero.bane.mixin;

import hero.bane.HerosGotMotion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "handleSetExperience", at = @At("HEAD"))
    private void hero$onXpUpdate(ClientboundSetExperiencePacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int exp = packet.getTotalExperience();
        int prev = HerosGotMotion.prevTotalExp;
        HerosGotMotion.prevTotalExp = exp;

        if (prev < 0) return;
        if (exp > prev && HerosGotMotion.hideTicksAfterXp > 0) {
            HerosGotMotion.hideBarUntilTick = mc.player.tickCount + HerosGotMotion.hideTicksAfterXp;
        }
    }
}
