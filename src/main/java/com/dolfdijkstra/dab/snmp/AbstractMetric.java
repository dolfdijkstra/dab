package com.dolfdijkstra.dab.snmp;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.dolfdijkstra.dab.Dimension;

import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;
import org.jrobin.graph.TimeAxisUnit;
import org.snmp4j.PDU;
import org.snmp4j.smi.VariableBinding;

/**
 * @author dolf
 *
 */
public abstract class AbstractMetric implements Metric, Grapher {

    protected PDU pdu;

    protected abstract NVP[] toNVP(VariableBinding[] bindings);

    protected final Collector collector;

    public AbstractMetric(Collector collector) {
        this.collector = collector;
    }
    @Override
    public final int getPollTime() {
        return collector.getDataResolution();
    }
    public final PDU getPDU() {
        return pdu;
    }

    public final void handleResponse(PDU responsePDU) {
        VariableBinding[] bindings = responsePDU.toArray();
        NVP[] nvp = toNVP(bindings);
        try {
            long now = org.jrobin.core.Util.getTime();
            collector.update(now, nvp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // for (VariableBinding vb : bindings) {
        // Variable v = vb.getVariable();
        // System.out.println("Snmp Get Response = " + vb.getOid() + ": " +
        // v.getSyntaxString() + ": " + v.toString());
        //
        // }
    }

    /**
     * @param pdu the pdu to set
     */
    public final void setPdu(PDU pdu) {
        this.pdu = pdu;
    }

    /**
     * @param startTime
     * @param endTime
     * @param dimension
     * @param graphDef
     * @return
     * @throws RrdException
     * @throws IOException
     */
    public BufferedImage produceImage(long startTime, long endTime, Dimension dimension, final RrdGraphDef graphDef)
            throws RrdException, IOException {
        graphDef.setBackColor(Color.white);
        graphDef.setImageBorder(Color.white, 0);
        graphDef.setShowSignature(false);

        setTicks(startTime, endTime, graphDef);
        graphDef.setTimePeriod(startTime - 1, endTime + 1);

        RrdGraph rrdGraph = new RrdGraph(graphDef, true); // uses pool

        return rrdGraph.getBufferedImage(dimension.getWidth(), dimension.getHeight());
    }

    /**
     * @param startTime
     * @param endTime
     * @param graphDef
     */
    protected void setTicks(long startTime, long endTime, final RrdGraphDef graphDef) {
        // per graph
        // between 2 and 20 major ticks
        // minor ticks at 20%

        if (endTime - startTime < 60) {
            graphDef.setTimeAxis(TimeAxisUnit.SECOND, 2, TimeAxisUnit.SECOND, 5, "HH:mm:ss", false);
        } else if (endTime - startTime < 60 * 2) {
            graphDef.setTimeAxis(TimeAxisUnit.SECOND, 10, TimeAxisUnit.SECOND, 20, "HH:mm:ss", false);
        } else if (endTime - startTime < 60 * 5) {
            graphDef.setTimeAxis(TimeAxisUnit.SECOND, 10, TimeAxisUnit.SECOND, 30, "HH:mm:ss", false);
        } else if (endTime - startTime < 60 * 60) {
            graphDef.setTimeAxis(TimeAxisUnit.MINUTE, 5, TimeAxisUnit.MINUTE, 15, "HH:mm", false);
        } else if (endTime - startTime < 2 * 60 * 60) {
            graphDef.setTimeAxis(TimeAxisUnit.MINUTE, 20, TimeAxisUnit.MINUTE, 35, "HH:mm", false);
        } else {
            graphDef.setTimeAxis(TimeAxisUnit.MINUTE, 15, TimeAxisUnit.HOUR, 1, "HH:mm", false);
        }
        /*
         * This sets the grid and labels on the X axis. There are both minor and
         * major grid lines, the major lines are accompanied by a time label. To
         * define a grid line you must define a specific time unit, and a number
         * of time steps. A grid line will appear everey steps*unit. Possible
         * units are defined in the TimeAxisUnit class, and are SECOND, MINUTE,
         * HOUR, DAY, WEEK, MONTH and YEAR.
         */
    }

}
