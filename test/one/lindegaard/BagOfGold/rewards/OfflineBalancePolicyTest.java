package one.lindegaard.BagOfGold.rewards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OfflineBalancePolicyTest {

	private static final double DELTA = 0.000001;

	@Test
	public void offlineWithdrawalKeepsPhysicalSnapshotAndRecordsPendingDebit() {
		OfflineBalancePolicy.Withdrawal result = OfflineBalancePolicy.withdraw(500, 0, 0, 200, false, 0);

		assertTrue(result.allowed);
		assertEquals(500, result.balance, DELTA);
		assertEquals(-200, result.balanceChanges, DELTA);
		assertEquals(0, result.debtAfter, DELTA);
	}

	@Test
	public void consecutiveOfflineChangesAccumulateAgainstSameSnapshot() {
		OfflineBalancePolicy.Withdrawal first = OfflineBalancePolicy.withdraw(500, 0, 0, 200, false, 0);
		OfflineBalancePolicy.Withdrawal second = OfflineBalancePolicy.withdraw(first.balance, first.balanceChanges,
				first.debtAfter, 50, false, 0);
		OfflineBalancePolicy.Deposit third = OfflineBalancePolicy.deposit(second.balance, second.balanceChanges,
				second.debtAfter, 25);

		assertEquals(500, third.balance, DELTA);
		assertEquals(-225, third.balanceChanges, DELTA);
	}

	@Test
	public void offlineDepositPaysDebtBeforeCreatingPendingCash() {
		OfflineBalancePolicy.Deposit result = OfflineBalancePolicy.deposit(0, 0, 100, 40);

		assertEquals(0, result.balance, DELTA);
		assertEquals(0, result.balanceChanges, DELTA);
		assertEquals(60, result.debtAfter, DELTA);
		assertEquals(40, result.debtPayment, DELTA);
	}
}
