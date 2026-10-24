package one.lindegaard.BagOfGold.storage;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.junit.Test;

import one.lindegaard.BagOfGold.PlayerBalance;

public class PendingBalanceSnapshotsTest {

	@Test
	public void keepsOnlyNewestSnapshotForEachAccount() {
		UUID uuid = UUID.randomUUID();
		PendingBalanceSnapshots pending = new PendingBalanceSnapshots();
		pending.put(balance(uuid, "world", 10));
		pending.put(balance(uuid, "world", 25));

		assertEquals(1, pending.size());
		Set<PlayerBalance> drained = pending.drain(uuid);
		assertEquals(1, drained.size());
		assertEquals(25, drained.iterator().next().getBalance(), 0.000001);
		assertEquals(0, pending.size());
	}

	@Test
	public void preservesSeparateWorldGroupSnapshots() {
		UUID uuid = UUID.randomUUID();
		PendingBalanceSnapshots pending = new PendingBalanceSnapshots();
		pending.put(balance(uuid, "world", 10));
		pending.put(balance(uuid, "nether", 20));

		assertEquals(2, pending.drain(uuid).size());
	}

	private PlayerBalance balance(UUID uuid, String worldGroup, double amount) {
		OfflinePlayer player = (OfflinePlayer) Proxy.newProxyInstance(OfflinePlayer.class.getClassLoader(),
				new Class<?>[] { OfflinePlayer.class }, (proxy, method, args) -> {
					if (method.getName().equals("getUniqueId"))
						return uuid;
					if (method.getName().equals("getName"))
						return uuid.toString();
					if (method.getReturnType().equals(boolean.class))
						return false;
					return null;
				});
		return new PlayerBalance(player, worldGroup, GameMode.SURVIVAL, amount, 0, 0, 0, 0);
	}
}
