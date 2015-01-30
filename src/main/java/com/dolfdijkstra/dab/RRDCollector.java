package com.dolfdijkstra.dab;

import java.io.File;
import java.io.IOException;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.jrobin.core.RrdException;

import com.dolfdijkstra.dab.Statistics.Snapshot;
import com.dolfdijkstra.dab.snmp.Collector;
import com.dolfdijkstra.dab.snmp.ConcurrencyGrapher;
import com.dolfdijkstra.dab.snmp.DS;
import com.dolfdijkstra.dab.snmp.DS.Type;
import com.dolfdijkstra.dab.snmp.DtGrapher;
import com.dolfdijkstra.dab.snmp.Grapher;
import com.dolfdijkstra.dab.snmp.NVP;
import com.dolfdijkstra.dab.snmp.SnmpRRD;
import com.dolfdijkstra.dab.snmp.TpsGrapher;

public class RRDCollector implements PeriodicSummaryCollector {

    private Collector collector;
    private String rrdPath;

    public RRDCollector(File rrdPath) throws IOException, RrdException {
        this(rrdPath, 1);
    }

    public RRDCollector(File rrdPath, int dataResolution) throws IOException,
            RrdException {
        this.collector = new SnmpRRD(rrdPath, dataResolution);
        this.rrdPath = rrdPath.getAbsolutePath();
    }

    public void createRrdFileIfNecessary() throws RrdException, IOException {

        collector.createRrdFileIfNecessary(new DS("tps", Type.GAUGE), new DS("dt",
                Type.GAUGE), new DS("concurrency", Type.GAUGE), new DS(
                "aconcurrency", Type.GAUGE));

    }

    @Override
    public void update(long timestamp, long elapsed, Snapshot snapshot) {
        StatisticalSummary responseTimeStat = snapshot.responseTime;
        StatisticalSummary concurrencyStat = snapshot.concurrency;
        double dt = responseTimeStat.getMean();
        long tps = responseTimeStat.getN() * 1000 / elapsed;

        double concurrency = concurrencyStat.getMax();
        double averageConcurrency = concurrencyStat.getMean();
        try {
            collector.update(timestamp, new NVP[] { new NVP("tps", tps),
                    new NVP("dt", dt), new NVP("concurrency", concurrency),
                    new NVP("aconcurrency", averageConcurrency) });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Grapher[] createGraphers() {
        return new Grapher[] { new TpsGrapher(rrdPath), new DtGrapher(rrdPath),
                new ConcurrencyGrapher(rrdPath) };
    }

}
