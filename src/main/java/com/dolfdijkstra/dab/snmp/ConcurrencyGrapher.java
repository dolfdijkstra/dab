package com.dolfdijkstra.dab.snmp;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;

import com.dolfdijkstra.dab.Dimension;

import org.jrobin.core.RrdException;
import org.jrobin.graph.RrdGraphDef;

public class ConcurrencyGrapher extends AbstractGrapher {
    private final String rrdPath;

    /**
     * @param rrdPath
     */
    public ConcurrencyGrapher(String rrdPath) {
        super();
        this.rrdPath = rrdPath;
    }

    public BufferedImage createGraph(long startTime, long endTime, String title, Dimension dimension)
            throws RrdException, IOException {
        final RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.datasource("c", rrdPath, "concurrency", "AVERAGE");
        graphDef.line("c", Color.green, "");
        graphDef.datasource("a", rrdPath, "aconcurrency", "AVERAGE");
        graphDef.line("a", Color.blue, "");
        graphDef.setMinorGridY(false);
        graphDef.setLowerLimit(0);
        graphDef.setShowLegend(false);
        graphDef.setVerticalLabel("Concurrency.");
        // graphDef.setTitle(title);
        return produceImage(startTime, endTime, dimension, graphDef);

    }
}