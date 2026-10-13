package one.lindegaard.BagOfGold;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Identifies asynchronous balance loads that belong to the current player
 * session. Results from an older connection must not replace current data.
 */
final class BalanceSessionTracker {

	private final AtomicLong sequence = new AtomicLong();
	private final ConcurrentHashMap<UUID, Long> activeSessions = new ConcurrentHashMap<UUID, Long>();

	long begin(UUID uuid) {
		long generation = sequence.incrementAndGet();
		activeSessions.put(uuid, generation);
		return generation;
	}

	boolean isCurrent(UUID uuid, long generation) {
		Long current = activeSessions.get(uuid);
		return current != null && current.longValue() == generation;
	}

	void invalidate(UUID uuid) {
		activeSessions.remove(uuid);
	}
}
