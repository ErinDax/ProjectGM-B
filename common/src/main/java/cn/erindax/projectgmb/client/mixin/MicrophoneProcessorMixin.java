package cn.erindax.projectgmb.client.mixin;

import cn.erindax.projectgmb.client.HostBroadcastClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "de.maxhenkel.voicechat.voice.client.MicrophoneProcessor", remap = false)
public class MicrophoneProcessorMixin {
	@Inject(method = "isPttButtonDown", at = @At("RETURN"), cancellable = true, remap = false)
	private void projectgmBForcePtt(CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && HostBroadcastClient.shouldForceMic()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isMuted", at = @At("RETURN"), cancellable = true, remap = false)
	private void projectgmBUnmute(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && HostBroadcastClient.shouldForceMic()) {
			cir.setReturnValue(false);
		}
	}
}
