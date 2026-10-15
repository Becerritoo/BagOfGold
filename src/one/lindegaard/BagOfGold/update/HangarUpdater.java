package one.lindegaard.BagOfGold.update;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.CustomItemsLib.update.HangarUpdateService;

public class HangarUpdater extends HangarUpdateService {

	private final BagOfGold plugin;

	public HangarUpdater(BagOfGold plugin) {
		super(plugin, "Becerritoo", "BagOfGold");
		this.plugin = plugin;
	}

	@Override
	protected long getCheckIntervalSeconds() {
		return plugin.getConfigManager().checkEvery;
	}

	@Override
	protected boolean isAutoUpdateEnabled() {
		return plugin.getConfigManager().autoupdate;
	}

	@Override
	protected String getPrefix() {
		return BagOfGold.PREFIX;
	}

	@Override
	protected String getCheckingMessage() {
		return plugin.getMessages().getString("bagofgold.commands.update.check");
	}

	@Override
	protected String getVersionFoundMessage(String version) {
		return plugin.getMessages().getString("bagofgold.commands.update.version-found", "newversion", version);
	}

	@Override
	protected String getNoUpdateMessage() {
		return plugin.getMessages().getString("bagofgold.commands.update.no-update");
	}

	@Override
	protected String getUpdateCompleteMessage() {
		return plugin.getMessages().getString("bagofgold.commands.update.complete");
	}

	@Override
	protected String getCouldNotUpdateMessage() {
		return plugin.getMessages().getString("bagofgold.commands.update.could-not-update");
	}

	@Override
	protected String getUpdateHelpMessage(String projectUrl) {
		return plugin.getMessages().getString("bagofgold.commands.update.help") + " " + projectUrl;
	}
}
