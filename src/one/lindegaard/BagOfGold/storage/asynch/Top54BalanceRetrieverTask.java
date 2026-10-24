package one.lindegaard.BagOfGold.storage.asynch;

import java.util.Iterator;
import java.util.List;

import one.lindegaard.BagOfGold.PlayerBalance;
import one.lindegaard.BagOfGold.storage.PendingBalanceSnapshots;
import one.lindegaard.CustomItemsLib.storage.DataStoreException;
import one.lindegaard.BagOfGold.storage.IDataStore;

public class Top54BalanceRetrieverTask implements IDataStoreTask<List<PlayerBalance>> {
	private int mCount;
	private String mWorldGroup;
	private int mGamemode;
	private PendingBalanceSnapshots mWaiting;

	public Top54BalanceRetrieverTask(int count, String worldGroup, int gamemode, PendingBalanceSnapshots waiting) {
		mCount = count;
		mWorldGroup = worldGroup;
		mGamemode = gamemode;
		mWaiting = waiting;
	}

	private void updateUsingCache(List<PlayerBalance> stats) {
		for (PlayerBalance cached : mWaiting.snapshot()) {

				Iterator<PlayerBalance> it = stats.iterator();
				boolean found = false;

				while (it.hasNext()) {
					PlayerBalance stat = it.next();
					if (cached.getPlayer().getUniqueId().equals(stat.getPlayer().getUniqueId())
							&& cached.getWorldGroup().equals(stat.getWorldGroup())
							&& cached.getGamemode() == stat.getGamemode()) {
						stat.setBalance(cached.getBalance());
						stat.setBalanceChanges(cached.getBalanceChanges());
						stat.setDebt(cached.getDebt());
						stat.setBankBalance(cached.getBankBalance());
						stat.setBankBalanceChanges(cached.getBankBalanceChanges());
						found = true;
					}
				}

				if (!found && cached.getWorldGroup().equals(mWorldGroup)
						&& cached.getGamemode().getValue() == mGamemode)
					stats.add(cached);
		}
	}

	@Override
	public List<PlayerBalance> run(IDataStore store) throws DataStoreException {
		List<PlayerBalance> stats = store.loadTop54(mCount, mWorldGroup, mGamemode);
		updateUsingCache(stats);
		return stats;
	}

	@Override
	public boolean readOnly() {
		return true;
	}

}
