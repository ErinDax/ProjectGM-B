package cn.erindax.projectgmb.network.codec;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class ByteBufCodecs {
	public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
	public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
	public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
	public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
		(buffer, value) -> wrap(buffer).writeVarInt(value), buffer -> wrap(buffer).readVarInt());
	public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(
		(buffer, value) -> wrap(buffer).writeUtf(value), buffer -> wrap(buffer).readUtf());
	public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = StreamCodec.of(
		(buffer, value) -> wrap(buffer).writeByteArray(value), buffer -> wrap(buffer).readByteArray());
	public static final StreamCodec<ByteBuf, java.util.UUID> UUID = StreamCodec.of(
		(buffer, value) -> wrap(buffer).writeUUID(value), buffer -> wrap(buffer).readUUID());
	public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of(
		(buffer, value) -> wrap(buffer).writeBlockPos(value), buffer -> wrap(buffer).readBlockPos());
	public static final StreamCodec<ByteBuf, Component> COMPONENT = StreamCodec.of(
		(buffer, value) -> wrap(buffer).writeComponent(value), buffer -> wrap(buffer).readComponent());

	private ByteBufCodecs() {
	}

	public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
		return list(Integer.MAX_VALUE);
	}

	public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
		return codec -> StreamCodec.of((buffer, values) -> {
			if (values.size() > maxSize) {
				throw new EncoderException("List size " + values.size() + " is larger than allowed " + maxSize);
			}
			wrap(buffer).writeVarInt(values.size());
			for (V value : values) {
				codec.encode(buffer, value);
			}
		}, buffer -> {
			int size = wrap(buffer).readVarInt();
			if (size > maxSize) {
				throw new DecoderException(size + " elements exceeded max size of: " + maxSize);
			}
			List<V> values = new ArrayList<>(Math.min(size, 65536));
			for (int i = 0; i < size; i++) {
				values.add(codec.decode(buffer));
			}
			return values;
		});
	}

	private static FriendlyByteBuf wrap(ByteBuf buffer) {
		return buffer instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buffer);
	}
}
