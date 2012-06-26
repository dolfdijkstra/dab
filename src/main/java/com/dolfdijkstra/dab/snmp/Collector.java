package com.dolfdijkstra.dab.snmp;

import java.io.IOException;

import org.jrobin.core.RrdException;

public interface Collector {

    void update(long timestamp, NVP... nvp) throws Exception;

    void createRrdFileIfNecessary(DS... ds) throws RrdException, IOException;

    int getDataResolution();

}
