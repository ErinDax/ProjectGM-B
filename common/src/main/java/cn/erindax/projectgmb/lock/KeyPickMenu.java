package cn.erindax.projectgmb.lock;

import cn.erindax.projectgmb.item.LockItem;
import cn.erindax.projectgmb.manage.LockedSlot;
import cn.erindax.projectgmb.manage.ManagerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class KeyPickMenu extends AbstractContainerMenu {

	private static final int TOP = 9;

	private final BlockPos pos;

	public static void open(ServerPlayer player, BlockPos pos) {
		player.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> new KeyPickMenu(id, inventory, pos),
			Component.translatable("screen.projectgm_b.lock.pick_key")));
	}

	private KeyPickMenu(int containerId, Inventory inventory, BlockPos pos) {
		super(MenuType.GENERIC_9x1, containerId);
		this.pos = pos.immutable();

		SimpleContainer top = new SimpleContainer(TOP);
		for (int i = 0; i < TOP; i++) {
			top.setItem(i, ManagerItems.filler());
		}
		top.setItem(4, ManagerItems.button(Items.TRIPWIRE_HOOK,
			"screen.projectgm_b.lock.pick_key", "screen.projectgm_b.lock.pick_key_lore"));
		for (int i = 0; i < TOP; i++) {
			addSlot(new LockedSlot(top, i));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new LockedSlot(inventory, col + row * 9 + 9));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new LockedSlot(inventory, col));
		}
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (!(player instanceof ServerPlayer viewer) || clickType != ClickType.PICKUP || slotId < TOP
				|| slotId >= slots.size()) {
			return;
		}
		ItemStack chosen = slots.get(slotId).getItem();
		if (chosen.isEmpty()) {
			return;
		}
		if (chosen.getItem() instanceof LockItem) {
			viewer.displayClientMessage(Component.translatable("screen.projectgm_b.lock.key_cannot_be_lock"), true);
			return;
		}
		viewer.closeContainer();
		LockHandler.finishKeySetup(viewer, pos, chosen);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.blockPosition().distSqr(pos) <= 64.0;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return false;
	}
}
