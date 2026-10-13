package one.lindegaard.BagOfGold.rewards;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JoinBalancePolicyTest {

	@Test
	public void preservesPhysicalMoneyWhenDatabaseIsBehind() {
		assertEquals(JoinBalancePolicy.Action.PRESERVE_PHYSICAL_BALANCE,
				JoinBalancePolicy.decide(0, 7334));
	}

	@Test
	public void materializesLegitimateOfflineCredit() {
		assertEquals(JoinBalancePolicy.Action.MATERIALIZE_STORED_CREDIT,
				JoinBalancePolicy.decide(7334, 0));
	}

	@Test
	public void leavesMatchingBalanceUntouched() {
		assertEquals(JoinBalancePolicy.Action.MATCH, JoinBalancePolicy.decide(7334, 7334));
	}
}
