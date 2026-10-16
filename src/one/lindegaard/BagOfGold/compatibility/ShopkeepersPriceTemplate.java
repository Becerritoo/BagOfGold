package one.lindegaard.BagOfGold.compatibility;

import java.util.UUID;

final class ShopkeepersPriceTemplate {

	private final UUID shopkeeperId;
	private final double unitAmount;
	private final int quantity;
	private final double totalAmount;

	private ShopkeepersPriceTemplate(UUID shopkeeperId, double unitAmount, int quantity, double totalAmount) {
		this.shopkeeperId = shopkeeperId;
		this.unitAmount = unitAmount;
		this.quantity = quantity;
		this.totalAmount = totalAmount;
	}

	static ShopkeepersPriceTemplate create(UUID shopkeeperId, double unitAmount, int quantity) {
		if (shopkeeperId == null || quantity <= 0)
			return null;
		double totalAmount = ShopkeepersPriceParser.calculateTotal(unitAmount, quantity);
		if (totalAmount <= 0)
			return null;
		return new ShopkeepersPriceTemplate(shopkeeperId, unitAmount, quantity, totalAmount);
	}

	static ShopkeepersPriceTemplate parse(String payload) {
		if (payload == null)
			return null;
		String[] parts = payload.split("\\|", -1);
		if (parts.length != 2 && parts.length != 3)
			return null;
		try {
			UUID shopkeeperId = UUID.fromString(parts[0]);
			double unitAmount = Double.parseDouble(parts[1]);
			int quantity = parts.length == 3 ? Integer.parseInt(parts[2]) : 1;
			return create(shopkeeperId, unitAmount, quantity);
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	String serialize() {
		return shopkeeperId + "|" + Double.toString(unitAmount) + "|" + quantity;
	}

	UUID getShopkeeperId() {
		return shopkeeperId;
	}

	double getUnitAmount() {
		return unitAmount;
	}

	int getQuantity() {
		return quantity;
	}

	double getTotalAmount() {
		return totalAmount;
	}
}
