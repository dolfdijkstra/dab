package com.dolfdijkstra.dab;

import java.io.File;
import java.io.IOException;

import com.dolfdijkstra.dab.snmp.Collector;
import com.dolfdijkstra.dab.snmp.ConcurrencyGrapher;
import com.dolfdijkstra.dab.snmp.DS;
import com.dolfdijkstra.dab.snmp.DS.Type;
import com.dolfdijkstra.dab.snmp.DtGrapher;
import com.dolfdijkstra.dab.snmp.Grapher;
import com.dolfdijkstra.dab.snmp.NVP;
import com.dolfdijkstra.dab.snmp.SnmpRRD;
import com.dolfdijkstra.dab.snmp.TpsGrapher;

import org.jrobin.core.RrdException;

public class TpsCollector {

    private Collector collector;
    private String rrdPath;

    public TpsCollector(File rrdPath) throws IOException, RrdException {
        this(rrdPath, 1);
    }

    public TpsCollector(File rrdPath, int dataResolution) throws IOException, RrdException {
        this.collector = new SnmpRRD(rrdPath, dataResolution);
        this.rrdPath = rrdPath.getAbsolutePath();
    }

    public void createRrdFileIfNecessary() throws RrdException, IOException {

        collector.createRrdFileIfNecessary(

        new DS("tps", Type.GAUGE), new DS("dt", Type.GAUGE), new DS("concurrency", Type.GAUGE), new DS("aconcurrency",
                Type.GAUGE));

    }

    public void update(long timestamp, double tps, double dt, double concurrency, double averageConcurrency)
            throws Exception {
        collector.update(timestamp, new NVP[] { new NVP("tps", tps), new NVP("dt", dt),
                new NVP("concurrency", concurrency), new NVP("aconcurrency", averageConcurrency) });

    }

    public Grapher[] createGraphers() {
        return new Grapher[] { new TpsGrapher(rrdPath), new DtGrapher(rrdPath), new ConcurrencyGrapher(rrdPath) };
    }

}
