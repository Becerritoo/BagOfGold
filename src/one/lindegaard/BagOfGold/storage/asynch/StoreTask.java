package one.lindegaard.BagOfGold.storage.asynch;

import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.PlayerBalance;
import one.lindegaard.CustomItemsLib.storage.DataStoreException;
import one.lindegaard.BagOfGold.storage.IDataStore;

public class StoreTask implements IDataStoreTask<Void> {
	private LinkedHashSet<PlayerBalance> mWaitingPlayerBalances = new LinkedHashSet<PlayerBalance>();
	private final boolean mCleanCache;
	private final boolean mSaveBank;

	public StoreTask(Set<Object> waiting) {
		this(waiting, null, true, true);
	}

	/**
	 * Drain only the pending balance snapshots for one player. This is used as a
	 * write barrier before that player's balance is read again.
	 */
	public StoreTask(Set<Object> waiting, UUID playerUuid) {
		this(waiting, playerUuid, false, false);
	}

	private StoreTask(Set<Object> waiting, UUID playerUuid, boolean cleanCache, boolean saveBank) {
		mCleanCache = cleanCache;
		mSaveBank = saveBank;
		synchronized (waiting) {
			Iterator<Object> iterator = waiting.iterator();
			while (iterator.hasNext()) {
				Object obj = iterator.next();
				if (obj instanceof PlayerBalance) {
					PlayerBalance balance = (PlayerBalance) obj;
					if (playerUuid == null || playerUuid.equals(balance.getPlayer().getUniqueId())) {
						mWaitingPlayerBalances.add(balance);
						iterator.remove();
					}
				} else if (playerUuid == null) {
					iterator.remove();
				}
			}
		}
	}

	@Override
	public Void run(IDataStore store) throws DataStoreException {
		if (!mWaitingPlayerBalances.isEmpty())
			store.savePlayerBalances(mWaitingPlayerBalances, mCleanCache);
		
		if (mSaveBank) {
			BagOfGold.getInstance().getMessages().debug("Saving BagOfGold data");
			BagOfGold.getInstance().getBankManager().save();
		}
		
		return null;
	}

	@Override
	public boolean readOnly() {
		return false;
	}
}
