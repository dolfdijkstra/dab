package com.dolfdijkstra.dab;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class Statistics implements StatisticsCollector {
    private final NumberFormat nf = DecimalFormat.getIntegerInstance(Locale.US);
    private final NumberFormat ndf = DecimalFormat.getNumberInstance(Locale.US);
    private final SummaryStatistics stat = new SummaryStatistics();
    private final SummaryStatistics sizeStat = new SummaryStatistics();

    private final Frequency statusFrequency = new Frequency();
    private final Frequency concurrencyFrequency = new Frequency();
    private final Frequency idFrequency = new Frequency();
    private final TimeSeriesStat timeSeriesStat;
    private final SummaryStatistics statPerSecond = new SummaryStatistics();

    private final SummaryStatistics concurrencyStatPerSecond = new SummaryStatistics();
    private final AtomicLong errorCounter = new AtomicLong();
    private final Frequency errors = new Frequency();

    public Statistics(TimeSeriesStat timeSeriesStat) {
        this.timeSeriesStat = timeSeriesStat;
    }

    public void addToStats(Object[] result) {
        if (result.length == 1 && result[0] instanceof Exception) {
            Exception e = (Exception) result[0];
            errors.addValue(e.getClass().getName());
            return;
        }

        for (int i = 0; i < result.length; i++) {
            if (i == 2) {
                long value = (Long) result[i];
                stat.addValue(value);
                this.statPerSecond.addValue(value);
                Long period = (((Long) result[1]) / 1000L);
                this.timeSeriesStat.add(period.intValue(), value);

            } else if (i == 3) {
                Integer v = (Integer) result[i];
                statusFrequency.addValue(v.intValue());
            } else if (i == 4) {
                Long v = (Long) result[i];
                if (v.longValue() > -1)
                    this.sizeStat.addValue(v.intValue());
            } else if (i == 5) {
                Integer v = (Integer) result[i];
                idFrequency.addValue(v.intValue());
            } else if (i == 6) {
                Integer v = (Integer) result[i];
                concurrencyFrequency.addValue(v.intValue());
                this.concurrencyStatPerSecond.addValue(v);
            }
        }
    }

    public long getTransactions() {
        return stat.getN();
    }

    /**
     * @param duration
     * @return
     */
    public String createSummary(long start, long end, int verbose) {
        long duration = (end - start) / 1000L;
        StringBuilder b = new StringBuilder();

        String EOL = "\n";
        int pad = 15;
        FastDateFormat df = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");
        b.append("Test ran between ").append(df.format(start)).append(" and ").append(df.format(end)).append(EOL);
        b.append(EOL);
        b.append("Test duration:      " + StringUtils.leftPad(nf.format(duration), pad) + " seconds.").append(EOL);

        b.append("Number of requests: " + StringUtils.leftPad(nf.format(stat.getN()), pad) + ".").append(EOL);
        b.append("Errors:             " + StringUtils.leftPad(nf.format(errorCounter.get()), pad) + ".").append(EOL);
        b.append("Average throughput: " + StringUtils.leftPad(nf.format(stat.getN() / duration), pad) + " tps.")
                .append(EOL);
        b.append("Mean:               " + StringUtils.leftPad(nf.format(stat.getMean()), pad) + " μs.").append(EOL);
        b.append("Min:                " + StringUtils.leftPad(nf.format(stat.getMin()), pad) + " μs.").append(EOL);
        b.append("Max:                " + StringUtils.leftPad(nf.format(stat.getMax()), pad) + " μs.").append(EOL);
        b.append("StandardDeviation:  " + StringUtils.leftPad(nf.format(stat.getStandardDeviation()), pad) + " μs.")
                .append(EOL);
        b.append(EOL);
        b.append("Average size:       " + StringUtils.leftPad(nf.format(sizeStat.getMean()), pad) + " bytes.").append(
                EOL);
        double network = sizeStat.getMean() * sizeStat.getN() / (duration * 1024D * 1024D);
        b.append("Network Throughput: " + StringUtils.leftPad(ndf.format(network), pad) + " Mb/s.").append(EOL);

        b.append(EOL);
        b.append("HTTP status").append(EOL);

        for (Iterator<Comparable<?>> i = statusFrequency.valuesIterator(); i.hasNext();) {
            Comparable<?> v = i.next();
            b.append(v + ": " + StringUtils.leftPad(nf.format(statusFrequency.getCount(v)), pad) + ".").append(EOL);

        }
        
        if (verbose > 2) {
            b.append(EOL);
            b.append("Concurrency").append(EOL);

            for (Iterator<Comparable<?>> i = concurrencyFrequency.valuesIterator(); i.hasNext();) {
                Comparable<?> v = i.next();
                b.append(
                        StringUtils.leftPad(v.toString(), 4) + ": "
                                + StringUtils.leftPad(nf.format(concurrencyFrequency.getCount(v)), pad) + ".").append(
                        EOL);

            }
            b.append(EOL);
            b.append("Worker distribution").append(EOL);
            for (Iterator<Comparable<?>> i = idFrequency.valuesIterator(); i.hasNext();) {
                Comparable<?> v = i.next();
                b.append(
                        StringUtils.leftPad(v.toString(), 4) + ": "
                                + StringUtils.leftPad(nf.format(idFrequency.getCount(v)), pad) + ".").append(EOL);

            }

        }
        if (verbose > 3) {
            b.append(EOL);
            b.append("Period distribution").append(EOL);
            for (int i = 0; i < timeSeriesStat.size(); i++) {
                b.append(
                        StringUtils.leftPad(Integer.toString(i), 6) + ": "
                                + StringUtils.leftPad(nf.format(timeSeriesStat.get(i).getN()), pad) + ":"
                                + StringUtils.leftPad(nf.format(timeSeriesStat.get(i).getMean()), pad)).append(EOL);

            }
        }
        if (verbose >= 0 && errorCounter.get() > 0) {
            b.append(EOL);
            b.append("Errors").append(EOL);
            int max = 0;
            for (Iterator<Comparable<?>> i = errors.valuesIterator(); i.hasNext();) {
                Comparable<?> v = i.next();
                max = Math.max(max, v.toString().length());
            }
            for (Iterator<Comparable<?>> i = errors.valuesIterator(); i.hasNext();) {
                Comparable<?> v = i.next();
                b.append(
                        StringUtils.leftPad(v.toString(), max) + ": "
                                + StringUtils.leftPad(nf.format(errors.getCount(v)), pad) + ".").append(EOL);

            }

        }
        return b.toString();
    }

    /**
     * @return the statPerSecond
     */
    public SummaryStatistics getStatPerSecond() {
        return statPerSecond;
    }

    /**
     * @return the concurrencyStatPerSecond
     */
    public SummaryStatistics getConcurrencyStatPerSecond() {
        return concurrencyStatPerSecond;
    }
}
