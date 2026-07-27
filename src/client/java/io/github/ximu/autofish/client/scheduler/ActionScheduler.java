package io.github.ximu.autofish.client.scheduler;

import java.util.ArrayList;
import java.util.List;

public final class ActionScheduler {
    private final List<ScheduledAction> queuedActions = new ArrayList<>();

    public void schedule(ActionType type, long delayMs, Runnable action) {
        queuedActions.add(new ScheduledAction(type, nowMillis() + Math.max(0, delayMs), action));
    }

    public void tick() {
        long now = nowMillis();
        queuedActions.removeIf(action -> action.runIfReady(now));
    }

    public boolean hasQueued(ActionType type) {
        return queuedActions.stream().anyMatch(action -> action.type() == type);
    }

    public void clear() {
        queuedActions.clear();
    }

    private static long nowMillis() {
        return System.nanoTime() / 1_000_000L;
    }
}
