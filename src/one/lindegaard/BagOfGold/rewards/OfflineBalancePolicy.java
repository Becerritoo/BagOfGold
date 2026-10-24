package one.lindegaard.BagOfGold.rewards;

/**
 * Keeps the last physical inventory snapshot stable while recording economy
 * changes made against an account that is not currently represented by a player
 * inventory.
 */
final class OfflineBalancePolicy {

	private OfflineBalancePolicy() {
	}

	static Deposit deposit(double balance, double balanceChanges, double debt, double amount) {
		double currentCash = Math.max(0, balance + balanceChanges);
		DebtPolicy.Deposit settlement = DebtPolicy.settleDeposit(debt, amount);
		double cashAfter = currentCash + settlement.cashCredit;
		return new Deposit(balance, cashAfter - balance, settlement.debtAfter, settlement.debtPayment);
	}

	static Withdrawal withdraw(double balance, double balanceChanges, double debt, double amount,
			boolean debtEnabled, double maximumDebt) {
		double currentCash = Math.max(0, balance + balanceChanges);
		DebtPolicy.Withdrawal settlement = DebtPolicy.withdraw(currentCash, debt, amount, debtEnabled, maximumDebt);
		return new Withdrawal(settlement.allowed, balance, settlement.cashAfter - balance, settlement.debtAfter,
				settlement.debtCreated);
	}

	static final class Deposit {
		final double balance;
		final double balanceChanges;
		final double debtAfter;
		final double debtPayment;

		private Deposit(double balance, double balanceChanges, double debtAfter, double debtPayment) {
			this.balance = balance;
			this.balanceChanges = balanceChanges;
			this.debtAfter = debtAfter;
			this.debtPayment = debtPayment;
		}
	}

	static final class Withdrawal {
		final boolean allowed;
		final double balance;
		final double balanceChanges;
		final double debtAfter;
		final double debtCreated;

		private Withdrawal(boolean allowed, double balance, double balanceChanges, double debtAfter,
				double debtCreated) {
			this.allowed = allowed;
			this.balance = balance;
			this.balanceChanges = balanceChanges;
			this.debtAfter = debtAfter;
			this.debtCreated = debtCreated;
		}
	}
}
