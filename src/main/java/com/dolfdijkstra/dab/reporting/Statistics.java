package com.dolfdijkstra.dab.reporting;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.math3.stat.Frequency;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import com.dolfdijkstra.dab.RequestResult;
import com.dolfdijkstra.dab.ResultsCollector;

public class Statistics implements ResultsCollector {
    private final SummaryStatistics elapsed = new SummaryStatistics();
    private final SummaryStatistics sizeStat = new SummaryStatistics();

    private final SummaryStatistics receivedLengthStat = new SummaryStatistics();
    private final SummaryStatistics sendLengthStat = new SummaryStatistics();

    private final SummaryStatistics concurrency = new SummaryStatistics();

    private final Frequency statusFrequency = new Frequency();
    private final Frequency concurrencyFrequency = new Frequency();
    private final Frequency idFrequency = new Frequency();
    private final TimeSeriesStat timeSeriesStat;
    private volatile SummaryStatistics elapsedPerSecond = new SummaryStatistics();
    private volatile SummaryStatistics concurrencyPerSecond = new SummaryStatistics();
    private long prevErrors = 0;
    private final AtomicLong errorCounter = new AtomicLong();
    private final Frequency errors = new Frequency();

    private final MBeanServer mbs;
    private final ObjectName beanName;

    /**
     * @param periods
     *            the expected number of periods that need to be collected
     */
    public Statistics(int periods) {
        this.timeSeriesStat = new TimeSeriesStat(periods);
        mbs = ManagementFactory.getPlatformMBeanServer();
        beanName = ManagementFactory.getOperatingSystemMXBean().getObjectName();

    }

    @Override
    public synchronized void collect(RequestResult results) {
        if (results.exeption != null) {
            errorCounter.incrementAndGet();
            errors.addValue(results.exeption.getClass().getName());
            return;
        }
        this.elapsedPerSecond.addValue(results.elapsed);
        this.concurrencyPerSecond.addValue(results.concurrency);

        this.elapsed.addValue(results.elapsed);
        this.concurrency.addValue(results.concurrency);
        Long period = results.relativeStart / 1000L;
        this.timeSeriesStat.add(period.intValue(), results.elapsed);

        this.statusFrequency.addValue(results.status);
        if (results.length > -1)
            this.sizeStat.addValue(results.length);
        if (results.sendLength > 1)
            this.sendLengthStat.addValue(results.sendLength);
        if (results.receivedLength > 1)
            this.receivedLengthStat.addValue(results.receivedLength);

        this.idFrequency.addValue(results.id);
        this.concurrencyFrequency.addValue(results.concurrency);

    }

    public synchronized long getTransactions() {
        return elapsed.getN();
    }

