package cn.erindax.projectgmb.lock;

import cn.erindax.projectgmb.ProjectGmB;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LockNames {
	private static final String LOCK_ITEM_NAME_KEY = "projectgm_b.lock_item_name";
	private static final String KEY_ITEM_NAME_KEY = "projectgm_b.key_item_name";

	private static String lockItemName = "上锁器";
	private static String keyItemName = "钥匙";

	private LockNames() {
	}

	public static String lockItemName() {
		return lockItemName;
	}

	public static String keyItemName() {
		return keyItemName;
	}

	public static void reload() {
		Path path = configLangPath();
		if (!Files.exists(path)) {
			copyDefault(path);
		}
		if (!Files.exists(path)) {
			return;
		}
		try {
			JsonObject json = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			if (json.has(LOCK_ITEM_NAME_KEY)) {
				lockItemName = json.get(LOCK_ITEM_NAME_KEY).getAsString();
			}
			if (json.has(KEY_ITEM_NAME_KEY)) {
				keyItemName = json.get(KEY_ITEM_NAME_KEY).getAsString();
			}
		} catch (IOException e) {
			ProjectGmB.LOGGER.error("Failed to reload lock names", e);
		}
	}

	private static Path configLangPath() {
		return FabricLoader.getInstance().getConfigDir().resolve("projectgm_b").resolve("zh_cn.json");
	}

	private static void copyDefault(Path path) {
		try (InputStream in = ProjectGmB.class.getResourceAsStream("/assets/projectgm_b/lang/zh_cn.json")) {
			if (in == null) {
				return;
			}
			Files.createDirectories(path.getParent());
			Files.copy(in, path);
		} catch (IOException e) {
			ProjectGmB.LOGGER.error("Failed to write default lock names", e);
		}
	}
}
