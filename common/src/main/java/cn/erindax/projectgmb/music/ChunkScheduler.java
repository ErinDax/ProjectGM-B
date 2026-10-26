package cn.erindax.projectgmb.music;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

final class ChunkScheduler<K> {

	interface Sink<K> {
		boolean send(K key, String name, int version, int index, int total, byte[] chunk);
	}

	private static final class Transfer {
		private final String name;
		private final int version;
		private final byte[] data;
		private final int total;
		private int sent;
		private int acked;
		private long waitingSince;

		private Transfer(String name, int version, byte[] data, int chunkBytes) {
			this.name = name;
			this.version = version;
			this.data = data;
			this.total = Math.max(1, (data.length + chunkBytes - 1) / chunkBytes);
		}

		private int pending() {
			return sent - acked;
		}
	}

	private final int chunkBytes;
	private final int window;
	private final int maxInFlight;
	private final int maxQueued;
	private final long timeoutNanos;
	private final Map<K, Deque<Transfer>> transfers = new LinkedHashMap<>();
	private int inFlight;

	ChunkScheduler(int chunkBytes, int window, int maxInFlight, int maxQueued, long timeoutNanos) {
		this.chunkBytes = chunkBytes;
		this.window = window;
		this.maxInFlight = maxInFlight;
		this.maxQueued = maxQueued;
		this.timeoutNanos = timeoutNanos;
	}

	boolean isEmpty() {
		return transfers.isEmpty();
	}

	int inFlight() {
		return inFlight;
	}

	boolean offer(K key, String name, int version, byte[] data) {
		Deque<Transfer> queue = transfers.get(key);
		if (queue == null) {
			queue = new ArrayDeque<>();
			transfers.put(key, queue);
		} else {
			for (Transfer transfer : queue) {
				if (transfer.name.equals(name) && transfer.version == version) {
					return false;
				}
			}
			if (queue.size() >= maxQueued) {
				return false;
			}
		}
		queue.add(new Transfer(name, version, data, chunkBytes));
		return true;
	}

	void ack(K key, String name, int version, int index, long now, Sink<K> sink) {
		Deque<Transfer> queue = transfers.get(key);
		Transfer head = queue == null ? null : queue.peek();
		if (head == null || head.pending() == 0 || head.acked != index || head.version != version
				|| !head.name.equals(name)) {
			return;
		}
		head.acked++;
		head.waitingSince = now;
		inFlight--;
		if (head.acked >= head.total) {
			queue.poll();
			transfers.remove(key);
			if (!queue.isEmpty()) {
				transfers.put(key, queue);
			}
		}
		pump(now, sink);
	}

	void drop(K key) {
		Deque<Transfer> queue = transfers.remove(key);
		Transfer head = queue == null ? null : queue.peek();
		if (head != null) {
			inFlight -= head.pending();
		}
	}

	void tick(long now, Predicate<K> online, Sink<K> sink) {
		int pending = 0;
		Iterator<Map.Entry<K, Deque<Transfer>>> it = transfers.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<K, Deque<Transfer>> entry = it.next();
			if (!online.test(entry.getKey())) {
				it.remove();
				continue;
			}
			Deque<Transfer> queue = entry.getValue();
			Transfer head = queue.peek();
			if (head == null || head.pending() > 0 && now - head.waitingSince > timeoutNanos) {
				it.remove();
			} else {
				pending += head.pending();
			}
		}
		inFlight = pending;
		pump(now, sink);
	}

	void pump(long now, Sink<K> sink) {
		if (inFlight >= maxInFlight || transfers.isEmpty()) {
			return;
		}
		for (K key : new ArrayList<>(transfers.keySet())) {
			Deque<Transfer> queue = transfers.get(key);
			Transfer head = queue == null ? null : queue.peek();
			while (head != null && inFlight < maxInFlight && head.sent < head.total
					&& head.pending() < (head.acked > 0 ? window : 1)) {
				int from = head.sent * chunkBytes;
				int to = Math.min(head.data.length, from + chunkBytes);
				if (!sink.send(key, head.name, head.version, head.sent, head.total,
						Arrays.copyOfRange(head.data, from, to))) {
					break;
				}
				if (head.pending() == 0) {
					head.waitingSince = now;
				}
				head.sent++;
				inFlight++;
			}
			if (inFlight >= maxInFlight) {
				return;
			}
		}
	}
}
