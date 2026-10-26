package cn.erindax.projectgmb.client.mixin;

import cn.erindax.projectgmb.item.ModItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMarkerParticleMixin {

	@Shadow
	@Final
	@Mutable
	private static Set<Item> MARKER_PARTICLE_ITEMS;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void projectgmb$addBarrierPanelMarker(CallbackInfo ci) {
		if (MARKER_PARTICLE_ITEMS.contains(ModItems.BARRIER_PANEL)) {
			return;
		}
		Set<Item> items = new HashSet<>(MARKER_PARTICLE_ITEMS);
		items.add(ModItems.BARRIER_PANEL);
		MARKER_PARTICLE_ITEMS = Set.copyOf(items);
	}
}
