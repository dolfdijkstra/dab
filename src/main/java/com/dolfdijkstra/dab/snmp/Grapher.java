package com.dolfdijkstra.dab.snmp;

import java.awt.image.BufferedImage;
import java.io.IOException;

import com.dolfdijkstra.dab.Dimension;

import org.jrobin.core.RrdException;

public interface Grapher {
    public BufferedImage createGraph(long startTime, long endTime, String title, Dimension dimension)
            throws RrdException, IOException;
}