package com.dolfdijkstra.dab.reporting;

import java.io.Console;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import com.dolfdijkstra.dab.DabApp;
import com.dolfdijkstra.dab.WorkerManager;
import com.dolfdijkstra.dab.reporting.Statistics.Snapshot;

public final class ConsoleSummary implements PeriodicSummaryCollector {

    private final Console console = System.console();
    private final WorkerManager manager;

    public ConsoleSummary(final WorkerManager manager) {
        this.manager = manager;
    }

    @Override
    public void update(final long timestamp, final long elapsed,
            final Snapshot snapshot) {
        if (console == null) {
            return;
        }
        final StatisticalSummary responseTimeStat = snapshot.responseTime;
        final StatisticalSummary concurrencyStat = snapshot.concurrency;
        final long tps = responseTimeStat.getN() * 1000 / elapsed;

        final double mean = responseTimeStat.getMean();
        final int maxConcurrency = (int) concurrencyStat.getMax();
        final int minConcurrency = (int) concurrencyStat.getMin();
        final double cMean = concurrencyStat.getMean();

        final long tps_per_core = tps / DabApp.availableCores;

        console.format("\033[5A\r\033[K%-15s: %,9d TPS Errors: %,d%n", "Throughput",
                tps, snapshot.errors);
        console.format("\r\033[K%-15s: %,9d TPS/core on %d cores%n", "          ",
                tps_per_core, DabApp.availableCores);
        console.format("\r\033[K%-15s: %s min: %s max: %s%n", "Response time",
                format("%,9.0f %s", mean),
                format("%,4.0f %s", responseTimeStat.getMin()),
                format("%,4.0f %s", responseTimeStat.getMax()));

        console.format(
                "\r\033[KConcurrency    : %,d of %,d [ %,d / %,.1f / %,d] [min/avg/max]%n",
                manager.getConcurrentRequestsNumber(), manager.getWorkerCount(),
                minConcurrency, cMean, maxConcurrency);
        console.format(
                "\r\033[K1m-load-avg    : %4.2f system: %4.2f %% process: %4.2f %%%n",
                snapshot.systemLoadAverage, snapshot.systemCpuLoad,
                snapshot.processCpuLoad);

    }

    private String format(final String format, final double mean) {
        double m = mean;
        String ps = "μs";
        if (m > 10000000) {
            m = mean / 1000000;
            ps = " beanName";
        } else if (mean > 10000) {
            m = mean / 1000;
            ps = "ms";
        }
        return String.format(format, m, ps);
    }

}