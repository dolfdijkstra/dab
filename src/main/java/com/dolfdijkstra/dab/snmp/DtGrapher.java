package com.dolfdijkstra.dab.snmp;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.dolfdijkstra.dab.Dimension;

import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdGraphDef;

public class DtGrapher extends AbstractGrapher {
    private final String rrdPath;

    /**
     * @param rrdPath
     */
    public DtGrapher(String rrdPath) {
        super();
        this.rrdPath = rrdPath;
    }

    public BufferedImage createGraph(long startTime, long endTime, String title, Dimension dimension)
            throws RrdException, IOException {
        final RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.datasource("dt", rrdPath, "dt", "AVERAGE");
        graphDef.line("dt", Color.black, "");
        graphDef.gprint("dt", "AVERAGE", "Average: @0@r");
        graphDef.setVerticalLabel("Download time in us.");
        // graphDef.setTitle(title);
        return produceImage(startTime, endTime, dimension, graphDef);

    }
}