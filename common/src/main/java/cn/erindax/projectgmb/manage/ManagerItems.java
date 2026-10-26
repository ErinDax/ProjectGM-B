package cn.erindax.projectgmb.manage;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

public final class ManagerItems {

	private ManagerItems() {
	}

	public static ItemStack filler() {
		ItemStack stack = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		stack.setHoverName(Component.empty());
		return stack;
	}

	public static ItemStack button(ItemLike icon, String nameKey, String loreKey) {
		return button(icon, Component.translatable(nameKey), Component.translatable(loreKey));
	}

	public static ItemStack button(ItemLike icon, Component name, Component lore) {
		ItemStack stack = new ItemStack(icon);
		stack.setHoverName(plain(name.copy().withStyle(ChatFormatting.YELLOW)));
		setLore(stack, List.of(plain(lore.copy().withStyle(ChatFormatting.GRAY))));
		return stack;
	}

	public static ItemStack head(ServerPlayer player, String group) {
		ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
		stack.getOrCreateTag().put(PlayerHeadItem.TAG_SKULL_OWNER,
			NbtUtils.writeGameProfile(new CompoundTag(), player.getGameProfile()));
		stack.setHoverName(plain(Component.literal(player.getGameProfile().getName())
			.withStyle(ChatFormatting.AQUA)));
		List<Component> lore = new ArrayList<>();
		if (!group.isEmpty()) {
			lore.add(plain(Component.translatable("screen.projectgm_b.players.group", group).withStyle(ChatFormatting.GRAY)));
		}
		lore.add(plain(Component.translatable("screen.projectgm_b.players.open_inventory").withStyle(ChatFormatting.GRAY)));
		setLore(stack, lore);
		return stack;
	}

	private static void setLore(ItemStack stack, List<Component> lore) {
		ListTag lines = new ListTag();
		for (Component line : lore) {
			lines.add(StringTag.valueOf(Component.Serializer.toJson(line)));
		}
		stack.getOrCreateTagElement(ItemStack.TAG_DISPLAY).put(ItemStack.TAG_LORE, lines);
	}

	private static Component plain(Component component) {
		return component.copy().withStyle(style -> style.withItalic(false));
	}
}
