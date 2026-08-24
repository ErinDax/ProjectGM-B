package cn.erindax.projectgmb.voice;

import cn.erindax.projectgmb.ProjectGmB;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.audiochannel.StaticAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HostVoicePlugin implements VoicechatPlugin {
	private static final String CATEGORY_ID = "host";
	private static final Map<UUID, StaticAudioChannel> CHANNELS = new ConcurrentHashMap<>();
	private VoicechatServerApi api;

	@Override
	public String getPluginId() {
		return ProjectGmB.MOD_ID;
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
		registration.registerEvent(VoicechatServerStoppedEvent.class, this::onServerStopped);
		registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
		registration.registerEvent(PlayerDisconnectedEvent.class, event -> HostBroadcast.remove(event.getPlayerUuid()));
	}

	private void onServerStarted(VoicechatServerStartedEvent event) {
		api = event.getVoicechat();
		VolumeCategory category = api.volumeCategoryBuilder()
			.setId(CATEGORY_ID)
			.setName("主持人")
			.setNameTranslationKey("voicechat.category.projectgm_b.host")
			.setDescription("主持人全服广播")
			.setDescriptionTranslationKey("voicechat.category.projectgm_b.host.tooltip")
			.build();
		api.registerVolumeCategory(category);
	}

	private void onServerStopped(VoicechatServerStoppedEvent event) {
		api = null;
		HostBroadcast.clear();
	}

	private void onMicrophone(MicrophonePacketEvent event) {
		VoicechatConnection sender = event.getSenderConnection();
		if (sender == null || api == null) {
			return;
		}
		UUID uuid = sender.getPlayer().getUuid();
		if (!HostBroadcast.isActive(uuid)) {
			return;
		}
		Object raw = sender.getPlayer().getPlayer();
		if (!(raw instanceof ServerPlayer player) || !player.hasPermissions(2)) {
			HostBroadcast.remove(uuid);
			return;
		}
		event.cancel();
		StaticAudioChannel channel = CHANNELS.get(uuid);
		if (channel == null) {
			channel = api.createStaticAudioChannel(channelId(uuid));
			if (channel == null) {
				return;
			}
			channel.setBypassGroupIsolation(true);
			channel.setCategory(CATEGORY_ID);
			CHANNELS.put(uuid, channel);
		}
		channel.clearTargets();
		MinecraftServer server = player.getServer();
		if (server == null) {
			return;
		}
		for (ServerPlayer other : server.getPlayerList().getPlayers()) {
			if (other.getUUID().equals(uuid)) {
				continue;
			}
			VoicechatConnection connection = api.getConnectionOf(other.getUUID());
			if (connection != null) {
				channel.addTarget(connection);
			}
		}
		channel.send(event.getPacket());
	}

	static void closeChannel(UUID playerId) {
		StaticAudioChannel channel = CHANNELS.remove(playerId);
		if (channel != null) {
			channel.flush();
		}
	}

	static void closeAllChannels() {
		for (StaticAudioChannel channel : CHANNELS.values()) {
			channel.flush();
		}
		CHANNELS.clear();
	}

	private static UUID channelId(UUID player) {
		return UUID.nameUUIDFromBytes(("projectgm_b:host:" + player).getBytes(StandardCharsets.UTF_8));
	}
}
