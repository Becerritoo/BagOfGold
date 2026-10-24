package one.lindegaard.BagOfGold.storage.asynch;

import java.util.LinkedHashSet;
import java.util.UUID;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.PlayerBalance;
import one.lindegaard.BagOfGold.storage.PendingBalanceSnapshots;
import one.lindegaard.CustomItemsLib.storage.DataStoreException;
import one.lindegaard.BagOfGold.storage.IDataStore;

public class StoreTask implements IDataStoreTask<Void> {
	private LinkedHashSet<PlayerBalance> mWaitingPlayerBalances = new LinkedHashSet<PlayerBalance>();
	private final boolean mCleanCache;
	private final boolean mSaveBank;

	public StoreTask(PendingBalanceSnapshots waiting) {
		this(waiting, null, true, true);
	}

	/**
	 * Drain only the pending balance snapshots for one player. This is used as a
	 * write barrier before that player's balance is read again.
	 */
	public StoreTask(PendingBalanceSnapshots waiting, UUID playerUuid) {
		this(waiting, playerUuid, false, false);
	}

	private StoreTask(PendingBalanceSnapshots waiting, UUID playerUuid, boolean cleanCache, boolean saveBank) {
		mCleanCache = cleanCache;
		mSaveBank = saveBank;
		mWaitingPlayerBalances.addAll(waiting.drain(playerUuid));
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
