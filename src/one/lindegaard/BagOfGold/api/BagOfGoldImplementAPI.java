package one.lindegaard.BagOfGold.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.CustomItemsLib.rewards.Reward;

public class BagOfGoldImplementAPI implements BagOfGoldAPI {

	private final BagOfGold instance;

	/**
	 * Creates an API backed by the active BagOfGold instance.
	 */
	public BagOfGoldImplementAPI() {
		this(BagOfGold.getInstance());
	}

	public BagOfGoldImplementAPI(BagOfGold instance) {
		if (instance == null)
			throw new IllegalStateException("BagOfGold is not enabled");
		this.instance = instance;
	}

	/**
	 * Gets the BagOfgGold Instance
	 * 
	 * @return Instance
	 */
	public BagOfGold getBagOfGold() {
		return instance;
	}

	/**
	 * 
	 * @param itemStack
	 * @return true if the itemStack is a Money item (is a BagOfGold item or a
	 *         Gringotts item type )
	 */
	public boolean isMoney(ItemStack itemStack) {
		if (!Reward.isReward(itemStack))
			return false;
		Reward reward = Reward.getReward(itemStack);
		return reward != null && reward.checkHash() && reward.isMoney();
	}

	/**
	 * Get the value of the ItemStack
	 * 
	 * @param itemStack
	 * @return the value of the ItemStack. Returns 0 if the ItemStack is not a
	 *         BagOfGold item.
	 */
	public double getValue(ItemStack itemStack) {
		return isMoney(itemStack) ? Reward.getReward(itemStack).getMoney() : 0;
	}

	/**
	 * sets the value of the BagOfGOld Item.
	 * 
	 * @param itemStack
	 * @param value
	 */
	public void setValue(ItemStack itemStack, double value) {
		if (isMoney(itemStack))
			Reward.getReward(itemStack).setMoney(value);
	}

	/**
	 * Add the amount of money to the players balance. If there is not enough space
	 * in the players inventory, the money will be dropped on the ground in front of
	 * the player.
	 * 
	 * Updates BOTH player inventory and player balance
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return the amount of money added or dropped on ground.
	 */
	public boolean depositPlayer(OfflinePlayer offlinePlayer, double amount) {
		return getBagOfGold().getRewardManager().depositPlayer(offlinePlayer, amount);
	}

	/**
	 * withdrawPlayer : withdraw the amount from the players balance and remove the
	 * mount to the players inventory. Do not withdraw negative amounts.
	 * 
	 * Updates BOTH player inventory and player balance
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return true if amount of money was removed from the player.
	 */
	public boolean withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
		return getBagOfGold().getRewardManager().withdrawPlayer(offlinePlayer, amount);
	}

	/**
	 * Get the player balance from memory
	 * 
	 * @param offlinePlayer
	 * @return
	 */
	public double getBalance(OfflinePlayer offlinePlayer) {
		return getBagOfGold().getRewardManager().getBalance(offlinePlayer);
	}

	/**
	 * set the player balance in memory to the amount.
	 * 
	 * Updates ONLY player balance.
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return
	 */
	public boolean setbalance(OfflinePlayer offlinePlayer, double amount) {
		return getBagOfGold().getRewardManager().setbalance(offlinePlayer, amount);
	}

	/**
	 * Check if the player has amount of money in the player balance
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return true if the player has amount of money in the player balance.
	 */
	public boolean hasMoney(OfflinePlayer offlinePlayer, double amount) {
		return getBagOfGold().getRewardManager().hasMoney(offlinePlayer, amount);
	}

	/**
	 * Remove the amount from the player balance in memory/database without removing
	 * amount from the player inventory.
	 * 
	 * Updates ONLY the player balance.
	 * 
	 * @param offlinePlayer
	 * @param amount
	 */
	public void removeMoneyFromPlayerBalance(OfflinePlayer offlinePlayer, double amount) {
		getBagOfGold().getRewardManager().removeMoneyFromPlayerBalance(offlinePlayer, amount);
	}

	/**
	 * Calculate the total amount of physical money available in the player's
	 * inventory, including money stored in carried shulker boxes and bundles.
	 * 
	 * @param player
	 * @return
	 */
	public double getAmountInInventory(Player player) {
		return getBagOfGold().getRewardManager().getAmountInInventory(player);
	}

	@Override
	public double getAmountInPortableContainers(Player player) {
		return getBagOfGold().getBagOfGoldItems().getAmountInPortableContainers(player);
	}

	@Override
	public boolean hasMoneyInPortableContainers(Player player) {
		return getAmountInPortableContainers(player) > 0;
	}

	/**
	 * Add the amount of money to the players inventory
	 *
	 * Updates ONLY the player inventory.
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return the amount of money added. Not all will be added the there is not
	 *         enough space for the items in the player inventory.
	 */
	public double addMoneyToPlayer(Player player, double amount) {
		return getBagOfGold().getRewardManager().addMoneyToPlayer(player, amount);
	}

	/**
	 * Return the amount of money removed from the players inventory
	 * 
	 * Updates ONLY the player inventory.
	 * 
	 * @param offlinePlayer
	 * @param value
	 * @return
	 */
	public double removeMoneyFromPlayer(Player player, double amount) {
		return getBagOfGold().getRewardManager().removeMoneyFromPlayer(player, amount);
	}

}
