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

public class Network extends AbstractMetric {
    private String oid_ifNumber = "1.3.6.1.2.1.2.1.0";
    private String[] names;
    private String[][] map;
    private String rrdPath;

    public Network(File rrdPath) throws IOException, RrdException {
        super(new SnmpRRD(rrdPath, 15));
        this.rrdPath = rrdPath.getAbsolutePath();
    }

    public void init(SnmpGet snmp) throws Exception {

        int num = snmp.getInt(oid_ifNumber);
        names = new String[num];

        String oid_ifDescr = "1.3.6.1.2.1.2.2.1.2";
        String oid_ifInOctets = "1.3.6.1.2.1.2.2.1.10";
        String oid_ifOutOctets = "1.3.6.1.2.1.2.2.1.16";
        map = new String[num * 2][2];
        DS[] ds = new DS[num * 2];
        for (int i = 0; i < num; i++) {
            names[i] = snmp.getString(oid_ifDescr + "." + (i + 1));
            map[i * 2] = new String[] { names[i] + "_in", oid_ifInOctets + "." + (i + 1) };
            map[i * 2 + 1] = new String[] { names[i] + "_out", oid_ifOutOctets + "." + (i + 1) };
            ds[i * 2] = new DS(names[i] + "_in", Type.COUNTER);
            ds[i * 2 + 1] = new DS(names[i] + "_out", Type.COUNTER);

        }
        pdu = Util.toPDU(map);
        collector.createRrdFileIfNecessary(ds);

    }

    @Override
    protected NVP[] toNVP(VariableBinding[] bindings) {
        NVP[] nvp = new NVP[bindings.length];
        for (int i = 0; i < bindings.length; i++) {
            nvp[i] = new NVP(map[i][0], bindings[i].getVariable().toLong());
            System.out.println(nvp[i].getName() + " " + nvp[i].getValue() + " " + bindings[i].getVariable());
        }
        return nvp;
    }

    @Override
    public BufferedImage createGraph(long startTime, long endTime, String title, Dimension dimension)
            throws RrdException, IOException {
        final RrdGraphDef graphDef = new RrdGraphDef();
        for (int i = 0; i < map.length; i++) {
            graphDef.datasource(map[i][0], rrdPath, map[i][0], "AVERAGE");
        }
        for (int i = 1; i < map.length; i = i + 2) {
            graphDef.datasource(map[i][0] + "neg", map[i][0] + ",-1,*");
        }
        Color cIn = Color.blue;
        Color cOut = Color.orange;
        for (int i = 0; i < map.length; i = i + 2) {
            graphDef.line(map[i][0], cIn, "");
            graphDef.line(map[i + 1][0] + "neg", cOut, "");
            cIn = cIn.brighter();
            cOut = cOut.brighter();
        }

        graphDef.setVerticalLabel("Network");
        graphDef.setShowLegend(false);
        if (title != null)
            graphDef.setTitle(title);
        return produceImage(startTime, endTime, dimension, graphDef);
    }
}
