package one.lindegaard.BagOfGold.compatibility;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.CustomItemsLib.Tools;
import one.lindegaard.CustomItemsLib.compatibility.CompatPlugin;
import one.lindegaard.CustomItemsLib.rewards.Reward;

public class ShopkeepersCompat implements Listener {

	private static final String BALANCE_SOURCE_HINT = "shopkeepers-trade";

	private static Plugin mPlugin;
	private static boolean supported = false;

	private static class MoneyPayment {
		private final int slot;
		private final ItemStack template;
		private final double amount;

		private MoneyPayment(int slot, ItemStack template, double amount) {
			this.slot = slot;
			this.template = template;
			this.amount = amount;
		}
	}

	private static class PreparedPaymentState {
		private final int inventoryId;
		private int selectedIndex;
		private final double[] amounts = new double[2];

		private PreparedPaymentState(MerchantInventory inventory, int selectedIndex) {
			this.inventoryId = System.identityHashCode(inventory);
			this.selectedIndex = selectedIndex;
		}

		private double total() {
			return Tools.round(amounts[0] + amounts[1]);
		}
	}

	private final BagOfGold plugin;
	private final Map<UUID, PreparedPaymentState> preparedPayments = new HashMap<>();
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

			registerDynamicEvent(TradeSelectEvent.class, EventPriority.MONITOR,
					(listener, event) -> handleTradeSelect((TradeSelectEvent) event));
			registerDynamicEvent(InventoryClickEvent.class, EventPriority.LOWEST,
					(listener, event) -> handleInventoryClick((InventoryClickEvent) event), false);
			registerDynamicEvent(InventoryCloseEvent.class, EventPriority.LOWEST,
					(listener, event) -> handleInventoryClose((InventoryCloseEvent) event));
			registerDynamicEvent(tradeEventClass, EventPriority.HIGHEST, (listener, event) -> handleTrade(event));
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
		registerDynamicEvent(eventClass, EventPriority.MONITOR, executor);
	}

	private void registerDynamicEvent(Class<? extends Event> eventClass, EventPriority priority, EventExecutor executor) {
		registerDynamicEvent(eventClass, priority, executor, true);
	}

	private void registerDynamicEvent(Class<? extends Event> eventClass, EventPriority priority, EventExecutor executor,
			boolean ignoreCancelled) {
		Bukkit.getPluginManager().registerEvent(eventClass, this, priority, executor, plugin, ignoreCancelled);
	}

	private void handleTrade(Event event) {
		if (!supported || !tradeUsesBagOfGold(event))
			return;

		Player player = getPlayer(event);
		normalizeBagOfGoldPayment(event, player);
		clearPreparedPayments(player);
		shopkeepersDebug("trade uses BagOfGold money items: player=%s",
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

	private void handleTradeSelect(TradeSelectEvent event) {
		if (!supported || !(event.getWhoClicked() instanceof Player)) {
			shopkeepersDebug("TradeSelect ignored: supported=%s player=%s", supported,
					event.getWhoClicked() instanceof Player);
			return;
		}

		Player player = (Player) event.getWhoClicked();
		MerchantInventory inventory = event.getInventory();
		int selectedIndex = event.getIndex();
		MerchantRecipe recipe = getSelectedRecipe(inventory, selectedIndex);
		shopkeepersDebug("TradeSelect received: player=%s selectedIndex=%s selectedRecipe=%s", player.getName(),
				selectedIndex, describeRecipe(recipe));
		returnPreparedMoneyForSelectionChange(player, inventory, selectedIndex);
		Bukkit.getScheduler().runTask(plugin,
				() -> prepareSelectedMerchantPayment(player, inventory, selectedIndex));
	}

	private void handleInventoryClick(InventoryClickEvent event) {
		if (!supported || event.getView().getType() != InventoryType.MERCHANT
				|| !(event.getWhoClicked() instanceof Player) || !(event.getInventory() instanceof MerchantInventory))
			return;

		Player player = (Player) event.getWhoClicked();
		MerchantInventory inventory = (MerchantInventory) event.getInventory();

		if (event.getRawSlot() == 0 || event.getRawSlot() == 1) {
			if (hasTrackedPreparedPayment(player, inventory, event.getRawSlot())) {
				event.setCancelled(true);
				restoreTrackedPreparedPaymentSlot(player, inventory, event.getRawSlot());
				player.updateInventory();
				shopkeepersDebug("prepared merchant payment click blocked: player=%s slot=%s action=%s cursor=%s current=%s",
						player.getName(), event.getRawSlot(), event.getAction(), describeItem(event.getCursor()),
						describeItem(inventory.getItem(event.getRawSlot())));
				return;
			}

			if (handleMerchantMoneyInputClick(player, inventory, event))
				return;

			Bukkit.getScheduler().runTask(plugin,
					() -> prepareSelectedMerchantPayment(player, inventory, inventory.getSelectedRecipeIndex()));
			return;
		}

		if (event.getRawSlot() != 2)
			return;

		int selectedIndex = inventory.getSelectedRecipeIndex();
		returnPreparedMoneyForSelectionChange(player, inventory, selectedIndex);
		MerchantRecipe recipe = getSelectedRecipe(inventory, selectedIndex);
		shopkeepersDebug("result click received: player=%s action=%s cancelled=%s selectedIndex=%s slot0=%s slot1=%s result=%s recipe=%s",
				player.getName(), event.getAction(), event.isCancelled(), selectedIndex, describeItem(inventory.getItem(0)),
				describeItem(inventory.getItem(1)), describeItem(inventory.getItem(2)), describeRecipe(recipe));

		if (!prepareResultClickPayment(player, inventory, recipe))
			return;

		ItemStack result = recipe.getResult().clone();
		inventory.setItem(2, result);
		event.setCurrentItem(result);
		shopkeepersDebug("result click prepared: player=%s result=%s slot0=%s slot1=%s", player.getName(),
				describeItem(result), describeItem(inventory.getItem(0)), describeItem(inventory.getItem(1)));
	}

	private void handleInventoryClose(InventoryCloseEvent event) {
		if (!supported || !(event.getPlayer() instanceof Player) || !(event.getInventory() instanceof MerchantInventory))
			return;

		Player player = (Player) event.getPlayer();
		returnPreparedMoney(player, (MerchantInventory) event.getInventory());
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

	private ItemStack createMatchingMoneyItem(ItemStack template, double money) {
		if (!isBagOfGoldMoney(template))
			return null;
		if (Tools.round(getMoneyValue(template)) == Tools.round(money)) {
			ItemStack item = template.clone();
			item.setAmount(1);
			return item;
		}

		return createMoneyItem(template, money);
	}

	private void prepareSelectedMerchantPayment(Player player, MerchantInventory inventory, int selectedIndex) {
		if (player == null || !player.isOnline() || !player.isValid() || inventory == null)
			return;

		int currentSelectedIndex = inventory.getSelectedRecipeIndex();
		if (currentSelectedIndex >= 0 && currentSelectedIndex != selectedIndex) {
			shopkeepersDebug("selected trade payment skipped: player=%s staleIndex=%s currentIndex=%s",
					player.getName(), selectedIndex, currentSelectedIndex);
			return;
		}

		returnPreparedMoneyForSelectionChange(player, inventory, selectedIndex);
		MerchantRecipe recipe = getSelectedRecipe(inventory, selectedIndex);
		if (recipe == null)
			return;

		if (!prepareMerchantPayment(player, inventory, recipe, selectedIndex, true))
			return;

		inventory.setItem(2, recipe.getResult().clone());
		player.updateInventory();
		shopkeepersDebug("selected trade payment prepared: player=%s selectedIndex=%s slot0=%s slot1=%s result=%s",
				player.getName(), selectedIndex, describeItem(inventory.getItem(0)), describeItem(inventory.getItem(1)),
				describeItem(inventory.getItem(2)));
	}

	private boolean prepareResultClickPayment(Player player, MerchantInventory inventory, MerchantRecipe recipe) {
		if (player == null || !player.isOnline() || !player.isValid() || inventory == null || recipe == null) {
			shopkeepersDebug("result click payment skipped: invalid player, inventory, or recipe player=%s inventory=%s recipe=%s",
					player == null ? "null" : player.getName(), inventory == null ? "null" : "present",
					recipe == null ? "null" : "present");
			return false;
		}

		return prepareMerchantPayment(player, inventory, recipe, inventory.getSelectedRecipeIndex(), true);
	}

	private boolean prepareMerchantPayment(Player player, MerchantInventory inventory, MerchantRecipe recipe,
			int selectedIndex, boolean withdrawFromPlayer) {
		List<MoneyPayment> payments = getRequiredMoneyPayments(recipe);
		if (payments.isEmpty()) {
			shopkeepersDebug("merchant payment skipped: selected recipe does not require BagOfGold money");
			return false;
		}

		for (MoneyPayment payment : payments) {
			if (hasTrackedPreparedPayment(player, inventory, payment.slot)) {
				if (!restoreTrackedPreparedPayment(player, inventory, payment, selectedIndex))
					return false;
				continue;
			}

			ItemStack item = inventory.getItem(payment.slot);
			if (isBagOfGoldMoney(item)) {
				if (!normalizeMerchantMoneyInput(player, inventory, payment, item, selectedIndex))
					return false;
				continue;
			}

			if (!isEmpty(item)) {
				shopkeepersDebug("merchant payment blocked: slot=%s contains non-money item=%s", payment.slot,
						describeItem(item));
				return false;
			}

			if (!withdrawFromPlayer || !withdrawMoneyIntoMerchantInput(player, inventory, payment, selectedIndex))
				return false;
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	private boolean handleMerchantMoneyInputClick(Player player, MerchantInventory inventory, InventoryClickEvent event) {
		ItemStack cursor = event.getCursor();
		if (!isBagOfGoldMoney(cursor))
			return false;

		int slot = event.getRawSlot();
		MerchantRecipe recipe = getSelectedRecipe(inventory, inventory.getSelectedRecipeIndex());
		MoneyPayment payment = getRequiredMoneyPayment(recipe, slot);
		if (payment == null)
			return false;

		ItemStack current = inventory.getItem(slot);
		if (!isEmpty(current) && !isBagOfGoldMoney(current)) {
			shopkeepersDebug("manual money input blocked: player=%s slot=%s contains non-money item=%s",
					player.getName(), slot, describeItem(current));
			return false;
		}

		double cursorValue = getMoneyValue(cursor);
		double currentValue = isBagOfGoldMoney(current) ? getMoneyValue(current) : 0;
		double provided = Tools.round(cursorValue + currentValue);
		if (Tools.round(provided) < Tools.round(payment.amount)) {
			event.setCancelled(true);
			shopkeepersDebug("manual money input blocked: player=%s slot=%s required=%s provided=%s cursor=%s current=%s",
					player.getName(), slot, Tools.format(payment.amount), Tools.format(provided), describeItem(cursor),
					describeItem(current));
			return true;
		}

		ItemStack paymentItem = createMatchingMoneyItem(payment.template, payment.amount);
		if (paymentItem == null)
			return false;

		event.setCancelled(true);
		inventory.setItem(slot, paymentItem);
		event.setCursor(new ItemStack(Material.AIR));
		recordPreparedPayment(player, inventory, inventory.getSelectedRecipeIndex(), slot, payment.amount);

		double change = Tools.round(provided - payment.amount);
		double returned = returnMoneyToPlayerAndBalance(player, change);

		inventory.setItem(2, recipe.getResult().clone());
		player.updateInventory();
		shopkeepersDebug("manual money input normalized: player=%s slot=%s required=%s provided=%s returned=%s payment=%s",
				player.getName(), slot, Tools.format(payment.amount), Tools.format(provided), Tools.format(returned),
				describeItem(paymentItem));
		return true;
	}

	private boolean normalizeMerchantMoneyInput(Player player, MerchantInventory inventory, MoneyPayment payment,
			ItemStack item, int selectedIndex) {
		double value = getMoneyValue(item);
		if (Tools.round(value) < Tools.round(payment.amount)) {
			shopkeepersDebug("merchant payment not enough: player=%s slot=%s required=%s provided=%s",
					player.getName(), payment.slot, Tools.format(payment.amount), Tools.format(value));
			return false;
		}

		ItemStack paymentItem = createMatchingMoneyItem(payment.template, payment.amount);
		if (paymentItem == null)
			return false;

		inventory.setItem(payment.slot, paymentItem);
		recordPreparedPayment(player, inventory, selectedIndex, payment.slot, payment.amount);

		double change = Tools.round(value - payment.amount);
		if (change > 0) {
			double returned = returnMoneyToPlayerAndBalance(player, change);
			shopkeepersDebug("merchant payment normalized with change: player=%s slot=%s required=%s provided=%s change=%s",
					player.getName(), payment.slot, Tools.format(payment.amount), Tools.format(value),
					Tools.format(returned));
		} else {
			shopkeepersDebug("merchant payment normalized: player=%s slot=%s amount=%s", player.getName(), payment.slot,
					Tools.format(payment.amount));
		}
		return true;
	}

	private boolean withdrawMoneyIntoMerchantInput(Player player, MerchantInventory inventory, MoneyPayment payment,
			int selectedIndex) {
		double available = plugin.getRewardManager().getAmountInInventory(player);
		if (Tools.round(available) < Tools.round(payment.amount)) {
			shopkeepersDebug("merchant payment not prepared: player=%s needs=%s available=%s", player.getName(),
					Tools.format(payment.amount), Tools.format(available));
			return false;
		}

		ItemStack paymentItem = createMatchingMoneyItem(payment.template, payment.amount);
		if (paymentItem == null)
			return false;

		double removed = plugin.getRewardManager().removeMoneyFromPlayer(player, payment.amount);
		if (Tools.round(removed) < Tools.round(payment.amount)) {
			if (removed > 0)
				plugin.getRewardManager().addMoneyToPlayer(player, removed);
			shopkeepersDebug("merchant payment not prepared: removed=%s required=%s player=%s", Tools.format(removed),
					Tools.format(payment.amount), player.getName());
			return false;
		}

		inventory.setItem(payment.slot, paymentItem);
		recordPreparedPayment(player, inventory, selectedIndex, payment.slot, payment.amount);
		shopkeepersDebug("merchant payment prepared from inventory: player=%s slot=%s amount=%s item=%s",
				player.getName(), payment.slot, Tools.format(payment.amount), describeItem(paymentItem));
		return true;
	}

	private void recordPreparedPayment(Player player, MerchantInventory inventory, int selectedIndex, int slot,
			double amount) {
		if (player == null || inventory == null || slot < 0 || slot > 1 || amount <= 0)
			return;

		UUID uuid = player.getUniqueId();
		PreparedPaymentState state = preparedPayments.get(uuid);
		if (state == null || state.inventoryId != System.identityHashCode(inventory)) {
			state = new PreparedPaymentState(inventory, selectedIndex);
			preparedPayments.put(uuid, state);
		}

		state.selectedIndex = selectedIndex;
		state.amounts[slot] = Tools.round(amount);
	}

	private boolean hasTrackedPreparedPayment(Player player, MerchantInventory inventory, int slot) {
		PreparedPaymentState state = getPreparedPaymentState(player, inventory);
		return state != null && slot >= 0 && slot <= 1 && Tools.round(state.amounts[slot]) > 0;
	}

	private boolean restoreTrackedPreparedPaymentSlot(Player player, MerchantInventory inventory, int slot) {
		PreparedPaymentState state = getPreparedPaymentState(player, inventory);
		if (state == null || slot < 0 || slot > 1 || Tools.round(state.amounts[slot]) <= 0)
			return false;

		MerchantRecipe recipe = getSelectedRecipe(inventory, state.selectedIndex);
		MoneyPayment payment = getRequiredMoneyPayment(recipe, slot);
		if (payment == null)
			return false;

		return restoreTrackedPreparedPayment(player, inventory, payment, state.selectedIndex);
	}

	private boolean restoreTrackedPreparedPayment(Player player, MerchantInventory inventory, MoneyPayment payment,
			int selectedIndex) {
		PreparedPaymentState state = getPreparedPaymentState(player, inventory);
		if (state == null || payment == null || payment.slot < 0 || payment.slot > 1)
			return false;

		double trackedAmount = Tools.round(state.amounts[payment.slot]);
		if (trackedAmount <= 0)
			return false;
		if (state.selectedIndex != selectedIndex || Tools.round(trackedAmount) != Tools.round(payment.amount)) {
			shopkeepersDebug("tracked merchant payment mismatch: player=%s slot=%s trackedIndex=%s selectedIndex=%s tracked=%s required=%s",
					player.getName(), payment.slot, state.selectedIndex, selectedIndex, Tools.format(trackedAmount),
					Tools.format(payment.amount));
			return false;
		}

		ItemStack paymentItem = createMatchingMoneyItem(payment.template, payment.amount);
		if (paymentItem == null)
			return false;

		inventory.setItem(payment.slot, paymentItem);
		recordPreparedPayment(player, inventory, selectedIndex, payment.slot, payment.amount);
		shopkeepersDebug("merchant payment restored from tracked state: player=%s slot=%s amount=%s",
				player.getName(), payment.slot, Tools.format(payment.amount));
		return true;
	}

	private void clearPreparedPayments(Player player) {
		if (player != null)
			preparedPayments.remove(player.getUniqueId());
	}

	private void clearPreparedSlot(Player player, MerchantInventory inventory, int slot) {
		PreparedPaymentState state = getPreparedPaymentState(player, inventory);
		if (state == null || slot < 0 || slot > 1)
			return;

		state.amounts[slot] = 0;
		if (state.total() <= 0)
			preparedPayments.remove(player.getUniqueId());
	}

	private PreparedPaymentState getPreparedPaymentState(Player player, MerchantInventory inventory) {
		if (player == null || inventory == null)
			return null;

		PreparedPaymentState state = preparedPayments.get(player.getUniqueId());
		if (state == null)
			return null;
		if (state.inventoryId != System.identityHashCode(inventory)) {
			preparedPayments.remove(player.getUniqueId());
			return null;
		}
		return state;
	}

	private void returnPreparedMoneyForSelectionChange(Player player, MerchantInventory inventory, int selectedIndex) {
		PreparedPaymentState state = getPreparedPaymentState(player, inventory);
		if (state == null || state.total() <= 0 || state.selectedIndex == selectedIndex)
			return;

		shopkeepersDebug("selected trade changed: player=%s previousIndex=%s selectedIndex=%s returning=%s",
				player.getName(), state.selectedIndex, selectedIndex, Tools.format(state.total()));
		returnPreparedMoney(player, inventory);
		player.updateInventory();
	}

	private double returnMoneyToPlayerAndBalance(Player player, double amount) {
		if (player == null || amount <= 0)
			return 0;

		double returned = plugin.getRewardManager().addMoneyToPlayer(player, Tools.round(amount));
		if (Tools.round(returned) > 0)
			plugin.getRewardManager().addMoneyToPlayerBalance(player, Tools.round(returned));
		if (Tools.round(returned) < Tools.round(amount))
			shopkeepersDebug("returned only %s of %s to %s's inventory; the remainder may have been dropped",
					Tools.format(returned), Tools.format(amount), player.getName());
		return Tools.round(returned);
	}

	private boolean isEmpty(ItemStack item) {
		return item == null || item.getType() == Material.AIR;
	}

	private MoneyPayment getRequiredMoneyPayment(MerchantRecipe recipe, int slot) {
		for (MoneyPayment payment : getRequiredMoneyPayments(recipe)) {
			if (payment.slot == slot)
				return payment;
		}
		return null;
	}

	private List<MoneyPayment> getRequiredMoneyPayments(MerchantRecipe recipe) {
		List<MoneyPayment> payments = new ArrayList<>();
		if (recipe == null)
			return payments;

		List<ItemStack> ingredients = recipe.getIngredients();
		for (int slot = 0; slot < ingredients.size() && slot < 2; slot++) {
			ItemStack ingredient = ingredients.get(slot);
			shopkeepersDebug("recipe ingredient: slot=%s item=%s", slot, describeItem(ingredient));
			if (!isBagOfGoldMoney(ingredient))
				continue;

			double amount = getMoneyValue(ingredient);
			shopkeepersDebug("recipe money ingredient: slot=%s amount=%s", slot, Tools.format(amount));
			if (amount > 0)
				payments.add(new MoneyPayment(slot, ingredient, amount));
		}
		return payments;
	}

	private MerchantRecipe getSelectedRecipe(MerchantInventory inventory, int selectedIndex) {
		if (inventory == null)
			return null;

		Merchant merchant = inventory.getMerchant();
		if (merchant != null && selectedIndex >= 0 && selectedIndex < merchant.getRecipeCount()) {
			try {
				return merchant.getRecipe(selectedIndex);
			} catch (IndexOutOfBoundsException e) {
				shopkeepersDebug("selected recipe by index failed: selectedIndex=%s recipeCount=%s", selectedIndex,
						merchant.getRecipeCount());
			}
		}

		return inventory.getSelectedRecipe();
	}

	private void returnPreparedMoney(Player player, MerchantInventory inventory) {
		for (int slot = 0; slot <= 1; slot++) {
			ItemStack item = inventory.getItem(slot);
			PreparedPaymentState state = getPreparedPaymentState(player, inventory);
			double trackedAmount = state == null ? 0 : Tools.round(state.amounts[slot]);
			if (!isBagOfGoldMoney(item) && trackedAmount <= 0)
				continue;

			double amount = Math.max(getMoneyValue(item), trackedAmount);
			inventory.setItem(slot, null);
			if (amount > 0)
				returnMoneyToPlayerAndBalance(player, amount);
			clearPreparedSlot(player, inventory, slot);
			shopkeepersDebug("returned prepared payment: player=%s amount=%s",
					player.getName(), Tools.format(amount));
		}
	}

	private void normalizeBagOfGoldPayment(Event event, Player player) {
		Object tradingRecipe = invoke(event, "getTradingRecipe");
		if (tradingRecipe == null)
			return;

		double change = 0;
		change += normalizeReceivedMoneyItem(event, "getItem1", "getReceivedItem1", "setReceivedItem1");
		change += normalizeReceivedMoneyItem(event, "getItem2", "getReceivedItem2", "setReceivedItem2");
		if (Tools.round(change) > 0)
			addChangeTradeEffect(event, player, Tools.round(change));
	}

	private double normalizeReceivedMoneyItem(Event event, String recipeMethod, String receivedGetter,
			String receivedSetter) {
		Object tradingRecipe = invoke(event, "getTradingRecipe");
		ItemStack expected = getTradeItem(tradingRecipe, recipeMethod);
		ItemStack received = getTradeItem(event, receivedGetter);

		if (!isBagOfGoldMoney(expected))
			return 0;
		if (!isBagOfGoldMoney(received)) {
			cancelTrade(event, "This Shopkeepers trade expected BagOfGold money, but received a different item.");
			return 0;
		}

		double expectedValue = getMoneyValue(expected);
		double receivedValue = getMoneyValue(received);
		if (expectedValue <= 0 || receivedValue <= 0) {
			cancelTrade(event, "This Shopkeepers trade contains an invalid BagOfGold money item.");
			return 0;
		}
		if (Tools.round(receivedValue) < Tools.round(expectedValue)) {
			cancelTrade(event, "This Shopkeepers trade received less BagOfGold money than required.");
			return 0;
		}

		double change = Tools.round(receivedValue - expectedValue);
		if (change <= 0)
			return 0;

		ItemStack normalizedPayment = createMoneyItem(received, expectedValue);
		if (normalizedPayment == null) {
			cancelTrade(event, "Could not normalize the BagOfGold payment for this Shopkeepers trade.");
			return 0;
		}

		setUnmodifiableItemStack(event, receivedSetter, normalizedPayment);
		return change;
	}

	private double getMoneyValue(ItemStack item) {
		if (!isBagOfGoldMoney(item))
			return 0;

		Reward reward = Reward.getReward(item);
		if (!reward.checkHash())
			return 0;

		return Tools.round(reward.getMoney() * item.getAmount());
	}

	private ItemStack createMoneyItem(ItemStack template, double money) {
		if (!isBagOfGoldMoney(template))
			return null;

		ItemStack item = template.clone();
		item.setAmount(1);
		Reward reward = Reward.getReward(item);
		reward.setMoney(Tools.round(money));
		return Reward.setDisplayNameAndHiddenLores(item, reward);
	}

	@SuppressWarnings("unchecked")
	private void addChangeTradeEffect(Event event, Player player, double change) {
		if (player == null || change <= 0)
			return;

		Object tradeEffects = invoke(event, "getTradeEffects");
		if (!(tradeEffects instanceof List<?>))
			return;

		try {
			Class<?> tradeEffectClass = loadShopkeepersClass("com.nisovin.shopkeepers.api.trading.TradeEffect");
			Object tradeEffect = Proxy.newProxyInstance(mPlugin.getClass().getClassLoader(),
					new Class<?>[] { tradeEffectClass }, (proxy, method, args) -> {
						if ("onTradeApplied".equals(method.getName()))
							returnChange(player, change);
						return null;
					});
			((List<Object>) tradeEffects).add(tradeEffect);
			shopkeepersDebug("payment normalized: change=%s player=%s",
					Tools.format(change), player.getName());
		} catch (Exception e) {
			cancelTrade(event, "Could not register BagOfGold change handling for this Shopkeepers trade.");
			if (plugin.getConfigManager().debug)
				e.printStackTrace();
		}
	}

	private void returnChange(Player player, double change) {
		if (player == null || !player.isOnline() || !player.isValid() || change <= 0)
			return;

		double returned = returnMoneyToPlayerAndBalance(player, Tools.round(change));
		shopkeepersDebug("returned %s change to %s after trade", Tools.format(change),
				player.getName());
		if (Tools.round(returned) < Tools.round(change))
			shopkeepersDebug("could not return full trade change to %s: expected=%s returned=%s",
					player.getName(), Tools.format(change), Tools.format(returned));
	}

	private void setUnmodifiableItemStack(Event event, String setterName, ItemStack item) {
		try {
			Class<?> itemClass = loadShopkeepersClass("com.nisovin.shopkeepers.api.util.UnmodifiableItemStack");
			Object unmodifiableItem = itemClass.getMethod("of", ItemStack.class).invoke(null, item);
			event.getClass().getMethod(setterName, itemClass).invoke(event, unmodifiableItem);
		} catch (Exception e) {
			cancelTrade(event, "Could not update the BagOfGold payment item for this Shopkeepers trade.");
			if (plugin.getConfigManager().debug)
				e.printStackTrace();
		}
	}

	private void cancelTrade(Event event, String reason) {
		invoke(event, "setCancelled", boolean.class, true);
		Player player = getPlayer(event);
		if (player != null)
			player.sendMessage(ChatColor.RED + reason);
		shopkeepersDebug("trade cancelled: %s", reason);
	}

	private boolean isBagOfGoldMoney(ItemStack item) {
		if (item == null)
			return false;
		if (!Reward.isReward(item))
			return false;

		Reward reward = Reward.getReward(item);
		return reward.isMoney() && (reward.isBagOfGoldReward() || reward.isItemReward());
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

	private Object invoke(Object target, String methodName, Class<?> parameterType, Object value) {
		try {
			Method method = target.getClass().getMethod(methodName, parameterType);
			return method.invoke(target, value);
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
			shopkeepersDebug("balance sync skipped for %s in %s mode",
					player.getName(), player.getGameMode());
			return;
		}

		plugin.getRewardManager().adjustPlayerBalanceToAmounOfMoneyInInventory(player, BALANCE_SOURCE_HINT);
	}

	private void shopkeepersDebug(String message, Object... args) {
		if (plugin.getConfigManager().debug || plugin.getConfigManager().debugIntegrationShopkeepers) {
			Bukkit.getServer().getConsoleSender()
					.sendMessage(BagOfGold.PREFIX_DEBUG + "[Shopkeepers] " + String.format(message, args));
		}
	}

	private String describeRecipe(MerchantRecipe recipe) {
		if (recipe == null)
			return "null";

		List<ItemStack> ingredients = recipe.getIngredients();
		StringBuilder builder = new StringBuilder();
		builder.append("result=").append(describeItem(recipe.getResult()));
		builder.append(" ingredients=").append(ingredients.size());
		for (int i = 0; i < ingredients.size(); i++)
			builder.append(" [").append(i).append("]=").append(describeItem(ingredients.get(i)));
		return builder.toString();
	}

	private String describeItem(ItemStack item) {
		if (item == null)
			return "empty";

		StringBuilder builder = new StringBuilder(item.getType().name());
		builder.append("x").append(item.getAmount());
		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
			builder.append(" name=").append(ChatColor.stripColor(item.getItemMeta().getDisplayName()));

		if (Reward.isReward(item)) {
			Reward reward = Reward.getReward(item);
			builder.append(" reward=true money=").append(reward.isMoney());
			builder.append(" bag=").append(reward.isBagOfGoldReward());
			builder.append(" itemReward=").append(reward.isItemReward());
			builder.append(" hash=").append(reward.checkHash());
			builder.append(" value=").append(Tools.format(getMoneyValue(item)));
		} else {
			builder.append(" reward=false");
		}
		return builder.toString();
	}

}
