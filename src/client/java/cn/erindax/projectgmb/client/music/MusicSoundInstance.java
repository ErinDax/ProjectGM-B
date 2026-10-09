package cn.erindax.projectgmb.client.music;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.music.net.MusicControlPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class MusicSoundInstance extends AbstractTickableSoundInstance {

	public static final String PATH_PREFIX = "music/";

	private final String track;
	private final int range;
	private final int entityId;
	private final long offsetMillis;
	private final long offsetAt;

	public MusicSoundInstance(UUID session, String track, int range, int entityId, BlockPos pos, long offsetMillis,
			long offsetAt) {
		super(SoundEvent.createVariableRangeEvent(ProjectGmB.id(PATH_PREFIX + session)), SoundSource.RECORDS,
			SoundInstance.createUnseededRandom());
		this.track = track;
		this.range = range;
		this.entityId = entityId;
		this.offsetMillis = offsetMillis;
		this.offsetAt = offsetAt;
		this.volume = 1.0F;
		this.pitch = 1.0F;
		this.looping = false;
		this.relative = false;
		this.attenuation = SoundInstance.Attenuation.LINEAR;
		this.x = pos.getX() + 0.5;
		this.y = pos.getY() + 0.5;
		this.z = pos.getZ() + 0.5;
		follow();
	}

	public String track() {
		return track;
	}

	public long offsetMillis() {
		return offsetMillis;
	}

	public long offsetAt() {
		return offsetAt;
	}

	@Override
	public WeighedSoundEvents resolve(SoundManager manager) {
		this.sound = new Sound(this.location, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F), 1, Sound.Type.FILE,
			true, false, range);
		return new WeighedSoundEvents(this.location, null);
	}

	@Override
	public void tick() {
		follow();
	}

	private void follow() {
		if (entityId == MusicControlPayload.NO_ENTITY) {
			return;
		}
		ClientLevel level = Minecraft.getInstance().level;
		Entity entity = level == null ? null : level.getEntity(entityId);
		if (entity != null && !entity.isRemoved()) {
			this.x = entity.getX();
			this.y = entity.getEyeY();
			this.z = entity.getZ();
		}
	}
}
