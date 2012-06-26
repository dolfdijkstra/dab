package com.dolfdijkstra.dab;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

final class PerSecondRunnable implements Runnable {
    long prev = System.currentTimeMillis();
    private final SummaryStatistics ps;
    private final SummaryStatistics concurrencyStatPerSecond;
    private final TpsCollector rrdChart;
    OperatingSystemMXBean o;

    /**
     * @param ps
     * @param concurrencyStatPerSecond
     */
    public PerSecondRunnable(SummaryStatistics ps, SummaryStatistics concurrencyStatPerSecond, TpsCollector rrdChart) {
        super();
        this.ps = ps;
        this.concurrencyStatPerSecond = concurrencyStatPerSecond;
        this.rrdChart = rrdChart;
        o = ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public void run() {

        long now = System.currentTimeMillis();
        long elapsed = (now - prev);
        prev = now;
        long tps = ps.getN() * 1000 / elapsed;
        double mean = ps.getMean();
        double c = concurrencyStatPerSecond.getMax();
        double cMean = concurrencyStatPerSecond.getMean();
        ps.clear();
        concurrencyStatPerSecond.clear();

        try {
            rrdChart.update(now / 1000L, tps, mean, c, cMean);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (o.getSystemLoadAverage() != -1) {
            System.out.println("Throughput: " + (tps) + " tps, this machine load-avg: " + o.getSystemLoadAverage());
        } else {
            System.out.println("Throughput: " + (tps));
        }
    }
}
