package one.lindegaard.BagOfGold.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeCompletedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.events.ShopkeepersStartupEvent;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.CustomItemsLib.Core;
import one.lindegaard.CustomItemsLib.compatibility.CompatPlugin;
import one.lindegaard.CustomItemsLib.rewards.Reward;

public class ShopkeepersCompat implements Listener {

	private static final String BALANCE_SOURCE_HINT = "shopkeepers-trade";

	private static Plugin mPlugin;
	private static boolean supported = false;

	private final BagOfGold plugin;

	public ShopkeepersCompat() {
		plugin = BagOfGold.getInstance();
		Bukkit.getPluginManager().registerEvents(this, plugin);

		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender()
					.sendMessage(BagOfGold.PREFIX + "Compatibility with Shopkeepers is disabled in config.yml");
			return;
		}

		mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.Shopkeepers.getName());
		if (mPlugin == null) {
			Bukkit.getConsoleSender().sendMessage(
					BagOfGold.PREFIX + "Shopkeepers compatibility is enabled, but Shopkeepers is not installed.");
			return;
		}

		enableIfShopkeepersApiReady();
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static Plugin getShopkeepers() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return BagOfGold.getInstance().getConfigManager().enableIntegrationShopkeepersBETA;
	}

	private void enableIfShopkeepersApiReady() {
		if (!isEnabledInConfig() || mPlugin == null) {
			supported = false;
			return;
		}

		if (!ShopkeepersAPI.isEnabled()) {
			Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX
					+ "Shopkeepers compatibility is waiting for the Shopkeepers API to finish loading.");
			supported = false;
			return;
		}

		supported = true;
		Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX + "Enabling compatibility with Shopkeepers ("
				+ mPlugin.getDescription().getVersion() + ")");
	}

	private boolean tradeUsesBagOfGold(ShopkeeperTradeEvent event) {
		return isBagOfGoldMoney(event.getOfferedItem1()) || isBagOfGoldMoney(event.getOfferedItem2())
				|| isBagOfGoldMoney(event.getReceivedItem1()) || isBagOfGoldMoney(event.getReceivedItem2())
				|| isBagOfGoldMoney(event.getResultItem());
	}

	private boolean isBagOfGoldMoney(UnmodifiableItemStack item) {
		if (item == null)
			return false;
		return isBagOfGoldMoney(item.copy());
	}

	private boolean isBagOfGoldMoney(ItemStack item) {
		if (!Reward.isReward(item))
			return false;

		Reward reward = Reward.getReward(item);
		return reward.isBagOfGoldReward() || reward.isItemReward();
	}

	private void syncPlayerBalanceAfterTrade(Player player) {
		if (player == null || !player.isOnline() || !player.isValid())
			return;

		if (player.getGameMode() != GameMode.SURVIVAL) {
			Core.getMessages().debug("Shopkeepers trade skipped BagOfGold balance sync for %s in %s mode",
					player.getName(), player.getGameMode());
			return;
		}

		plugin.getRewardManager().adjustPlayerBalanceToAmounOfMoneyInInventory(player, BALANCE_SOURCE_HINT);
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

	@EventHandler(priority = EventPriority.MONITOR)
	public void onShopkeepersStartup(ShopkeepersStartupEvent event) {
		enableIfShopkeepersApiReady();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopkeeperTrade(ShopkeeperTradeEvent event) {
		if (!supported || !tradeUsesBagOfGold(event))
			return;

		Core.getMessages().debug("Shopkeepers trade uses BagOfGold money items: player=%s, shopkeeper=%s",
				event.getPlayer().getName(), event.getShopkeeper().getIdString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onShopkeeperTradeCompleted(ShopkeeperTradeCompletedEvent event) {
		if (!supported || !tradeUsesBagOfGold(event.getCompletedTrade()))
			return;

		Player player = event.getCompletedTrade().getPlayer();
		Bukkit.getScheduler().runTask(plugin, () -> syncPlayerBalanceAfterTrade(player));
	}

}
