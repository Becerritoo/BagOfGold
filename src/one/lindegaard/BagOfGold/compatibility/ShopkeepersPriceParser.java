package one.lindegaard.BagOfGold.compatibility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ShopkeepersPriceParser {

	private static final Pattern PLACEHOLDER = Pattern
			.compile("^bagofgold\\s+([0-9]+(?:\\.[0-9]{1,5})?)$", Pattern.CASE_INSENSITIVE);
	private static final double MAX_PRICE = 1_000_000_000_000D;

	private ShopkeepersPriceParser() {
	}

	static Double parse(String displayName) {
		if (displayName == null)
			return null;
		Matcher matcher = PLACEHOLDER.matcher(displayName.trim());
		if (!matcher.matches())
			return null;
		try {
			double amount = Double.parseDouble(matcher.group(1));
			return Double.isFinite(amount) && amount > 0 && amount <= MAX_PRICE ? amount : null;
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	static boolean isValidAmount(double amount) {
		return Double.isFinite(amount) && amount > 0 && amount <= MAX_PRICE;
	}
}