    public synchronized String createSummary(long start, long end, int verbose) {
        long duration = (end - start) / 1000L;
        Duration d = Duration.ofSeconds(duration);
        StringAppender b = new StringAppender();
        FastDateFormat df = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");
        b.line("Test ran between %s and %s.", df.format(start), df.format(end));
        b.line();

        b.line("Test duration:       %,9d:%02d:%02d (%,d seconds)", d.toHours(),
                d.toMinutes() % 60, duration - (d.toMinutes() * 60), duration);

        b.line("Number of requests:  %,15d", elapsed.getN());
        b.line("Errors:              %,15d", errorCounter.get());
        b.line("Average throughput:  %,15d tps.", elapsed.getN() / duration);
        b.line("Average concurrency: %,15.1f", concurrency.getMean());
        b.line("Min concurrency:     %,15.0f", concurrency.getMin());
        b.line("Peak concurrency:    %,15.0f", concurrency.getMax());

        b.line("Mean:                %,15.2f μs.", elapsed.getMean());
        b.line("Min:                 %,15.0f μs.", elapsed.getMin());
        b.line("Max:                 %,15.0f μs.", elapsed.getMax());
        b.line("StandardDeviation:   %,15.2f μs.", elapsed.getStandardDeviation());
        b.line();
        b.line("Average size:        %,15.0f bytes.", sizeStat.getMean());
        double network = sizeStat.getMean() * sizeStat.getN()
                / (duration * 1024D * 1024D);
        double networkSend = sendLengthStat.getMean() * sendLengthStat.getN()
                / (duration * 1024D * 1024D);
        double networkReceive = receivedLengthStat.getMean() * receivedLengthStat.getN()
                / (duration * 1024D * 1024D);
        
        
        //b.line("Network Throughput:  %,15.2f Mb/s.", network);
        b.line("Network Send:        %,15.2f Mb/s.", networkSend);
        b.line("Network Receive:     %,15.2f Mb/s.", networkReceive);

        b.line();
        b.line("HTTP status");

        for (Iterator<Comparable<?>> i = statusFrequency.valuesIterator(); i
                .hasNext();) {
            Comparable<?> v = i.next();
            b.line("%3s: %,d", v, statusFrequency.getCount(v));

        }

        if (verbose > 2) {
            b.line();
            b.line("Concurrency");

            for (Iterator<Comparable<?>> i = concurrencyFrequency.valuesIterator(); i
                    .hasNext();) {
                Comparable<?> v = i.next();
                b.line("%4s: %,15d", v, concurrencyFrequency.getCount(v));

            }
            b.line();
            b.line("Worker distribution");
            for (Iterator<Comparable<?>> i = idFrequency.valuesIterator(); i
                    .hasNext();) {
                Comparable<?> v = i.next();
                b.line("%4s: %,15d", v, idFrequency.getCount(v));

            }

        }
        if (verbose > 3) {
            b.line();
            b.line("Period distribution");
            for (int i = 0; i < timeSeriesStat.size(); i++) {
                SummaryStatistics s = timeSeriesStat.get(i);
                b.line("%6d: %,15d: %,15.2f", i, s == null ? -1 : s.getN(),
                        s == null ? -1 : s.getMean());

            }
        }
        if (verbose >= 0 && errorCounter.get() > 0) {
            b.line();
            b.line("Errors");
            int max = 0;
            for (Iterator<Comparable<?>> i = errors.valuesIterator(); i.hasNext();) {
                Comparable<?> v = i.next();
                max = Math.max(max, v.toString().length());
            }
            for (Iterator<Comparable<?>> i = errors.valuesIterator(); i.hasNext();) {
                Comparable<?> v = i.next();
                b.line("%" + max + "s: %,15d", v.toString(), errors.getCount(v));

            }

        }
        return b.toString();
    }

    public static class Snapshot {
        public StatisticalSummary responseTime;
        public StatisticalSummary concurrency;
        public long errors;
        public double processCpuLoad;
        public double systemCpuLoad;
        public double systemLoadAverage;

    }

    /**
     * @return responseTime.getSummary() and concurrency.getSummary()
     */
    public synchronized Snapshot collect() {
        // swap periodic stats with new ones, read of old so there are no
        // changes of writing while reading

        SummaryStatistics a = elapsedPerSecond;
        SummaryStatistics b = concurrencyPerSecond;

        elapsedPerSecond = new SummaryStatistics();
        concurrencyPerSecond = new SummaryStatistics();

        long err = this.errorCounter.get();
        long diff = err - prevErrors;
        prevErrors = err;

        Snapshot snapshot = new Snapshot();
        snapshot.errors = diff;
        snapshot.responseTime = a.getSummary();
        snapshot.concurrency = b.getSummary();
        // when SystemCpuLoad is read in fast succession it reports an incorrect
        // value.
        // read once as part of the periodic swap operation.
        snapshot.systemLoadAverage = loadMetric("SystemLoadAverage");
        snapshot.systemCpuLoad = loadMetric("SystemCpuLoad") * 100.0;
        snapshot.processCpuLoad = loadMetric("ProcessCpuLoad") * 100.0;

        if (snapshot.responseTime.getN() != snapshot.concurrency.getN())
            throw new IllegalStateException(String.format("stats don't match %s %s",
                    snapshot.responseTime, snapshot.concurrency));
        return snapshot;
    }

    protected double loadMetric(final String metric) {

        try {

            double load = (double) mbs.getAttribute(beanName, metric);
            if (load < 0.0) {
                load = Double.NaN;
            }
            return load;
        } catch (final Exception e) {
            return Double.NaN;
        }

    }

    @Override
    public void close() throws IOException {
        // NOOP
    }
}
