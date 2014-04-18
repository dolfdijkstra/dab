package com.dolfdijkstra.dab;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

final class PerSecondRunnable implements Runnable {
    private static final String CONSOLE_FORMAT = "\r\033[KThroughput: %,8d, RT: %,6.0f %s, concurrency: %,7d, this machine load-avg: %,8.2f";
    long prev = System.currentTimeMillis();
    private final SummaryStatistics ps;
    private final SummaryStatistics concurrencyStatPerSecond;
    private final TpsCollector rrdChart;
    private final OperatingSystemMXBean operatingSystemMXBean;

    /**
     * @param ps
     * @param concurrencyStatPerSecond
     */
    public PerSecondRunnable(SummaryStatistics ps, SummaryStatistics concurrencyStatPerSecond, TpsCollector rrdChart) {
        super();
        this.ps = ps;
        this.concurrencyStatPerSecond = concurrencyStatPerSecond;
        this.rrdChart = rrdChart;
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
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

        try {
            double load = operatingSystemMXBean.getSystemLoadAverage();
            if (load < 0) {
                load = Double.NaN;
            }
            double m = mean;
            String ps = "μs";
            if (m > 10000000) {
                m = mean / 1000000;
                ps = " s";
            } else if (mean > 10000) {
                m = mean / 1000;
                ps = "ms";
            }
            System.out.printf(CONSOLE_FORMAT, tps, m, ps, (int) c, load);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
