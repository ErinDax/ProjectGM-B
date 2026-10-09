package cn.erindax.projectgmb.client.render;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.skin.TextureStore;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

public final class RemoteTextures {

	private record Entry(ResourceLocation id, byte[] png) {
	}

	private static final Map<String, Entry> TEXTURES = new HashMap<>();

	private RemoteTextures() {
	}

	@Nullable
	public static ResourceLocation get(String kind, String name) {
		Entry entry = TEXTURES.get(key(kind, name));
		if (entry == null) {
			return null;
		}
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		AbstractTexture texture = textures.getTexture(entry.id(), null);
		if (!(texture instanceof DynamicTexture dynamic) || dynamic.getPixels() == null) {
			if (!upload(textures, entry)) {
				return null;
			}
		}
		return entry.id();
	}

	public static void put(String kind, String name, byte[] png) {
		if (!TextureStore.isValidName(name)) {
			return;
		}
		Entry entry = new Entry(ProjectGmB.id("remote/" + kind + "/" + HexFormat.of().formatHex(
			name.getBytes(StandardCharsets.UTF_8))), png);
		if (upload(Minecraft.getInstance().getTextureManager(), entry)) {
			TEXTURES.put(key(kind, name), entry);
		}
	}

	public static void clear() {
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		TEXTURES.values().forEach(entry -> textures.release(entry.id()));
		TEXTURES.clear();
	}

	private static String key(String kind, String name) {
		return kind + "/" + name;
	}

	private static boolean upload(TextureManager textures, Entry entry) {
		NativeImage image = decode(entry);
		if (image == null) {
			return false;
		}
		textures.register(entry.id(), new DynamicTexture(image));
		return true;
	}

	@Nullable
	private static NativeImage decode(Entry entry) {
		NativeImage image;
		try {
			image = NativeImage.read(entry.png());
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Received invalid texture {}", entry.id(), e);
			return null;
		}
		if (!TextureStore.KEYS.validSize(image.getWidth(), image.getHeight())) {
			ProjectGmB.LOGGER.warn("Texture {} has unsupported size {}x{}", entry.id(), image.getWidth(), image.getHeight());
			image.close();
			return null;
		}
		return image;
	}
}
