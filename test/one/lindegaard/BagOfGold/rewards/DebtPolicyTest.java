package one.lindegaard.BagOfGold.rewards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DebtPolicyTest {

	private static final double DELTA = 0.000001;

	@Test
	public void depositPaysDebtBeforeCreditingCash() {
		DebtPolicy.Deposit partial = DebtPolicy.settleDeposit(100, 40);
		assertEquals(60, partial.debtAfter, DELTA);
		assertEquals(40, partial.debtPayment, DELTA);
		assertEquals(0, partial.cashCredit, DELTA);

		DebtPolicy.Deposit excess = DebtPolicy.settleDeposit(100, 140);
		assertEquals(0, excess.debtAfter, DELTA);
		assertEquals(100, excess.debtPayment, DELTA);
		assertEquals(40, excess.cashCredit, DELTA);
	}

	@Test
	public void withdrawalConsumesCashBeforeCreatingDebt() {
		DebtPolicy.Withdrawal result = DebtPolicy.withdraw(75, 0, 100, true, 500);
		assertTrue(result.allowed);
		assertEquals(0, result.cashAfter, DELTA);
		assertEquals(25, result.debtAfter, DELTA);
		assertEquals(25, result.debtCreated, DELTA);
	}

	@Test
	public void withdrawalRespectsCumulativeDebtLimit() {
		DebtPolicy.Withdrawal allowed = DebtPolicy.withdraw(10, 80, 30, true, 100);
		assertTrue(allowed.allowed);
		assertEquals(0, allowed.cashAfter, DELTA);
		assertEquals(100, allowed.debtAfter, DELTA);

		DebtPolicy.Withdrawal rejected = DebtPolicy.withdraw(10, 80, 31, true, 100);
		assertFalse(rejected.allowed);
		assertEquals(10, rejected.cashAfter, DELTA);
		assertEquals(80, rejected.debtAfter, DELTA);
	}

	@Test
	public void disabledDebtRejectsInsufficientFunds() {
		DebtPolicy.Withdrawal rejected = DebtPolicy.withdraw(20, 0, 21, false, 1000);
		assertFalse(rejected.allowed);

		DebtPolicy.Withdrawal allowed = DebtPolicy.withdraw(20, 0, 20, false, 1000);
		assertTrue(allowed.allowed);
		assertEquals(0, allowed.cashAfter, DELTA);
		assertEquals(0, allowed.debtAfter, DELTA);
	}
}
