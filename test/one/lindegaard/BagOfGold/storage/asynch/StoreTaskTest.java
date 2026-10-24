package one.lindegaard.BagOfGold.storage.asynch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.junit.Test;

import one.lindegaard.BagOfGold.PlayerBalance;
import one.lindegaard.BagOfGold.storage.IDataStore;
import one.lindegaard.BagOfGold.storage.PendingBalanceSnapshots;

public class StoreTaskTest {

	@Test
	public void targetedTaskDrainsAndStoresOnlyRequestedPlayer() throws Exception {
		UUID firstUuid = UUID.randomUUID();
		UUID secondUuid = UUID.randomUUID();
		PlayerBalance first = balance(firstUuid);
		PlayerBalance second = balance(secondUuid);
		PendingBalanceSnapshots waiting = new PendingBalanceSnapshots();
		waiting.put(first);
		waiting.put(second);

		StoreTask task = new StoreTask(waiting, firstUuid);
		AtomicBoolean cleanCache = new AtomicBoolean(true);
		final int[] saved = new int[1];
		IDataStore store = (IDataStore) Proxy.newProxyInstance(IDataStore.class.getClassLoader(),
				new Class<?>[] { IDataStore.class }, (proxy, method, args) -> {
					if (method.getName().equals("savePlayerBalances")) {
						saved[0] = ((Set<?>) args[0]).size();
						cleanCache.set((Boolean) args[1]);
					}
					return null;
				});

		task.run(store);

		assertEquals(1, saved[0]);
		assertFalse(cleanCache.get());
		assertEquals(1, waiting.size());
		assertEquals(secondUuid, waiting.snapshot().get(0).getPlayer().getUniqueId());
	}

	private PlayerBalance balance(UUID uuid) {
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
		return new PlayerBalance(player, "world", GameMode.SURVIVAL, 10, 0, 0, 0, 0);
	}
}
