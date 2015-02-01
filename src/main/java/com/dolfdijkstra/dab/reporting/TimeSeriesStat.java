package com.dolfdijkstra.dab.reporting;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class TimeSeriesStat {

    private SummaryStatistics[] series;

    public TimeSeriesStat(final int num) {
        series = new SummaryStatistics[num];
    }

    public void add(final int i, final long value) {
        if (i > series.length - 1) {
            growTo(i + 1);
        }
        if (series[i] == null) {
            series[i] = new SummaryStatistics();
        }
        series[i].addValue(value);
    }

    private void growTo(final int i) {
        final SummaryStatistics[] copy = new SummaryStatistics[i];
        System.arraycopy(series, 0, copy, 0, series.length);
        series = copy;

    }

    public SummaryStatistics get(final int i) {
        return series[i];
    }

    public SummaryStatistics[] getAll() {
        return series;
    }

    public int size() {
        return series.length;
    }
}
