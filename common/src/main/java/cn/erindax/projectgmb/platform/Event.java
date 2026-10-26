package cn.erindax.projectgmb.platform;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class Event<T> {
	private final List<T> listeners = new CopyOnWriteArrayList<>();

	public void register(T listener) {
		listeners.add(listener);
	}

	public List<T> listeners() {
		return listeners;
	}

	public static <V> void post(Event<Consumer<V>> event, V value) {
		for (Consumer<V> listener : event.listeners) {
			listener.accept(value);
		}
	}
}
