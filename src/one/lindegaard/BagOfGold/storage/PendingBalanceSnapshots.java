package one.lindegaard.BagOfGold.storage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;

import one.lindegaard.BagOfGold.PlayerBalance;

/**
 * Coalesces queued balance writes so only the newest snapshot for each player,
 * world group, and game mode reaches the storage thread.
 */
public final class PendingBalanceSnapshots {

	private final Map<Key, PlayerBalance> snapshots = new LinkedHashMap<>();

	public synchronized void put(PlayerBalance balance) {
		snapshots.put(Key.of(balance), balance);
	}

	public synchronized Set<PlayerBalance> drain(UUID playerUuid) {
		Set<PlayerBalance> drained = new LinkedHashSet<>();
		Iterator<Map.Entry<Key, PlayerBalance>> iterator = snapshots.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Key, PlayerBalance> entry = iterator.next();
			if (playerUuid == null || playerUuid.equals(entry.getKey().playerUuid)) {
				drained.add(entry.getValue());
				iterator.remove();
			}
		}
		return drained;
	}

	public synchronized List<PlayerBalance> snapshot() {
		return new ArrayList<>(snapshots.values());
	}

	public synchronized boolean isEmpty() {
		return snapshots.isEmpty();
	}

	public synchronized int size() {
		return snapshots.size();
	}

	private static final class Key {
		private final UUID playerUuid;
		private final String worldGroup;
		private final GameMode gameMode;

		private Key(UUID playerUuid, String worldGroup, GameMode gameMode) {
			this.playerUuid = playerUuid;
			this.worldGroup = worldGroup;
			this.gameMode = gameMode;
		}

		private static Key of(PlayerBalance balance) {
			return new Key(balance.getPlayer().getUniqueId(), balance.getWorldGroup(), balance.getGamemode());
		}

		@Override
		public boolean equals(Object other) {
			if (this == other)
				return true;
			if (!(other instanceof Key key))
				return false;
			return playerUuid.equals(key.playerUuid) && Objects.equals(worldGroup, key.worldGroup)
					&& gameMode == key.gameMode;
		}

		@Override
		public int hashCode() {
			return Objects.hash(playerUuid, worldGroup, gameMode);
		}
	}
}
