package cn.erindax.projectgmb.item;

import cn.erindax.projectgmb.seal.Seals;
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

public class SealerItem extends Item {

	public SealerItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player instanceof ServerPlayer serverPlayer) {
			Seals.cancelSelection(serverPlayer);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.projectgm_b.sealer.tooltip_select").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.projectgm_b.sealer.tooltip_toggle").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.projectgm_b.sealer.tooltip_cancel").withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.translatable("item.projectgm_b.sealer.tooltip_view").withStyle(ChatFormatting.DARK_GRAY));
	}
}
