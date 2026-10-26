package cn.erindax.projectgmb.item;

import cn.erindax.projectgmb.manage.PlayerPickerMenu;
import cn.erindax.projectgmb.manage.WandWhitelist;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class MagicWandItem extends Item {

	public MagicWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!WandWhitelist.isAllowed(player)) {
			if (!level.isClientSide) {
				player.displayClientMessage(
					Component.translatable("item.projectgm_b.magic_wand.no_permission").withStyle(ChatFormatting.RED), true);
			}
			return InteractionResultHolder.fail(stack);
		}
		if (player instanceof ServerPlayer serverPlayer) {
			PlayerPickerMenu.open(serverPlayer);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.projectgm_b.magic_wand.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
