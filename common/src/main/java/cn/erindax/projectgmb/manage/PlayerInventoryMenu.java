package cn.erindax.projectgmb.manage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public class PlayerInventoryMenu extends AbstractContainerMenu {

	private static final int SIZE = 54;
	private static final int TARGET_SLOTS = 41;
	private static final int BACK_SLOT = SIZE - 1;
	private static final int[] ARMOR_ORDER = {39, 38, 37, 36};
	private static final EquipmentSlot[] ARMOR_TYPES = {
		EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private final MinecraftServer server;
	private final UUID targetId;

	public static void open(ServerPlayer viewer, ServerPlayer target) {
		viewer.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> new PlayerInventoryMenu(id, inventory, target),
			Component.translatable("screen.projectgm_b.players.inventory", target.getGameProfile().getName())));
	}

	private PlayerInventoryMenu(int containerId, Inventory viewerInventory, ServerPlayer target) {
		super(MenuType.GENERIC_9x6, containerId);
		this.server = target.server;
		this.targetId = target.getUUID();
		Inventory inv = target.getInventory();

		for (int i = 9; i < 36; i++) {
			addSlot(new Slot(inv, i, 0, 0));
		}
		for (int i = 0; i < 9; i++) {
			addSlot(new Slot(inv, i, 0, 0));
		}
		for (int i = 0; i < ARMOR_ORDER.length; i++) {
			EquipmentSlot type = ARMOR_TYPES[i];
			addSlot(new Slot(inv, ARMOR_ORDER[i], 0, 0) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return target.getEquipmentSlotForItem(stack) == type;
				}
			});
		}
		addSlot(new Slot(inv, 40, 0, 0));

		SimpleContainer extras = new SimpleContainer(SIZE - TARGET_SLOTS);
		for (int i = 0; i < extras.getContainerSize(); i++) {
			extras.setItem(i, ManagerItems.filler());
		}
		extras.setItem(extras.getContainerSize() - 1, ManagerItems.button(Items.ARROW,
			"screen.projectgm_b.players.back", "screen.projectgm_b.players.back_lore"));
		for (int i = 0; i < extras.getContainerSize(); i++) {
			addSlot(new LockedSlot(extras, i));
		}

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new Slot(viewerInventory, col + row * 9 + 9, 0, 0));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new Slot(viewerInventory, col, 0, 0));
		}
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (slotId == BACK_SLOT) {
			if (player instanceof ServerPlayer viewer && clickType == ClickType.PICKUP) {
				PlayerPickerMenu.open(viewer);
			}
			return;
		}
		if (slotId >= TARGET_SLOTS && slotId < SIZE) {
			return;
		}
		super.clicked(slotId, button, clickType, player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		boolean moved = index < TARGET_SLOTS
			? moveItemStackTo(stack, SIZE, slots.size(), true)
			: moveItemStackTo(stack, 0, TARGET_SLOTS, false);
		if (!moved) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) {
			slot.setByPlayer(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return original;
	}

	@Override
	public boolean stillValid(Player player) {
		if (!WandWhitelist.isAllowed(player) || WandRosterState.get(server).isHidden(targetId)) {
			return false;
		}
		ServerPlayer target = server.getPlayerList().getPlayer(targetId);
		return target != null && target.isAlive();
	}
}
