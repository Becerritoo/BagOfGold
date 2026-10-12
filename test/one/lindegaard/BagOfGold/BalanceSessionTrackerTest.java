package one.lindegaard.BagOfGold;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.junit.Test;

public class BalanceSessionTrackerTest {

	@Test
	public void newerSessionRejectsOlderAsyncResult() {
		BalanceSessionTracker tracker = new BalanceSessionTracker();
		UUID uuid = UUID.randomUUID();
		long first = tracker.begin(uuid);
		long second = tracker.begin(uuid);

		assertFalse(tracker.isCurrent(uuid, first));
		assertTrue(tracker.isCurrent(uuid, second));
	}

	@Test
	public void disconnectInvalidatesPendingResult() {
		BalanceSessionTracker tracker = new BalanceSessionTracker();
		UUID uuid = UUID.randomUUID();
		long session = tracker.begin(uuid);

		tracker.invalidate(uuid);

		assertFalse(tracker.isCurrent(uuid, session));
	}
}
