package cn.erindax.projectgmb.entity;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.compat.bcya.BcyaMaskCompat;
import cn.erindax.projectgmb.entity.ai.PatrolRouteGoal;
import cn.erindax.projectgmb.entity.ai.StareGoal;
import cn.erindax.projectgmb.skin.TextureStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PatrollerEntity extends PathfinderMob {

	public enum State {
		PATROL, STARE, ATTACK
	}

	public static final int DEFAULT_STARE_TICKS = 6 * 20;
	public static final int DEFAULT_PAUSE_TICKS = 5 * 20;
	public static final int DEFAULT_DETECT_DIAMETER = 40;
	public static final int MIN_DETECT_DIAMETER = 2;
	public static final int MAX_DETECT_DIAMETER = 256;
	private static final double MIN_FOLLOW_RANGE = 48.0;
	private static final double VIEW_CONE_COS = Math.cos(Math.toRadians(60.0));
	private static final double PLAYER_KNOCKBACK = 0.4;
	private static final float EYE_HEIGHT = 1.62F;
	private static final ResourceKey<DamageType> ATTACK_DAMAGE_TYPE =
		ResourceKey.create(Registries.DAMAGE_TYPE, ProjectGmB.id("patroller_attack"));

	private static final EntityDataAccessor<String> SKIN =
		SynchedEntityData.defineId(PatrollerEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> SLIM =
		SynchedEntityData.defineId(PatrollerEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> PATROL_NAME =
		SynchedEntityData.defineId(PatrollerEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> NAME_VISIBLE =
		SynchedEntityData.defineId(PatrollerEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> SHOW_HELD_ITEMS =
		SynchedEntityData.defineId(PatrollerEntity.class, EntityDataSerializers.BOOLEAN);

	private static final String TAG_WAYPOINTS = "Waypoints";
	private static final String TAG_PAUSE = "PauseTicks";
	private static final String TAG_SKIN = "Skin";
	private static final String TAG_SLIM = "Slim";
	private static final String TAG_INDEX = "RouteIndex";
	private static final String TAG_FORWARD = "RouteForward";
	private static final String TAG_NAME = "PatrolName";
	private static final String TAG_NAME_VISIBLE = "NameVisible";
	private static final String TAG_DETECT = "DetectDiameter";
	private static final String TAG_STARE = "StareTicks";
	private static final String TAG_SHOW_HELD = "ShowHeldItems";
	private static final String TAG_PATROLLING = "Patrolling";
	private static final String TAG_HOSTILE = "Aggressive";

	private final List<BlockPos> waypoints = new ArrayList<>();
	private int pauseTicks = DEFAULT_PAUSE_TICKS;
	private int detectDiameter = DEFAULT_DETECT_DIAMETER;
	private int stareDuration = DEFAULT_STARE_TICKS;
	private boolean patrolling = true;
	private boolean hostile = true;
	private int routeIndex;
	private boolean routeForward = true;

	private State state = State.PATROL;
	private int stareTicks;
	@Nullable
	private UUID watchedPlayer;

	public PatrollerEntity(EntityType<? extends PatrollerEntity> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return createMobAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.ATTACK_DAMAGE, 1.0)
			.add(Attributes.FOLLOW_RANGE, MIN_FOLLOW_RANGE);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(SKIN, "");
		entityData.define(SLIM, false);
		entityData.define(PATROL_NAME, "");
		entityData.define(NAME_VISIBLE, false);
		entityData.define(SHOW_HELD_ITEMS, true);
	}

	@Override
	protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
		return EYE_HEIGHT;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new StareGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
		goalSelector.addGoal(3, new PatrolRouteGoal(this, 1.0));
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		if (!hostile) {
			if (state != State.PATROL) {
				returnToPatrol();
			}
			return;
		}
		switch (state) {
			case PATROL -> {
				Player player = findUnmaskedPlayerInView();
				if (player != null) {
					watchedPlayer = player.getUUID();
					stareTicks = 0;
					state = State.STARE;
				}
			}
			case STARE -> {
				Player player = getWatchedPlayer();
				if (player == null) {
					returnToPatrol();
				} else if (++stareTicks >= stareDuration) {
					setTarget(player);
					state = State.ATTACK;
				}
			}
			case ATTACK -> {
				Player player = getWatchedPlayer();
				if (player == null) {
					returnToPatrol();
				} else if (getTarget() != player) {
					setTarget(player);
				}
			}
		}
	}

	private void returnToPatrol() {
		watchedPlayer = null;
		stareTicks = 0;
		setTarget(null);
		state = State.PATROL;
	}

	@Nullable
	private Player findUnmaskedPlayerInView() {
		Player best = null;
		double bestDist = Double.MAX_VALUE;
		Vec3 eye = getEyePosition();
		Vec3 look = getViewVector(1.0F);
		double radius = detectDiameter / 2.0;
		for (Player player : level().players()) {
			if (!isValidVictim(player, radius)) {
				continue;
			}
			Vec3 toPlayer = player.getEyePosition().subtract(eye);
			double dist = toPlayer.length();
			if (dist > 1.0E-4 && toPlayer.scale(1.0 / dist).dot(look) < VIEW_CONE_COS) {
				continue;
			}
			if (dist < bestDist && hasLineOfSight(player)) {
				best = player;
				bestDist = dist;
			}
		}
		return best;
	}

	@Nullable
	private Player getWatchedPlayer() {
		if (watchedPlayer == null || !(level() instanceof ServerLevel serverLevel)) {
			return null;
		}
		if (!(serverLevel.getEntity(watchedPlayer) instanceof Player player)) {
			return null;
		}
		return isValidVictim(player, detectDiameter) ? player : null;
	}

	private boolean isValidVictim(Player player, double range) {
		return player.isAlive()
			&& !player.isSpectator()
			&& !player.isCreative()
			&& player.level() == level()
			&& distanceTo(player) <= range
			&& !BcyaMaskCompat.isMasked(player);
	}

	public State getState() {
		return state;
	}

	@Nullable
	public LivingEntity getStareTarget() {
		return state == State.STARE ? getWatchedPlayer() : null;
	}

	public List<BlockPos> getWaypoints() {
		return waypoints;
	}

	public void setWaypoints(List<BlockPos> points) {
		waypoints.clear();
		waypoints.addAll(points);
		routeIndex = 0;
		routeForward = true;
	}

	public int getPauseTicks() {
		return pauseTicks;
	}

	public void setPauseTicks(int ticks) {
		pauseTicks = Math.max(0, ticks);
	}

	public boolean isPatrolling() {
		return patrolling;
	}

	public void setPatrolling(boolean value) {
		patrolling = value;
	}

	public boolean isHostile() {
		return hostile;
	}

	public void setHostile(boolean value) {
		hostile = value;
	}

	public boolean showsHeldItems() {
		return entityData.get(SHOW_HELD_ITEMS);
	}

	public void setShowHeldItems(boolean value) {
		entityData.set(SHOW_HELD_ITEMS, value);
	}

	public int getStareTicks() {
		return stareDuration;
	}

	public void setStareTicks(int ticks) {
		stareDuration = Math.max(0, ticks);
	}

	public int getDetectDiameter() {
		return detectDiameter;
	}

	public void setDetectDiameter(int diameter) {
		detectDiameter = Math.max(MIN_DETECT_DIAMETER, Math.min(MAX_DETECT_DIAMETER, diameter));
		AttributeInstance followRange = getAttribute(Attributes.FOLLOW_RANGE);
		if (followRange != null) {
			followRange.setBaseValue(Math.max(MIN_FOLLOW_RANGE, detectDiameter + 8.0));
		}
	}

	public int getRouteIndex() {
		return routeIndex;
	}

	public void setRouteIndex(int index) {
		routeIndex = index;
	}

	public boolean isRouteForward() {
		return routeForward;
	}

	public void setRouteForward(boolean forward) {
		routeForward = forward;
	}

	public String getSkinName() {
		return entityData.get(SKIN);
	}

	public boolean isSlim() {
		return entityData.get(SLIM);
	}

	public void setSkin(String name, boolean slim) {
		entityData.set(SKIN, name);
		entityData.set(SLIM, slim);
	}

	public String getPatrolName() {
		return entityData.get(PATROL_NAME);
	}

	public void setPatrolName(String name) {
		entityData.set(PATROL_NAME, name);
	}

	public boolean isNameVisible() {
		return entityData.get(NAME_VISIBLE);
	}

	public void setNameVisible(boolean visible) {
		entityData.set(NAME_VISIBLE, visible);
	}

	@Override
	public Component getName() {
		String name = getPatrolName();
		return name.isEmpty() ? super.getName() : Component.literal(name);
	}

	@Override
	public boolean shouldShowName() {
		return isNameVisible() && !getPatrolName().isEmpty();
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		String skin = getSkinName();
		if (!skin.isEmpty()) {
			TextureStore.SKINS.sendTo(player, skin);
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!player.hasPermissions(2) || !player.isCreative()) {
			return InteractionResult.PASS;
		}
		ItemStack held = player.getItemInHand(hand);
		EquipmentSlot slot = held.isEmpty() ? EquipmentSlot.MAINHAND : getEquipmentSlotForItem(held);
		if (slot == EquipmentSlot.OFFHAND) {
			slot = EquipmentSlot.MAINHAND;
		}
		if (!level().isClientSide) {
			ItemStack previous = getItemBySlot(slot);
			setItemSlot(slot, held.copy());
			player.setItemInHand(hand, previous);
		}
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		DamageSource source = new DamageSource(level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
			.getHolderOrThrow(ATTACK_DAMAGE_TYPE), this);
		float damage = (float) getAttributeValue(Attributes.ATTACK_DAMAGE);
		if (target instanceof LivingEntity living) {
			damage += EnchantmentHelper.getDamageBonus(getMainHandItem(), living.getMobType());
		}
		if (!target.hurt(source, damage)) {
			return false;
		}
		if (target instanceof LivingEntity living) {
			living.knockback(PLAYER_KNOCKBACK,
				Mth.sin(getYRot() * Mth.DEG_TO_RAD), -Mth.cos(getYRot() * Mth.DEG_TO_RAD));
		}
		doEnchantDamageEffects(this, target);
		setLastHurtMob(target);
		return true;
	}

	@Override
	public boolean canHoldItem(ItemStack stack) {
		return false;
	}

	@Override
	public boolean wantsToPickUp(ItemStack stack) {
		return false;
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putLongArray(TAG_WAYPOINTS, waypoints.stream().mapToLong(BlockPos::asLong).toArray());
		tag.putInt(TAG_PAUSE, pauseTicks);
		tag.putString(TAG_SKIN, getSkinName());
		tag.putBoolean(TAG_SLIM, isSlim());
		tag.putInt(TAG_INDEX, routeIndex);
		tag.putBoolean(TAG_FORWARD, routeForward);
		tag.putString(TAG_NAME, getPatrolName());
		tag.putBoolean(TAG_NAME_VISIBLE, isNameVisible());
		tag.putInt(TAG_DETECT, detectDiameter);
		tag.putInt(TAG_STARE, stareDuration);
		tag.putBoolean(TAG_SHOW_HELD, showsHeldItems());
		tag.putBoolean(TAG_PATROLLING, patrolling);
		tag.putBoolean(TAG_HOSTILE, hostile);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		waypoints.clear();
		for (long packed : tag.getLongArray(TAG_WAYPOINTS)) {
			waypoints.add(BlockPos.of(packed));
		}
		pauseTicks = tag.contains(TAG_PAUSE) ? tag.getInt(TAG_PAUSE) : DEFAULT_PAUSE_TICKS;
		setSkin(tag.getString(TAG_SKIN), tag.getBoolean(TAG_SLIM));
		routeIndex = Math.min(tag.getInt(TAG_INDEX), Math.max(0, waypoints.size() - 1));
		routeForward = !tag.contains(TAG_FORWARD) || tag.getBoolean(TAG_FORWARD);
		setPatrolName(tag.getString(TAG_NAME));
		setNameVisible(tag.getBoolean(TAG_NAME_VISIBLE));
		setDetectDiameter(tag.contains(TAG_DETECT) ? tag.getInt(TAG_DETECT) : DEFAULT_DETECT_DIAMETER);
		setStareTicks(tag.contains(TAG_STARE) ? tag.getInt(TAG_STARE) : DEFAULT_STARE_TICKS);
		setShowHeldItems(!tag.contains(TAG_SHOW_HELD) || tag.getBoolean(TAG_SHOW_HELD));
		patrolling = !tag.contains(TAG_PATROLLING) || tag.getBoolean(TAG_PATROLLING);
		hostile = !tag.contains(TAG_HOSTILE) || tag.getBoolean(TAG_HOSTILE);
	}
}
