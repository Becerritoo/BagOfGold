package one.lindegaard.BagOfGold.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType.SlotType;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.BagOfGold.PlayerBalance;
import one.lindegaard.BagOfGold.compatibility.ShopkeepersCompat;
import one.lindegaard.CustomItemsLib.Tools;
import one.lindegaard.CustomItemsLib.api.PhysicalCurrencyService;

/**
 * Supplies BagOfGold balance policy to CustomItemsLib without requiring the
 * library to import BagOfGold classes.
 */
public class BagOfGoldPhysicalCurrencyService implements PhysicalCurrencyService {

	private final BagOfGold plugin;

	public BagOfGoldPhysicalCurrencyService(BagOfGold plugin) {
		this.plugin = plugin;
	}

	@Override
	public void addToBalance(OfflinePlayer player, double amount) {
		plugin.getRewardManager().addMoneyToPlayerBalance(player, amount);
	}

	@Override
	public void removeFromBalance(OfflinePlayer player, double amount) {
		plugin.getRewardManager().removeMoneyFromPlayerBalance(player, amount);
	}

	@Override
	public void recordInventoryCredit(Player player, double amount) {
		PlayerBalance balance = plugin.getPlayerBalanceManager().getPlayerBalance(player);
		balance.setBalance(Tools.round(balance.getBalance() + amount));
		plugin.getPlayerBalanceManager().setPlayerBalance(player, balance);
	}

	@Override
	public boolean supportsInventoryReconciliation() {
		return true;
	}

	@Override
	public void reconcileInventory(Player player, String sourceHint) {
		plugin.getRewardManager().adjustPlayerBalanceToAmounOfMoneyInInventory(player, sourceHint);
	}

	@Override
	public boolean isAdditionalRewardSlotAllowed(SlotType slotType) {
		return ShopkeepersCompat.isSupported()
				&& (slotType == SlotType.CRAFTING || slotType == SlotType.RESULT);
	}
}
