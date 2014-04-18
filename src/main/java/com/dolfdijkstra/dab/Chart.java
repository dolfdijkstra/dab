package com.dolfdijkstra.dab;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetChangeListener;
import org.jfree.data.general.DatasetGroup;

public class Chart {

    static class CategoryDatasetAdapter implements CategoryDataset {
        TimeSeriesStat stat;

        @Override
        public Comparable<?> getRowKey(int row) {
            return Integer.toString(row);
        }

        @Override
        public int getRowIndex(@SuppressWarnings("rawtypes") Comparable key) {
            return Integer.parseInt(key.toString());
        }

        @Override
        public List<String> getRowKeys() {
            int s = stat.size();
            List<String> list = new LinkedList<String>();
            for (int i = 0; i < s; i++) {
                if (stat.get(i) != null)
                    list.add(Integer.toString(i));
            }
            return list;
        }

        @Override
        public Comparable<String> getColumnKey(int column) {

            return "tps";
        }

        @Override
        public int getColumnIndex(@SuppressWarnings("rawtypes") Comparable key) {
            return 1;
        }

        @Override
        public List<String> getColumnKeys() {
            return Collections.singletonList("tps");
        }

        @Override
        public Number getValue(@SuppressWarnings("rawtypes") Comparable rowKey, @SuppressWarnings("rawtypes") Comparable columnKey) {
            return stat.get(getRowIndex(rowKey)).getN();
        }

        @Override
        public int getRowCount() {

            return 0;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Number getValue(int row, int column) {
            return stat.get(row).getN();
        }

        @Override
        public void addChangeListener(DatasetChangeListener listener) {
            // TODO Auto-generated method stub

        }

        @Override
        public void removeChangeListener(DatasetChangeListener listener) {
            // TODO Auto-generated method stub

        }

        private DatasetGroup group;

        @Override
        public DatasetGroup getGroup() {
            return group;
        }

        @Override
        public void setGroup(DatasetGroup group) {
            this.group = group;
        }

    }

    public void write(TimeSeriesStat stat, File filename) throws IOException {

        DefaultCategoryDataset tps = new DefaultCategoryDataset();
        for (int i = 0; i < stat.size(); i++) {
            tps.setValue(stat.get(i).getN(), "tps", Integer.toString(i));
        }

        JFreeChart chart = ChartFactory.createBarChart("Transactions per second", "Period", "tps", tps,
                PlotOrientation.VERTICAL, false, false, false);
        chart.setBackgroundPaint(Color.white);
        chart.getTitle().setPaint(Color.black);

        chart.getCategoryPlot().getRenderer().setSeriesPaint(0, Color.green);

        CategoryPlot p = chart.getCategoryPlot();
        p.setRangeGridlinePaint(Color.blue);
        

        ChartUtilities.saveChartAsPNG(filename, chart, 800, 300);
    }

}
