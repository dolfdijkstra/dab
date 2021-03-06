package com.dolfdijkstra.dab.reporting;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.management.MalformedObjectNameException;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import com.dolfdijkstra.dab.reporting.Statistics.Snapshot;

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
        final double load = snapshot.systemLoadAverage;
        final double cpu = snapshot.systemCpuLoad;
        final double proc = snapshot.processCpuLoad;

        writer.printf("%d,%s,%d,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%.2f,%.2f,%.2f,%.2f%n",
                i++, df.format(timestamp * 1000), tps, mean, min, max, stddev,
                minConcurrency, maxConcurrency, snapshot.errors, cMean, cpu, proc,
                load);

    }
}