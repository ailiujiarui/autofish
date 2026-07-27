package io.github.ximu.autofish.client.scheduler;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionSchedulerTest {
    @Test
    void runsReadyActionOnceAndRemovesIt() {
        ActionScheduler scheduler = new ActionScheduler();
        AtomicInteger calls = new AtomicInteger();
        scheduler.schedule(ActionType.RECAST, 0, calls::incrementAndGet);

        assertTrue(scheduler.hasQueued(ActionType.RECAST));
        scheduler.tick();
        scheduler.tick();

        assertEquals(1, calls.get());
        assertFalse(scheduler.hasQueued(ActionType.RECAST));
    }

    @Test
    void clearCancelsQueuedActions() {
        ActionScheduler scheduler = new ActionScheduler();
        AtomicInteger calls = new AtomicInteger();
        scheduler.schedule(ActionType.ROD_SWITCH, 0, calls::incrementAndGet);

        scheduler.clear();
        scheduler.tick();

        assertEquals(0, calls.get());
    }
}
