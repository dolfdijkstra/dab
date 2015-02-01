package com.dolfdijkstra.dab;

import com.dolfdijkstra.dab.reporting.PeriodicSummaryCollector;
import com.dolfdijkstra.dab.reporting.Statistics;
import com.dolfdijkstra.dab.reporting.Statistics.Snapshot;

final class PerSecondRunnable implements Runnable {

    private long prev = System.currentTimeMillis();
    
    private final Statistics stats;
    private final PeriodicSummaryCollector[] collectors;
    

    public PerSecondRunnable(Statistics stats, PeriodicSummaryCollector... collector) {

        this.stats = stats;
        this.collectors = collector;

    }

    @Override
    public void run() {

        long now = System.currentTimeMillis();
        long elapsed = (now - prev);
        prev = now;
        Snapshot snapshot = stats.collect();
        for (PeriodicSummaryCollector col : collectors) {
            try {
                col.update(now / 1000L, elapsed, snapshot);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
