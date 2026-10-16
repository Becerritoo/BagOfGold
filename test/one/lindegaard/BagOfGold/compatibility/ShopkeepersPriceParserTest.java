package one.lindegaard.BagOfGold.compatibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ShopkeepersPriceParserTest {

	@Test
	public void parsesExpectedPlaceholder() {
		assertEquals(3000D, ShopkeepersPriceParser.parse("bagofgold 3000"), 0D);
		assertEquals(12.34567D, ShopkeepersPriceParser.parse("BagOfGold 12.34567"), 0D);
	}

	@Test
	public void rejectsUnsafeOrAmbiguousValues() {
		assertNull(ShopkeepersPriceParser.parse("bagofgold -1"));
		assertNull(ShopkeepersPriceParser.parse("bagofgold 0"));
		assertNull(ShopkeepersPriceParser.parse("bagofgold NaN"));
		assertNull(ShopkeepersPriceParser.parse("bagofgold 1,000"));
		assertNull(ShopkeepersPriceParser.parse("bagofgold 1.000001"));
		assertNull(ShopkeepersPriceParser.parse("bagofgold 3000 extra"));
	}

	@Test
	public void calculatesSafeMultipliedPrice() {
		assertEquals(400D, ShopkeepersPriceParser.calculateTotal(200D, 2), 0D);
		assertEquals(600D, ShopkeepersPriceParser.calculateTotal(200D, 3), 0D);
		assertEquals(0D, ShopkeepersPriceParser.calculateTotal(200D, 0), 0D);
		assertEquals(0D, ShopkeepersPriceParser.calculateTotal(1_000_000_000_000D, 2), 0D);
	}
}
