package cn.erindax.projectgmb.client.render;

import cn.erindax.projectgmb.ProjectGmB;
import cn.erindax.projectgmb.item.KeyItem;
import cn.erindax.projectgmb.skin.TextureStore;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

public final class KeyItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {

	private record Vertex(float x, float y, float z, float u, float v) {
	}

	private record Quad(Vertex a, Vertex b, Vertex c, Vertex d, float nx, float ny, float nz) {
	}

	private static final ResourceLocation DEFAULT = ProjectGmB.id("textures/item/key.png");
	private static final float FRONT = 8.5F / 16.0F;
	private static final float BACK = 7.5F / 16.0F;

	private final Map<AbstractTexture, List<Quad>> geometry = new WeakHashMap<>();

	@Override
	public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers,
			int light, int overlay) {
		ResourceLocation texture = resolveTexture(stack);
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		AbstractTexture handle = textures.getTexture(texture);
		List<Quad> quads = geometry.computeIfAbsent(handle, h -> build(h, texture));

		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
		PoseStack.Pose pose = poseStack.last();
		for (Quad quad : quads) {
			vertex(consumer, pose, quad.a(), quad, light, overlay);
			vertex(consumer, pose, quad.b(), quad, light, overlay);
			vertex(consumer, pose, quad.c(), quad, light, overlay);
			vertex(consumer, pose, quad.d(), quad, light, overlay);
		}
	}

	private static ResourceLocation resolveTexture(ItemStack stack) {
		String skin = KeyItem.skinOf(stack);
		if (skin.isEmpty()) {
			return DEFAULT;
		}
		ResourceLocation remote = RemoteTextures.get(TextureStore.KEYS.kind(), skin);
		return remote != null ? remote : DEFAULT;
	}

	private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vertex v, Quad quad, int light, int overlay) {
		consumer.addVertex(pose, v.x(), v.y(), v.z())
			.setColor(255, 255, 255, 255)
			.setUv(v.u(), v.v())
			.setOverlay(overlay)
			.setLight(light)
			.setNormal(pose, quad.nx(), quad.ny(), quad.nz());
	}

	private static List<Quad> build(AbstractTexture handle, ResourceLocation texture) {
		List<Quad> quads = new ArrayList<>();
		quads.add(new Quad(
			new Vertex(1, 1, BACK, 1, 0), new Vertex(1, 0, BACK, 1, 1),
			new Vertex(0, 0, BACK, 0, 1), new Vertex(0, 1, BACK, 0, 0), 0, 0, -1));

		NativeImage image = handle instanceof DynamicTexture dynamic ? dynamic.getPixels() : null;
		boolean owned = false;
		if (image == null) {
			image = loadResource(texture);
			owned = true;
		}
		if (image != null) {
			try {
				addEdges(quads, image);
			} finally {
				if (owned) {
					image.close();
				}
			}
		}
		quads.add(new Quad(
			new Vertex(0, 1, FRONT, 0, 0), new Vertex(0, 0, FRONT, 0, 1),
			new Vertex(1, 0, FRONT, 1, 1), new Vertex(1, 1, FRONT, 1, 0), 0, 0, 1));
		return quads;
	}

	@Nullable
	private static NativeImage loadResource(ResourceLocation texture) {
		Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
		if (resource.isEmpty()) {
			return null;
		}
		try (InputStream in = resource.get().open()) {
			return NativeImage.read(in);
		} catch (IOException e) {
			ProjectGmB.LOGGER.warn("Failed to read key texture {}", texture, e);
			return null;
		}
	}

	private static void addEdges(List<Quad> quads, NativeImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		float du = 1.0F / w;
		float dv = 1.0F / h;
		for (int py = 0; py < h; py++) {
			for (int px = 0; px < w; px++) {
				if (transparent(image, px, py)) {
					continue;
				}
				float x0 = px * du;
				float x1 = x0 + du;
				float y1 = 1.0F - py * dv;
				float y0 = y1 - dv;
				float u0 = px * du;
				float u1 = u0 + du;
				float v0 = py * dv;
				float v1 = v0 + dv;
				if (transparent(image, px, py - 1)) {
					quads.add(new Quad(
						new Vertex(x0, y1, BACK, u0, v0), new Vertex(x0, y1, FRONT, u0, v1),
						new Vertex(x1, y1, FRONT, u1, v1), new Vertex(x1, y1, BACK, u1, v0), 0, 1, 0));
				}
				if (transparent(image, px, py + 1)) {
					quads.add(new Quad(
						new Vertex(x0, y0, FRONT, u0, v0), new Vertex(x0, y0, BACK, u0, v1),
						new Vertex(x1, y0, BACK, u1, v1), new Vertex(x1, y0, FRONT, u1, v0), 0, -1, 0));
				}
				if (transparent(image, px - 1, py)) {
					quads.add(new Quad(
						new Vertex(x0, y1, BACK, u0, v0), new Vertex(x0, y0, BACK, u0, v1),
						new Vertex(x0, y0, FRONT, u1, v1), new Vertex(x0, y1, FRONT, u1, v0), -1, 0, 0));
				}
				if (transparent(image, px + 1, py)) {
					quads.add(new Quad(
						new Vertex(x1, y1, FRONT, u0, v0), new Vertex(x1, y0, FRONT, u0, v1),
						new Vertex(x1, y0, BACK, u1, v1), new Vertex(x1, y1, BACK, u1, v0), 1, 0, 0));
				}
			}
		}
	}

	private static boolean transparent(NativeImage image, int x, int y) {
		if (x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
			return true;
		}
		return FastColor.ABGR32.alpha(image.getPixelRGBA(x, y)) == 0;
	}
}
