package cn.erindax.projectgmb.mixin;

import cn.erindax.projectgmb.reinforce.Reinforcements;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockAttachedEntity.class, ItemFrame.class, ArmorStand.class})
public abstract class EntityReinforceMixin {
	@Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
	private void projectgmb$keepReinforced(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (Reinforcements.protectsEntity((Entity) (Object) this, source)) {
			cir.setReturnValue(false);
		}
	}
}
