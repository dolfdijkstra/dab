package com.dolfdijkstra.dab;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import com.dolfdijkstra.dab.Statistics.Snapshot;

public final class WriterSummary implements PeriodicSummaryCollector {
    private final PrintWriter writer;
    private int i = 0;
    private final FastDateFormat df = FastDateFormat
            .getInstance("yyyy-MM-dd'T'HH:mm:ss");

    public WriterSummary(final FileWriter writer)
            throws MalformedObjectNameException, IOException {

        this.writer = new PrintWriter(writer, true);
        writer.write(String
                .format("count,timestamp,transactions_per_second,errors,response_time_avg,response_time_min,response_time_max,response_time_stddev,min_concurrency,max_concurrency,average_concurrency,system_cpu_load,process_cpu_load,system_load_average%n"));

    }

    @Override
    public void update(final long timestamp, final long elapsed,
            final Snapshot snapshot) {
        final StatisticalSummary responseTimeStat = snapshot.responseTime;
        final StatisticalSummary concurrencyStat = snapshot.concurrency;
        final long tps = responseTimeStat.getN() * 1000 / elapsed;
        final double mean = responseTimeStat.getMean();
        final double min = responseTimeStat.getMin();
        final double max = responseTimeStat.getMax();
        final double stddev = responseTimeStat.getStandardDeviation();

        final int maxConcurrency = (int) concurrencyStat.getMax();
        final int minConcurrency = (int) concurrencyStat.getMin();
        final double cMean = concurrencyStat.getMean();
        double load = snapshot.systemLoadAverage;
        double cpu = snapshot.systemCpuLoad;
        double proc = snapshot.processCpuLoad;

        writer.printf("%d,%s,%d,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%.2f,%.2f,%.2f,%.2f%n",
                i++, df.format(timestamp * 1000), tps, mean, min, max, stddev,
                minConcurrency, maxConcurrency, snapshot.errors, cMean, cpu, proc,
                load);

    }
}