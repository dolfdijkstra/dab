package com.dolfdijkstra.dab;

import com.dolfdijkstra.dab.reporting.PeriodicSummaryCollector;
import com.dolfdijkstra.dab.reporting.Statistics;
import com.dolfdijkstra.dab.reporting.Statistics.Snapshot;

final class PerSecondRunnable implements Runnable {

    private long prev = System.currentTimeMillis();

    private final Statistics stats;
    private final PeriodicSummaryCollector[] collectors;

    public PerSecondRunnable(final Statistics stats,
            final PeriodicSummaryCollector... collector) {

        this.stats = stats;
        collectors = collector;

    }

    @Override
    public void run() {

        final long now = System.currentTimeMillis();
        final long elapsed = now - prev;
        prev = now;
        final Snapshot snapshot = stats.collect();
        for (final PeriodicSummaryCollector col : collectors) {
            try {
                col.update(now / 1000L, elapsed, snapshot);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

    }
}
