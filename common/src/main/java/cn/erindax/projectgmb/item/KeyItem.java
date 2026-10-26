package cn.erindax.projectgmb.item;

import cn.erindax.projectgmb.lock.LockHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class KeyItem extends Item {

	public KeyItem(Properties properties) {
		super(properties);
	}

	public static String skinOf(ItemStack stack) {
		return stack.getOrDefault(ModComponents.KEY_SKIN, "");
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		if (!player.hasPermissions(2)) {
			return InteractionResultHolder.pass(player.getItemInHand(hand));
		}
		if (player instanceof ServerPlayer serverPlayer) {
			LockHandler.openKeySkinPicker(serverPlayer, hand);
		}
		return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
	}
}
