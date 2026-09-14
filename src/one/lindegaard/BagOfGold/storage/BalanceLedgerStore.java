package one.lindegaard.BagOfGold.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.OfflinePlayer;

import com.mysql.cj.jdbc.MysqlDataSource;

import one.lindegaard.BagOfGold.BagOfGold;
import one.lindegaard.CustomItemsLib.Tools;

public class BalanceLedgerStore {

	private static final String TABLE_NAME = "mh_balance_ledger";

	private final BagOfGold plugin;
	private final ExecutorService executor;
	private boolean enabled = false;

	public BalanceLedgerStore(BagOfGold plugin) {
		this.plugin = plugin;
		this.executor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "BagOfGold-BalanceLedger");
			thread.setDaemon(true);
			return thread;
		});

		if (!plugin.getConfigManager().databaseType.equalsIgnoreCase("mysql")) {
			plugin.getMessages().debug("BalanceLedger disabled: only MySQL storage is supported.");
			return;
		}

		try {
			setupSchema();
			enabled = true;
			plugin.getMessages().debug("BalanceLedger enabled using table %s", TABLE_NAME);
		} catch (SQLException e) {
			plugin.getLogger().warning("Could not initialize balance ledger table: " + e.getMessage());
		}
	}

	public void record(OfflinePlayer player, double balanceBefore, double balanceAfter, String sourceHint) {
		if (!enabled || player == null)
			return;

		double before = Tools.round(balanceBefore);
		double after = Tools.round(balanceAfter);
		double delta = Tools.round(after - before);
		if (delta == 0)
			return;

		String playerUuid = player.getUniqueId().toString();
		String playerName = player.getName();
		String sourcePlugin = detectSourcePlugin(Thread.currentThread().getStackTrace());
		String hint = sourceHint == null ? null : sourceHint;
		long createdAt = System.currentTimeMillis();

		try {
			executor.execute(() -> insert(playerUuid, playerName, delta, before, after, sourcePlugin, hint, createdAt));
		} catch (RejectedExecutionException ignored) {
		}
	}

	public void shutdown() {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(2, TimeUnit.SECONDS))
				executor.shutdownNow();
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}

	private void setupSchema() throws SQLException {
		try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
					+ "id BIGINT NOT NULL AUTO_INCREMENT,"
					+ "player_uuid VARCHAR(36) NOT NULL,"
					+ "player_name VARCHAR(20) NULL,"
					+ "delta DOUBLE NOT NULL,"
					+ "balance_before DOUBLE NOT NULL,"
					+ "balance_after DOUBLE NOT NULL,"
					+ "source_plugin VARCHAR(64) NOT NULL,"
					+ "source_hint VARCHAR(128) NULL,"
					+ "created_at BIGINT NOT NULL,"
					+ "PRIMARY KEY (id),"
					+ "INDEX idx_player_time (player_uuid, created_at),"
					+ "INDEX idx_source_time (source_plugin, created_at)"
					+ ");");
		}
	}

	private void insert(String playerUuid, String playerName, double delta, double before, double after,
			String sourcePlugin, String sourceHint, long createdAt) {
		try (Connection connection = openConnection();
				PreparedStatement statement = connection.prepareStatement("INSERT INTO " + TABLE_NAME
						+ " (player_uuid, player_name, delta, balance_before, balance_after, source_plugin, source_hint, created_at)"
						+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?);")) {
			statement.setString(1, playerUuid);
			statement.setString(2, playerName);
			statement.setDouble(3, delta);
			statement.setDouble(4, before);
			statement.setDouble(5, after);
			statement.setString(6, sourcePlugin);
			statement.setString(7, sourceHint);
			statement.setLong(8, createdAt);
			statement.executeUpdate();
		} catch (SQLException e) {
			plugin.getLogger().warning("Could not insert balance ledger record: " + e.getMessage());
		}
	}

	private Connection openConnection() throws SQLException {
		Locale.setDefault(new Locale("us", "US"));
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser(plugin.getConfigManager().databaseUsername);
		dataSource.setPassword(plugin.getConfigManager().databasePassword);
		if (plugin.getConfigManager().databaseHost.contains(":")) {
			String[] host = plugin.getConfigManager().databaseHost.split(":", 2);
			dataSource.setServerName(host[0]);
			dataSource.setPort(Integer.valueOf(host[1]));
		} else {
			dataSource.setServerName(plugin.getConfigManager().databaseHost);
		}
		dataSource.setDatabaseName(plugin.getConfigManager().databaseName);
		Connection connection = dataSource.getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("SET NAMES 'utf8'");
			statement.executeUpdate("SET CHARACTER SET 'utf8'");
		}
		connection.setAutoCommit(true);
		return connection;
	}

	private String detectSourcePlugin(StackTraceElement[] stackTrace) {
		for (StackTraceElement element : stackTrace) {
			String className = element.getClassName().toLowerCase(Locale.ROOT);
			if (className.contains("economyshopgui"))
				return "EconomyShopGUI";
			if (className.contains("gamingmesh.jobs") || className.contains(".jobs."))
				return "Jobs";
			if (className.contains("blepfishing"))
				return "BlepFishing";
			if (className.contains("towny"))
				return "Towny";
			if (className.contains("essentials"))
				return "Essentials";
			if (className.contains("shopkeepers"))
				return "Shopkeepers";
		}
		for (StackTraceElement element : stackTrace) {
			if (element.getClassName().toLowerCase(Locale.ROOT).contains("bagofgold"))
				return "BagOfGold";
		}
		return "Unknown";
	}
}
