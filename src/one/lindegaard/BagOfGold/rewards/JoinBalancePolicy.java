package one.lindegaard.BagOfGold.rewards;

final class JoinBalancePolicy {

	enum Action {
		MATCH,
		MATERIALIZE_STORED_CREDIT,
		PRESERVE_PHYSICAL_BALANCE
	}

	private JoinBalancePolicy() {
	}

	static Action decide(double storedBalance, double physicalBalance) {
		int comparison = Double.compare(storedBalance, physicalBalance);
		if (comparison > 0)
			return Action.MATERIALIZE_STORED_CREDIT;
		if (comparison < 0)
			return Action.PRESERVE_PHYSICAL_BALANCE;
		return Action.MATCH;
	}
}
