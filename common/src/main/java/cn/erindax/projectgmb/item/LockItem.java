package cn.erindax.projectgmb.item;

import cn.erindax.projectgmb.lock.LockType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LockItem extends Item {

	private final LockType type;

	public LockItem(LockType type, Properties properties) {
		super(properties);
		this.type = type;
	}

	public LockType getType() {
		return type;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.projectgm_b.lock.tooltip").withStyle(ChatFormatting.GRAY));
	}
}
