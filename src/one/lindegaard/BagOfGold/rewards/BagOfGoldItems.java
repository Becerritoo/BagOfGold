package one.lindegaard.BagOfGold.rewards;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.CustomItemsLib.Core;
import one.lindegaard.CustomItemsLib.Tools;
import one.lindegaard.CustomItemsLib.rewards.CoreCustomItems;
import one.lindegaard.CustomItemsLib.rewards.Reward;
import one.lindegaard.CustomItemsLib.rewards.RewardType;
import one.lindegaard.CustomItemsLib.rewards.TokenSpendStore;
import one.lindegaard.CustomItemsLib.rewards.TokenSpendStore.TokenSpend;
import one.lindegaard.CustomItemsLib.server.Servers;

public class BagOfGoldItems implements Listener {

	private static final int MAX_PORTABLE_CONTAINER_DEPTH = 16;

	BagOfGold plugin;

	public BagOfGoldItems(BagOfGold plugin) {
		this.plugin = plugin;
		if (isBagOfGoldStyle()) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
		}
	}

	public boolean isBagOfGoldStyle() {
		return Core.getConfigManager().rewardItemtype.equalsIgnoreCase("SKULL")
				|| Core.getConfigManager().rewardItemtype.equalsIgnoreCase("ITEM")
				|| Core.getConfigManager().rewardItemtype.equalsIgnoreCase("KILLED")
				|| Core.getConfigManager().rewardItemtype.equalsIgnoreCase("KILLER");
	}

	public void dropBagOfGoldMoneyOnGround(Player player, Entity killedEntity, Location location, double money) {
		Item item = null;
		double moneyLeftToDrop = Tools.ceil(money);
		ItemStack is;
		UUID skinuuid = null;
		RewardType rewardType;
		double nextBag = 0;
		while (moneyLeftToDrop > 0) {
			if (moneyLeftToDrop / Core.getConfigManager().limitPerBag > 100) {
				moneyLeftToDrop = 100 * Core.getConfigManager().limitPerBag;
				plugin.getMessages().debug(
						"To many Bags is going to be dropped. The number of Bags are limited to 100, not to crash the server. MoneyToBeDropped=%s (number of bags = %s",
						moneyLeftToDrop, moneyLeftToDrop / Core.getConfigManager().limitPerBag);
			}

			if (moneyLeftToDrop > Core.getConfigManager().limitPerBag) {
				nextBag = Core.getConfigManager().limitPerBag;
				moneyLeftToDrop = Tools.round(moneyLeftToDrop - nextBag);
			} else {
				nextBag = Tools.round(moneyLeftToDrop);
				moneyLeftToDrop = 0;
			}

			if (Core.getConfigManager().rewardItemtype.equalsIgnoreCase("SKULL")) {
				rewardType = RewardType.BAGOFGOLD;
				skinuuid = UUID.fromString(RewardType.BAGOFGOLD.getUUID());
				is = CoreCustomItems.getCustomtexture(
						new Reward(Core.getConfigManager().bagOfGoldName, nextBag, rewardType, skinuuid),
						Core.getConfigManager().skullTextureValue, Core.getConfigManager().skullTextureSignature);
			} else { // ITEM
				rewardType = RewardType.ITEM;
				skinuuid = null;
				is = new ItemStack(Material.valueOf(Core.getConfigManager().rewardItem), 1);
			}

			Reward reward = new Reward(
					ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + Core.getConfigManager().bagOfGoldName,
					nextBag, rewardType, skinuuid);
			is = Reward.setDisplayNameAndHiddenLores(is, reward);

			item = location.getWorld().dropItem(location, is);

			if (item != null) {
				Core.getCoreRewardManager().getDroppedMoney().put(item.getEntityId(), nextBag);
				item.setMetadata(Reward.MH_REWARD_DATA_NEW, new FixedMetadataValue(plugin, new Reward(reward)));
				item.setCustomName(is.getItemMeta().getDisplayName());
				item.setCustomNameVisible(Core.getConfigManager().showCustomDisplayname);
				if (player != null)
					plugin.getMessages().debug("%s dropped %s on the ground as item %s (# of rewards=%s)(3)",
							player.getName(), Tools.format(nextBag), Core.getConfigManager().rewardItemtype,
							Core.getCoreRewardManager().getDroppedMoney().size());
				else
					plugin.getMessages().debug("A %s(%s) was dropped on the ground as item %s (# of rewards=%s)(3)",
							Core.getConfigManager().rewardItemtype, Tools.format(nextBag),
							Core.getConfigManager().rewardItemtype,
							Core.getCoreRewardManager().getDroppedMoney().size());

			}
		}
	}

	public double getAmountOfBagOfGoldMoneyInInventory(Player player) {
		double amountInInventory = 0;

		for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
			ItemStack is = player.getInventory().getItem(slot);
			amountInInventory += getMoneyInItemAndPortableContents(player, is, 0);
		}
		return amountInInventory;
	}

	public double getAmountInPortableContainers(Player player) {
		double amount = 0;
		for (ItemStack item : player.getInventory().getContents())
			amount += getMoneyInPortableContainer(player, item);
		return amount;
	}

	public double getAmountInItemIncludingPortableContents(Player player, ItemStack item) {
		return Tools.round(getMoneyInItemAndPortableContents(player, item, 0));
	}

	private double getMoneyInPortableContainer(Player player, ItemStack containerItem) {
		return getMoneyInPortableContainer(player, containerItem, 0);
	}

	private double getMoneyInItemAndPortableContents(Player player, ItemStack item, int depth) {
		if (item == null || item.getType().isAir())
			return 0;

		double amount = getMoneyInItem(player, item);
		if (depth < MAX_PORTABLE_CONTAINER_DEPTH)
			amount += getMoneyInPortableContainer(player, item, depth);
		return amount;
	}

	private double getMoneyInPortableContainer(Player player, ItemStack containerItem, int depth) {
		if (depth >= MAX_PORTABLE_CONTAINER_DEPTH)
			return 0;
		if (containerItem == null || !containerItem.hasItemMeta())
			return 0;

		ItemMeta meta = containerItem.getItemMeta();
		if (meta instanceof BlockStateMeta blockStateMeta
				&& blockStateMeta.getBlockState() instanceof ShulkerBox shulker)
			return getMoneyInInventory(player, shulker.getSnapshotInventory(), depth + 1);

		if (meta instanceof BundleMeta bundleMeta && bundleMeta.hasItems()) {
			double amount = 0;
			for (ItemStack item : bundleMeta.getItems())
				amount += getMoneyInItemAndPortableContents(player, item, depth + 1);
			return amount;
		}
		return 0;
	}

	private boolean hasMoneyInPortableContainer(Player player, ItemStack item) {
		return getMoneyInPortableContainer(player, item) > 0;
	}

	private double getMoneyInInventory(Player player, Inventory inventory, int depth) {
		double amount = 0;
		for (ItemStack item : inventory.getStorageContents())
			amount += getMoneyInItemAndPortableContents(player, item, depth);
		return amount;
	}

	private double getMoneyInItem(Player player, ItemStack item) {
		if (!Reward.isReward(item))
			return 0;

		Reward reward = Reward.getReward(item);
		if (reward.checkHash())
			return reward.isMoney() ? reward.getMoney() * item.getAmount() : 0;

		Bukkit.getConsoleSender().sendMessage(
				ChatColor.GOLD + "[BagOfGold]" + ChatColor.RED + "[Warning] " + player.getName()
						+ " has tried to change the value of a BagOfGold Item. Value set to 0!(3)");
		reward.setMoney(0);
		Reward.setDisplayNameAndHiddenLores(item, reward);
		return 0;
	}

	private static final class PlannedSlotUpdate {
		private final int slot;
		private final ItemStack item;

		private PlannedSlotUpdate(int slot, ItemStack item) {
			this.slot = slot;
			this.item = item;
		}
	}

	private static final class PhysicalSpendPlan {
		private double remaining;
		private final UUID playerUuid;
		private final Set<String> tokenUuids = new HashSet<>();
		private final List<TokenSpend> tokenSpends = new ArrayList<>();
		private final List<PlannedSlotUpdate> slotUpdates = new ArrayList<>();
		private boolean duplicateToken;
		private boolean malformedStack;
		private boolean cursorChanged;
		private ItemStack cursorItem;

		private PhysicalSpendPlan(Player player, double amount) {
			playerUuid = player.getUniqueId();
			remaining = Tools.round(amount);
		}

		private void claim(Reward reward, double amount, String source) {
			String tokenUuid = reward.getTokenUUID();
			if (tokenUuid == null || tokenUuid.isEmpty() || !tokenUuids.add(tokenUuid)) {
				duplicateToken = true;
				return;
			}
			if (amount > 0)
				tokenSpends.add(new TokenSpend(tokenUuid, playerUuid, source, amount));
		}
	}

	/**
	 * Remove physical currency as one transaction. Inventory mutations are planned
	 * on clones first. The real inventory is changed only after the exact amount is
	 * available and every consumed token has been claimed atomically.
	 */
	public double removeBagOfGoldMoneyAtomically(Player player, double amount) {
		double requested = Tools.round(amount);
		if (requested <= 0)
			return 0;

		PhysicalSpendPlan plan = new PhysicalSpendPlan(player, requested);
		for (int slot = player.getInventory().getSize() - 1; slot >= 0; slot--) {
			ItemStack original = player.getInventory().getItem(slot);
			if (original == null || original.getType().isAir())
				continue;

			int mutationsBefore = plan.tokenSpends.size();
			ItemStack updated = planMoneyRemoval(player, original.clone(), plan, 0, "inventory-spend");
			if (plan.tokenSpends.size() != mutationsBefore)
				plan.slotUpdates.add(new PlannedSlotUpdate(slot, updated));
		}

		ItemStack cursor = player.getItemOnCursor();
		if (cursor != null && !cursor.getType().isAir()) {
			int mutationsBefore = plan.tokenSpends.size();
			plan.cursorItem = planMoneyRemoval(player, cursor.clone(), plan, 0, "inventory-cursor-spend");
			plan.cursorChanged = plan.tokenSpends.size() != mutationsBefore;
		}

		if (plan.duplicateToken || plan.malformedStack) {
			plugin.getMessages().debug(
					"Rejected physical-currency transaction for %s because the inventory contains duplicated or stacked token data.",
					player.getName());
			return 0;
		}
		if (Tools.round(plan.remaining) > 0 || plan.tokenSpends.isEmpty())
			return 0;
		if (Core.getTokenSpendStore() == null) {
			plugin.getMessages().debug("Rejected physical-currency transaction for %s because the token store is unavailable.",
					player.getName());
			return 0;
		}

		TokenSpendStore.MarkResult result = Core.getTokenSpendStore().markTokensSpent(plan.tokenSpends);
		if (result != TokenSpendStore.MarkResult.MARKED) {
			plugin.getMessages().debug("Rejected physical-currency transaction for %s because token claim returned %s.",
					player.getName(), result);
			return 0;
		}

		for (PlannedSlotUpdate update : plan.slotUpdates)
			player.getInventory().setItem(update.slot, update.item);
		if (plan.cursorChanged)
			player.setItemOnCursor(plan.cursorItem);
		return requested;
	}

	private ItemStack planMoneyRemoval(Player player, ItemStack item, PhysicalSpendPlan plan, int depth,
			String source) {
		if (item == null || item.getType().isAir())
			return item;

		if (Reward.isReward(item)) {
			Reward reward = Reward.getReward(item);
			if (!reward.checkHash()) {
				Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX_WARNING + player.getName()
						+ " has an invalid BagOfGold item; it was excluded from the transaction.");
				return item;
			}
			if (!reward.isMoney() || Tools.round(reward.getMoney()) <= 0)
				return item;
			if (item.getAmount() != 1) {
				plan.malformedStack = true;
				return item;
			}

			double consumed = plan.remaining > 0 ? Math.min(Tools.round(reward.getMoney()), plan.remaining) : 0;
			plan.claim(reward, consumed, source);
			if (consumed <= 0 || plan.duplicateToken)
				return item;

			plan.remaining = Tools.round(plan.remaining - consumed);
			if (Tools.round(consumed) >= Tools.round(reward.getMoney()))
				return null;

			reward.setMoney(Tools.round(reward.getMoney() - consumed));
			ItemStack remainder = Reward.setDisplayNameAndHiddenLores(item, reward);
			remainder.setAmount(1);
			return remainder;
		}

		if (depth >= MAX_PORTABLE_CONTAINER_DEPTH || !item.hasItemMeta())
			return item;

		ItemMeta meta = item.getItemMeta();
		if (meta instanceof BlockStateMeta blockStateMeta
				&& blockStateMeta.getBlockState() instanceof ShulkerBox shulker) {
			Inventory inventory = shulker.getSnapshotInventory();
			ItemStack[] contents = inventory.getStorageContents();
			boolean changed = false;
			for (int slot = contents.length - 1; slot >= 0; slot--) {
				ItemStack original = contents[slot];
				if (original == null || original.getType().isAir())
					continue;
				int mutationsBefore = plan.tokenSpends.size();
				contents[slot] = planMoneyRemoval(player, original.clone(), plan, depth + 1,
						"portable-container-spend");
				changed |= plan.tokenSpends.size() != mutationsBefore;
			}
			if (changed) {
				inventory.setStorageContents(contents);
				blockStateMeta.setBlockState(shulker);
				item.setItemMeta(blockStateMeta);
			}
			return item;
		}

		if (meta instanceof BundleMeta bundleMeta && bundleMeta.hasItems()) {
			List<ItemStack> contents = new ArrayList<>(bundleMeta.getItems());
			boolean changed = false;
			for (int slot = contents.size() - 1; slot >= 0; slot--) {
				ItemStack original = contents.get(slot);
				if (original == null || original.getType().isAir())
					continue;
				int mutationsBefore = plan.tokenSpends.size();
				contents.set(slot, planMoneyRemoval(player, original.clone(), plan, depth + 1,
						"portable-container-spend"));
				changed |= plan.tokenSpends.size() != mutationsBefore;
			}
			if (changed) {
				contents.removeIf(content -> content == null || content.getType().isAir());
				bundleMeta.setItems(contents);
				item.setItemMeta(bundleMeta);
			}
		}
		return item;
	}

	private void syncPortableMoneyOnNextTick(Player player, String sourceHint) {
		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player.isOnline() && player.isValid())
				plugin.getRewardManager().adjustPlayerBalanceToAmounOfMoneyInInventory(player, sourceHint);
		});
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPortableMoneyPickup(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player
				&& hasMoneyInPortableContainer(player, event.getItem().getItemStack()))
			syncPortableMoneyOnNextTick(player, "portable-container-pickup");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPortableMoneyDrop(PlayerDropItemEvent event) {
		Player player = event.getPlayer();
		if (hasMoneyInPortableContainer(player, event.getItemDrop().getItemStack()))
			syncPortableMoneyOnNextTick(player, "portable-container-drop");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPortableMoneyPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (hasMoneyInPortableContainer(player, event.getItemInHand()))
			syncPortableMoneyOnNextTick(player, "portable-container-place");
	}

	public double getSpaceForBagOfGoldMoney(Player player) {
		double space = 0;
		for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
			if (slot > 35)
				continue;
			ItemStack is = player.getInventory().getItem(slot);
			if (Reward.isReward(is)) {
				Reward rewardInSlot = Reward.getReward(is);
				int amount = is.getAmount();
				if (rewardInSlot.checkHash()) {
					if (rewardInSlot.isMoney())
						space = space + Core.getConfigManager().limitPerBag - rewardInSlot.getMoney() * amount;

				} else {
					Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX_WARNING + player.getName()
							+ " has tried to change the value of a BagOfGold Item. Value set to 0!(4)");
					rewardInSlot.setMoney(0);
					is = Reward.setDisplayNameAndHiddenLores(is, rewardInSlot);
				}
			} else if (is == null || is.getType() == Material.AIR) {
				space = space + Core.getConfigManager().limitPerBag;
			}
		}
		plugin.getMessages().debug("%s has room for %s BagOfGold in the inventory", player.getName(), space);
		return space;
	}

	// ***********************************************************************************
	// EVENTS
	// ***********************************************************************************

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {

		if (event.isCancelled())
			return;

		if (event.getRightClicked().getLocation() == null)
			return;

		Player player = event.getPlayer();
		if (event.getRightClicked().getType() == EntityType.ITEM_FRAME
				&& hasMoneyInPortableContainer(player, player.getInventory().getItemInMainHand()))
			syncPortableMoneyOnNextTick(player, "portable-container-itemframe-place");

		if (event.getRightClicked().getType() == EntityType.ITEM_FRAME
				&& Reward.isReward(player.getInventory().getItemInMainHand())) {
			Reward reward = Reward.getReward(player.getInventory().getItemInMainHand());
				if (reward.getMoney() != 0) {
					plugin.getMessages().debug("onPlayerInteractEntityEvent: %s placed a BagOfGod in an ItemFrame",
							player.getName());
					Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
						@Override
						public void run() {
							if (player.isOnline() && player.isValid())
								plugin.getRewardManager().adjustPlayerBalanceToAmounOfMoneyInInventory(player,
										"itemframe-place");
						}
					}, 1L);
					if (!Core.getPlayerSettingsManager().getPlayerSettings(player).isMuted())
						plugin.getMessages().playerActionBarMessageQueue(player,
								plugin.getMessages().getString("bagofgold.moneyframe", Core.PH_MONEY,
										Tools.round(reward.getMoney()), Core.PH_REWARDNAME,
										ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
												+ Core.getConfigManager().bagOfGoldName.trim()));
				}
			}
		}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.isCancelled())
			return;

		if (event.getClickedBlock() == null)
			return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;

		if (Servers.isMC19OrNewer() && event.getHand() != EquipmentSlot.HAND)
			return;

		Player player = event.getPlayer();

		Block block = event.getClickedBlock();

		if (Reward.isReward(block)) {
			Reward reward = Reward.getReward(block);
			if (!Core.getPlayerSettingsManager().getPlayerSettings(player).isMuted()) {
				if (reward.getMoney() == 0)
					plugin.getMessages().playerActionBarMessageQueue(player,
							ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + reward.getDisplayName());
				else
					plugin.getMessages().playerActionBarMessageQueue(player,
							ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
									+ (Core.getConfigManager().rewardItemtype.equalsIgnoreCase("ITEM")
											? Tools.format(reward.getMoney())
											: reward.getDisplayName() + " (" + Tools.format(reward.getMoney()) + ")"));
			}
		} else if (Servers.isMC113OrNewer()) {
			if (block.getType() == Material.PLAYER_HEAD || block.getType() == Material.PLAYER_WALL_HEAD) {
				Skull skullState = (Skull) block.getState();
				OfflinePlayer owner = skullState.getOwningPlayer();
				if (owner != null && owner.getName() != null
						&& !Core.getPlayerSettingsManager().getPlayerSettings(player).isMuted())
					plugin.getMessages().playerActionBarMessageQueue(player,
							ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + owner.getName());
			}
		} else {
			if (block.getType() == Material.matchMaterial("SKULL_ITEM")
					|| block.getType() == Material.matchMaterial("SKULL")) {
				Skull skullState = (Skull) block.getState();
				OfflinePlayer owner = skullState.getOwningPlayer();
				if (owner != null && owner.getName() != null
						&& !Core.getPlayerSettingsManager().getPlayerSettings(player).isMuted())
					plugin.getMessages().playerActionBarMessageQueue(player,
							ChatColor.valueOf(Core.getConfigManager().rewardTextColor) + owner.getName());
			}
		}
	}

	@EventHandler
	public void onVillagerTradeEvent(InventoryClickEvent event) {
		if (event.getClickedInventory() instanceof MerchantInventory inventory) {
			Integer slotClick = event.getSlot();
			// plugin.getMessages().debug("onVillagetTradeEvent:
			// slot=%s",slotClick.toString());
			MerchantInventory villagerMerchantInventory = inventory;
			ItemStack slotItem = villagerMerchantInventory.getItem(slotClick);
			// plugin.getMessages().debug("onVillagetTradeEvent:
			// slotItem=%s",slotItem.toString());
			MerchantRecipe villagerMerchantRecipe = villagerMerchantInventory.getSelectedRecipe();
			// plugin.getMessages().debug("onVillagetTradeEvent: Ingredients=%s",
			// villagerMerchantRecipe.getIngredients().toString());
			if (slotClick != 2) {
				return;
			}
			// if (slotItem != null || slotItem.getType() != Material.AIR){
			// Merchant entity = villagerMerchantInventory.getMerchant();
			// TradeEvent villagerTradeEvent = new TradeEvent(
			// (Player) entity.getTrader(),
			// entity,
			// villagerMerchantInventory,
			// villagerMerchantRecipe,
			// slotItem,
			// slotClick,
			// villagerMerchantRecipe.getAdjustedIngredient1(),
			// villagerMerchantRecipe.getMaxUses(),
			// villagerMerchantRecipe.getVillagerExperience()
			// );
			// Bukkit.getServer().getPluginManager().callEvent(villagerTradeEvent);
			// if (villagerTradeEvent.isCancelled()){ event.setCancelled(true); }
			// }
		}
	}

}
