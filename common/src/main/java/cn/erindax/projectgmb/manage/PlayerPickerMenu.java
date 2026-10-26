package cn.erindax.projectgmb.manage;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

public class PlayerPickerMenu extends AbstractContainerMenu {

	private static final int ROWS = 6;
	private static final int SIZE = ROWS * 9;
	private static final int PAGE_SIZE = SIZE - 9;
	private static final int GROUP_SLOT = SIZE - 9;
	private static final int PREV_SLOT = SIZE - 8;
	private static final int NEXT_SLOT = SIZE - 7;
	private static final String UNGROUPED = "";
	private static final Map<UUID, View> VIEWS = new HashMap<>();

	private record View(@Nullable String group, int page) {
	}

	private final MinecraftServer server;
	private final UUID viewerId;
	private final SimpleContainer container = new SimpleContainer(SIZE);
	private final List<UUID> players = new ArrayList<>();
	@Nullable
	private String group;
	private int page;

	public static void open(ServerPlayer viewer) {
		viewer.openMenu(new SimpleMenuProvider(
			(id, inventory, p) -> new PlayerPickerMenu(id, inventory, viewer),
			Component.translatable("screen.projectgm_b.players.title")));
	}

	private PlayerPickerMenu(int containerId, Inventory viewerInventory, ServerPlayer viewer) {
		super(MenuType.GENERIC_9x6, containerId);
		this.server = viewer.server;
		this.viewerId = viewer.getUUID();
		View view = VIEWS.get(viewerId);
		if (view != null) {
			group = view.group();
			page = view.page();
		}

		for (int i = 0; i < SIZE; i++) {
			addSlot(new LockedSlot(container, i));
		}
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				addSlot(new LockedSlot(viewerInventory, col + row * 9 + 9));
			}
		}
		for (int col = 0; col < 9; col++) {
			addSlot(new LockedSlot(viewerInventory, col));
		}
		refresh();
	}

	private void refresh() {
		WandRosterState roster = WandRosterState.get(server);
		List<ServerPlayer> visible = visible(roster);
		List<String> groups = groups(roster, visible);
		if (!groups.contains(group)) {
			group = null;
			page = 0;
		}
		List<ServerPlayer> shown = new ArrayList<>();
		for (ServerPlayer target : visible) {
			if (group == null || roster.groupOf(target.getUUID()).equals(group)) {
				shown.add(target);
			}
		}
		int pages = Math.max(1, (shown.size() + PAGE_SIZE - 1) / PAGE_SIZE);
		page = Mth.clamp(page, 0, pages - 1);
		VIEWS.put(viewerId, new View(group, page));

		players.clear();
		for (int i = 0; i < PAGE_SIZE; i++) {
			int index = page * PAGE_SIZE + i;
			if (index < shown.size()) {
				ServerPlayer target = shown.get(index);
				players.add(target.getUUID());
				container.setItem(i, ManagerItems.head(target, roster.groupOf(target.getUUID())));
			} else {
				container.setItem(i, ItemStack.EMPTY);
			}
		}
		for (int i = PAGE_SIZE; i < SIZE; i++) {
			container.setItem(i, ManagerItems.filler());
		}
		container.setItem(GROUP_SLOT, ManagerItems.button(Items.NAME_TAG,
			Component.translatable("screen.projectgm_b.players.group", groupName(group)),
			Component.translatable("screen.projectgm_b.players.group_lore")));
		Component pageLabel = Component.translatable("screen.projectgm_b.players.page", page + 1, pages);
		if (page > 0) {
			container.setItem(PREV_SLOT, ManagerItems.button(Items.ARROW,
				Component.translatable("screen.projectgm_b.players.prev"), pageLabel));
		}
		if (page < pages - 1) {
			container.setItem(NEXT_SLOT, ManagerItems.button(Items.SPECTRAL_ARROW,
				Component.translatable("screen.projectgm_b.players.next"), pageLabel));
		}
	}

	private List<ServerPlayer> visible(WandRosterState roster) {
		List<ServerPlayer> visible = new ArrayList<>();
		for (ServerPlayer target : server.getPlayerList().getPlayers()) {
			if (!roster.isHidden(target.getUUID())) {
				visible.add(target);
			}
		}
		visible.sort((a, b) -> a.getGameProfile().getName().compareToIgnoreCase(b.getGameProfile().getName()));
		return visible;
	}

	private static List<String> groups(WandRosterState roster, List<ServerPlayer> visible) {
		TreeSet<String> named = new TreeSet<>();
		boolean ungrouped = false;
		for (ServerPlayer target : visible) {
			String name = roster.groupOf(target.getUUID());
			if (name.isEmpty()) {
				ungrouped = true;
			} else {
				named.add(name);
			}
		}
		List<String> groups = new ArrayList<>();
		groups.add(null);
		groups.addAll(named);
		if (ungrouped && !named.isEmpty()) {
			groups.add(UNGROUPED);
		}
		return groups;
	}

	private static Component groupName(@Nullable String group) {
		if (group == null) {
			return Component.translatable("screen.projectgm_b.players.group_all");
		}
		return group.isEmpty() ? Component.translatable("screen.projectgm_b.players.group_none") : Component.literal(group);
	}

	private void cycle(int step) {
		WandRosterState roster = WandRosterState.get(server);
		List<String> groups = groups(roster, visible(roster));
		int index = Math.max(0, groups.indexOf(group));
		group = groups.get(Math.floorMod(index + step, groups.size()));
		page = 0;
		refresh();
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player player) {
		if (!(player instanceof ServerPlayer viewer) || clickType != ClickType.PICKUP || slotId < 0) {
			return;
		}
		if (slotId == GROUP_SLOT) {
			cycle(button == 1 ? -1 : 1);
			return;
		}
		if (slotId == PREV_SLOT || slotId == NEXT_SLOT) {
			page += slotId == NEXT_SLOT ? 1 : -1;
			refresh();
			return;
		}
		if (slotId < players.size()) {
			ServerPlayer target = server.getPlayerList().getPlayer(players.get(slotId));
			if (target != null && !WandRosterState.get(server).isHidden(target.getUUID())) {
				PlayerInventoryMenu.open(viewer, target);
			} else {
				refresh();
			}
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return WandWhitelist.isAllowed(player);
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return false;
	}
}
