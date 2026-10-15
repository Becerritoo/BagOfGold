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
}
