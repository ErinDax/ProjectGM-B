package cn.erindax.projectgmb.item;

import cn.erindax.projectgmb.ProjectGmB;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModComponents {

	public static final DataComponentType<String> KEY_SKIN = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		ProjectGmB.id("key_skin"),
		DataComponentType.<String>builder()
			.persistent(Codec.STRING)
			.networkSynchronized(ByteBufCodecs.STRING_UTF8)
			.build());

	public static final DataComponentType<String> MUSIC_TRACK = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		ProjectGmB.id("music_track"),
		DataComponentType.<String>builder()
			.persistent(Codec.STRING)
			.networkSynchronized(ByteBufCodecs.STRING_UTF8)
			.build());

	public static final DataComponentType<Integer> MUSIC_RANGE = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
		ProjectGmB.id("music_range"),
		DataComponentType.<Integer>builder()
			.persistent(Codec.INT)
			.networkSynchronized(ByteBufCodecs.VAR_INT)
			.build());

	private ModComponents() {
	}

	public static void init() {
	}
}
