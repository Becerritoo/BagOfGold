package one.lindegaard.BagOfGold.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.CustomItemsLib.Core;
import one.lindegaard.CustomItemsLib.compatibility.CompatPlugin;
import one.lindegaard.CustomItemsLib.rewards.Reward;

public class ShopkeepersCompat implements Listener {

	private static final String BALANCE_SOURCE_HINT = "shopkeepers-trade";

	private static Plugin mPlugin;
	private static boolean supported = false;

	private final BagOfGold plugin;
	private boolean startupListenerRegistered = false;
	private boolean tradeListenersRegistered = false;

	public ShopkeepersCompat() {
		plugin = BagOfGold.getInstance();

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

		if (!isShopkeepersApiReady()) {
			registerStartupListener();
			Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX
					+ "Shopkeepers compatibility is waiting for the Shopkeepers API to finish loading.");
			supported = false;
			return;
		}

		if (!registerTradeListeners()) {
			supported = false;
			return;
		}

		if (supported)
			return;

		supported = true;
		Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX + "Enabling compatibility with Shopkeepers ("
				+ mPlugin.getDescription().getVersion() + ")");
	}

	private boolean isShopkeepersApiReady() {
		try {
			Class<?> apiClass = loadShopkeepersClass("com.nisovin.shopkeepers.api.ShopkeepersAPI");
			Object result = apiClass.getMethod("isEnabled").invoke(null);
			return Boolean.TRUE.equals(result);
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX_WARNING
					+ "Could not check Shopkeepers API state. Compatibility will stay disabled for now.");
			if (plugin.getConfigManager().debug)
				e.printStackTrace();
			return false;
		}
	}

	private Class<?> loadShopkeepersClass(String className) throws ClassNotFoundException {
		return Class.forName(className, false, mPlugin.getClass().getClassLoader());
	}

	@SuppressWarnings("unchecked")
	private boolean registerStartupListener() {
		if (startupListenerRegistered)
			return true;

		try {
			Class<? extends Event> eventClass = (Class<? extends Event>) loadShopkeepersClass(
					"com.nisovin.shopkeepers.api.events.ShopkeepersStartupEvent");
			registerDynamicEvent(eventClass, (listener, event) -> enableIfShopkeepersApiReady());
			startupListenerRegistered = true;
			return true;
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX_WARNING
					+ "Could not register Shopkeepers startup listener. Compatibility is disabled.");
			if (plugin.getConfigManager().debug)
				e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean registerTradeListeners() {
		if (tradeListenersRegistered)
			return true;

		try {
			Class<? extends Event> tradeEventClass = (Class<? extends Event>) loadShopkeepersClass(
					"com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent");
			Class<? extends Event> tradeCompletedEventClass = (Class<? extends Event>) loadShopkeepersClass(
					"com.nisovin.shopkeepers.api.events.ShopkeeperTradeCompletedEvent");

			registerDynamicEvent(tradeEventClass, (listener, event) -> handleTrade(event));
			registerDynamicEvent(tradeCompletedEventClass, (listener, event) -> handleTradeCompleted(event));
			tradeListenersRegistered = true;
			return true;
		} catch (Exception e) {
			Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX_WARNING
					+ "Could not register Shopkeepers trade listeners. Compatibility is disabled.");
			if (plugin.getConfigManager().debug)
				e.printStackTrace();
			return false;
		}
	}

	private void registerDynamicEvent(Class<? extends Event> eventClass, EventExecutor executor) {
		Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, executor, plugin, true);
	}

	private void handleTrade(Event event) {
		if (!supported || !tradeUsesBagOfGold(event))
			return;

		Player player = getPlayer(event);
		Core.getMessages().debug("Shopkeepers trade uses BagOfGold money items: player=%s",
				player == null ? "unknown" : player.getName());
	}

	private void handleTradeCompleted(Event event) {
		if (!supported)
			return;

		Object completedTrade = invoke(event, "getCompletedTrade");
		if (completedTrade == null || !tradeUsesBagOfGold(completedTrade))
			return;

		Player player = getPlayer(completedTrade);
		Bukkit.getScheduler().runTask(plugin, () -> syncPlayerBalanceAfterTrade(player));
	}

	private boolean tradeUsesBagOfGold(Object trade) {
		return isBagOfGoldMoney(getTradeItem(trade, "getOfferedItem1"))
				|| isBagOfGoldMoney(getTradeItem(trade, "getOfferedItem2"))
				|| isBagOfGoldMoney(getTradeItem(trade, "getReceivedItem1"))
				|| isBagOfGoldMoney(getTradeItem(trade, "getReceivedItem2"))
				|| isBagOfGoldMoney(getTradeItem(trade, "getResultItem"));
	}

	private ItemStack getTradeItem(Object trade, String methodName) {
		Object item = invoke(trade, methodName);
		if (item == null)
			return null;
		if (item instanceof ItemStack)
			return (ItemStack) item;

		Object copy = invoke(item, "copy");
		if (copy instanceof ItemStack)
			return (ItemStack) copy;

		return null;
	}

	private boolean isBagOfGoldMoney(ItemStack item) {
		if (item == null)
			return false;
		if (!Reward.isReward(item))
			return false;

		Reward reward = Reward.getReward(item);
		return reward.isBagOfGoldReward() || reward.isItemReward();
	}

	private Player getPlayer(Object eventOrTrade) {
		Object player = invoke(eventOrTrade, "getPlayer");
		if (player instanceof Player)
			return (Player) player;
		return null;
	}

	private Object invoke(Object target, String methodName) {
		try {
			return target.getClass().getMethod(methodName).invoke(target);
		} catch (Exception e) {
			if (plugin.getConfigManager().debug)
				e.printStackTrace();
			return null;
		}
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

}
