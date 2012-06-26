package com.dolfdijkstra.dab.snmp;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import com.dolfdijkstra.dab.Dimension;
import com.dolfdijkstra.dab.snmp.DS.Type;

import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdGraphDef;
import org.snmp4j.smi.VariableBinding;

public class LoadAvgMetric extends AbstractMetric {
    private final String[][] map = new String[][] { { "loadAvg", "1.3.6.1.4.1.2021.10.1.5.1" } };

    // { "cpuIdle", "1.3.6.1.4.1.2021.11.11.0" } };
    private final String rrdPath;

    public LoadAvgMetric(final File rrdPath) throws IOException, RrdException {
        super(new SnmpRRD(rrdPath,5));
        this.rrdPath = rrdPath.getAbsolutePath();
        pdu = Util.toPDU(map);
    }

    @Override
    protected NVP[] toNVP(final VariableBinding[] bindings) {
        final NVP[] nvp = new NVP[bindings.length];
        for (int i = 0; i < bindings.length; i++) {
            // bindings[i].getSyntax();
            nvp[i] = new NVP(map[i][0], bindings[i].getVariable().toLong() / 100D);
        }
        return nvp;
    }

    @Override
    public void init(final SnmpGet snmp) throws Exception {
        collector.createRrdFileIfNecessary(new DS("loadAvg", Type.GAUGE));

    }

    @Override
    public BufferedImage createGraph(final long startTime, final long endTime, final String title,
            final Dimension dimension) throws RrdException, IOException {
        final RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.datasource("loadAvg", rrdPath, "loadAvg", "AVERAGE");
        graphDef.area("loadAvg", Color.pink, "");
        graphDef.gprint("loadAvg", "AVERAGE", "Average: @0@r");
        graphDef.setVerticalLabel("Load Average 1m.");
        if (title != null) {
            graphDef.setTitle(title);
        }
        return produceImage(startTime, endTime, dimension, graphDef);
    }

 
}
