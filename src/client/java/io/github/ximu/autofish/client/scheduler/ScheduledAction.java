package io.github.ximu.autofish.client.scheduler;

record ScheduledAction(ActionType type, long runAt, Runnable action) {
    boolean runIfReady(long now) {
        if (now < runAt) {
            return false;
        }
        action.run();
        return true;
    }
}
