package cn.erindax.projectgmb.item;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.block.ModBlocks;
import cn.erindax.projectgmb.lock.LockType;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class ModItems {

	public static final Item MAGIC_WAND = register("magic_wand",
		new MagicWandItem(new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.EPIC)));

	public static final Item BARRIER_PANEL = register("barrier_panel",
		new BlockItem(ModBlocks.BARRIER_PANEL, new Item.Properties()
			.rarity(Rarity.EPIC)));

	public static final Item KEY = register("key",
		new KeyItem(new Item.Properties()
			.stacksTo(16)));

	public static final Item PASSWORD_LOCK = register("password_lock",
		new LockItem(LockType.PASSWORD, new Item.Properties()
			.stacksTo(16)));

	public static final Item KEY_LOCK = register("key_lock",
		new LockItem(LockType.KEY, new Item.Properties()
			.stacksTo(16)));

	public static final Item MUSIC_NOTE = register("music_note",
		new MusicNoteItem(new Item.Properties()
			.stacksTo(1)
			.rarity(Rarity.RARE)));

	public static final Item DANCE_BLOCK = register("dance_block",
		new BlockItem(ModBlocks.DANCE_BLOCK, new Item.Properties()
			.rarity(Rarity.RARE)));

	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
		ProjectGmB.id(ProjectGmB.MOD_ID),
		FabricItemGroup.builder()
			.title(Component.translatable("itemGroup." + ProjectGmB.MOD_ID + "." + ProjectGmB.MOD_ID))
			.icon(() -> new ItemStack(MAGIC_WAND))
			.displayItems((parameters, output) -> {
				output.accept(MAGIC_WAND);
				output.accept(KEY);
				output.accept(PASSWORD_LOCK);
				output.accept(KEY_LOCK);
				output.accept(MUSIC_NOTE);
				output.accept(BARRIER_PANEL);
				output.accept(DANCE_BLOCK);
			})
			.build());

	private ModItems() {
	}

	private static <T extends Item> T register(String name, T item) {
		return Registry.register(BuiltInRegistries.ITEM, ProjectGmB.id(name), item);
	}

	public static void init() {
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.OP_BLOCKS).register(entries -> entries.accept(BARRIER_PANEL));
	}
}
