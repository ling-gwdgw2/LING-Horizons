package me.ling.horizons.client.mixin.minecraft;

import me.ling.horizons.client.LingClientInstance;
import me.ling.horizons.client.config.LingConfig;
import me.ling.horizons.commonImpl.LingCommon;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {
    @Inject(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;commonPlayerSpawnInfo()Lnet/minecraft/network/protocol/game/CommonPlayerSpawnInfo;"))
    private void voxy$init(ClientboundLoginPacket packet, CallbackInfo ci) {
        if (LingCommon.isAvailable() && !LingClientInstance.isInGame) {
            LingClientInstance.isInGame = true;
            if (LingConfig.CONFIG.enabled) {
                if (LingCommon.getInstance() != null) {
                    LingCommon.shutdownInstance();
                }
                LingCommon.createInstance();
            }
            me.ling.horizons.client.pregen.WorldPregenerator.triggerAutoPregenIfEnabled();
        }
    }
}
