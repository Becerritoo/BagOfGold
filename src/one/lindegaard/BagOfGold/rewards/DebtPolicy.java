package one.lindegaard.BagOfGold.rewards;

public final class DebtPolicy {

	private DebtPolicy() {
	}

	public static Deposit settleDeposit(double debt, double amount) {
		double safeDebt = Math.max(0, debt);
		double debtPayment = Math.min(safeDebt, Math.max(0, amount));
		return new Deposit(safeDebt - debtPayment, debtPayment, Math.max(0, amount) - debtPayment);
	}

	public static Withdrawal withdraw(double cash, double debt, double amount, boolean debtEnabled,
			double maximumDebt) {
		double safeCash = Math.max(0, cash);
		double safeDebt = Math.max(0, debt);
		double safeAmount = Math.max(0, amount);
		double limit = debtEnabled ? Math.max(0, maximumDebt) : 0;
		if (safeDebt + Math.max(0, safeAmount - safeCash) > limit)
			return new Withdrawal(false, safeCash, safeDebt, 0);

		double cashSpent = Math.min(safeCash, safeAmount);
		double debtCreated = safeAmount - cashSpent;
		return new Withdrawal(true, safeCash - cashSpent, safeDebt + debtCreated, debtCreated);
	}

	public static final class Deposit {
		public final double debtAfter;
		public final double debtPayment;
		public final double cashCredit;

		private Deposit(double debtAfter, double debtPayment, double cashCredit) {
			this.debtAfter = debtAfter;
			this.debtPayment = debtPayment;
			this.cashCredit = cashCredit;
		}
	}

	public static final class Withdrawal {
		public final boolean allowed;
		public final double cashAfter;
		public final double debtAfter;
		public final double debtCreated;

		private Withdrawal(boolean allowed, double cashAfter, double debtAfter, double debtCreated) {
			this.allowed = allowed;
			this.cashAfter = cashAfter;
			this.debtAfter = debtAfter;
			this.debtCreated = debtCreated;
		}
	}
}
