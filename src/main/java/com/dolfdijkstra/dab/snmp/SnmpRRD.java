package com.dolfdijkstra.dab.snmp;

import java.io.File;
import java.io.IOException;

import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.Sample;

public class SnmpRRD implements Collector {

    private String rrdPath;

    private int dataResolution = 1; // seconds

    private RrdDbPool rrdPool = RrdDbPool.getInstance();

    public SnmpRRD(File rrdPath) throws IOException, RrdException {
        this(rrdPath, 1);
    }

    public SnmpRRD(File rrdPath, int dataResolution) throws IOException, RrdException {
        this.rrdPath = rrdPath.getAbsolutePath();
        this.dataResolution = dataResolution;
    }

    public void createRrdFileIfNecessary(DS... ds) throws RrdException, IOException {
        // create RRD file since it does not exist
        if (!new File(rrdPath).exists()) {

            RrdDef rrdDef = new RrdDef(rrdPath, dataResolution);
            // single gauge datasource, named 'tps'
            for (DS d : ds) {
                rrdDef.addDatasource(d.getName(), d.getType(), 5 * dataResolution, 0, Double.NaN);
            }

            // several archives
            rrdDef.addArchive("AVERAGE", 0.5, 1, 4000);
            rrdDef.addArchive("AVERAGE", 0.5, 6, 4000);
            rrdDef.addArchive("AVERAGE", 0.5, 24, 4000);
            rrdDef.addArchive("AVERAGE", 0.5, 288, 4000);
            // create RRD file in the pool
            RrdDb rrdDb = rrdPool.requestRrdDb(rrdDef);
            rrdPool.release(rrdDb);
        }
    }

    @Override
    public void update(long timestamp, NVP... nvp) throws Exception {
        RrdDb rrdDb = rrdPool.requestRrdDb(rrdPath);
        Sample sample = rrdDb.createSample(timestamp);
        for (NVP n : nvp) {
            sample.setValue(n.getName(), n.getValue());
        }
        sample.update();
        rrdPool.release(rrdDb);

    }

    /**
     * @return the dataResolution
     */
    public int getDataResolution() {
        return dataResolution;
    }

}
