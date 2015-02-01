package com.dolfdijkstra.dab.reporting;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class TimeSeriesStat {

    private SummaryStatistics[] series;

    public TimeSeriesStat(int num) {
        series = new SummaryStatistics[num];
    }

    public void add(int i, long value) {
        if (i > series.length - 1) {
            growTo(i + 1);
        }
        if (series[i] == null)
            series[i] = new SummaryStatistics();
        series[i].addValue(value);
    }

    private void growTo(int i) {
        SummaryStatistics[] copy = new SummaryStatistics[i];
        System.arraycopy(series, 0, copy, 0, series.length);
        series = copy;

    }

    public SummaryStatistics get(int i) {
        return series[i];
    }

    public SummaryStatistics[] getAll() {
        return series;
    }

    public int size() {
        return series.length;
    }
}
