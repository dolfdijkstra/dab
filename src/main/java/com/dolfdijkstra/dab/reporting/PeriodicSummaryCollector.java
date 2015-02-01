package com.dolfdijkstra.dab.reporting;

import com.dolfdijkstra.dab.reporting.Statistics.Snapshot;

public interface PeriodicSummaryCollector {

    /**
     * @param timestamp
     *            in seconds since epoch
     * @param elapsed
     *            time in ms covering this reporting period
     * @param snapshot
     *            statistics over the last elapsed period
     */
    void update(long timestamp, long elapsed, Snapshot snapshot);

}
