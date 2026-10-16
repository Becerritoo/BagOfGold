package one.lindegaard.BagOfGold.compatibility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.Test;

public class ShopkeepersPriceTemplateTest {

	private static final UUID SHOPKEEPER_ID = UUID.fromString("12345678-1234-5678-9abc-123456789abc");

	@Test
	public void signsQuantityAndCalculatesTotal() {
		ShopkeepersPriceTemplate template = ShopkeepersPriceTemplate.create(SHOPKEEPER_ID, 200D, 3);

		assertEquals(SHOPKEEPER_ID, template.getShopkeeperId());
		assertEquals(200D, template.getUnitAmount(), 0D);
		assertEquals(3, template.getQuantity());
		assertEquals(600D, template.getTotalAmount(), 0D);
		assertEquals(SHOPKEEPER_ID + "|200.0|3", template.serialize());
	}

	@Test
	public void readsLegacyTemplatesAsQuantityOne() {
		ShopkeepersPriceTemplate template = ShopkeepersPriceTemplate.parse(SHOPKEEPER_ID + "|200.0");

		assertEquals(1, template.getQuantity());
		assertEquals(200D, template.getTotalAmount(), 0D);
	}

	@Test
	public void rejectsInvalidOrOverflowingTemplates() {
		assertNull(ShopkeepersPriceTemplate.parse(SHOPKEEPER_ID + "|200.0|0"));
		assertNull(ShopkeepersPriceTemplate.parse(SHOPKEEPER_ID + "|1000000000000.0|2"));
		assertNull(ShopkeepersPriceTemplate.parse("invalid|200.0|2"));
	}
}
