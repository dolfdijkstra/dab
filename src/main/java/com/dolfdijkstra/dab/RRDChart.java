package com.dolfdijkstra.dab;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.imageio.ImageIO;

import org.jrobin.core.RrdDb;
import org.jrobin.core.RrdDbPool;
import org.jrobin.core.RrdDef;
import org.jrobin.core.RrdException;
import org.jrobin.core.Sample;
import org.jrobin.graph.RrdGraph;
import org.jrobin.graph.RrdGraphDef;
import org.jrobin.graph.TimeAxisUnit;

public class RRDChart {

    private String rrdPath;

    private int dataResolution = 1; // seconds

    private RrdDbPool rrdPool = RrdDbPool.getInstance();

    public RRDChart(String rrdPath) throws IOException, RrdException {
        this(rrdPath, 1);
    }

    public RRDChart(String rrdPath, int dataResolution) throws IOException, RrdException {
        this.rrdPath = rrdPath;
        this.dataResolution = dataResolution;
    }

    public void createRrdFileIfNecessary() throws RrdException, IOException {
        // create RRD file since it does not exist
        if (!new File(rrdPath).exists()) {

            RrdDef rrdDef = new RrdDef(rrdPath, dataResolution);
            // single gauge datasource, named 'tps'
            rrdDef.addDatasource("tps", "GAUGE", 5 * dataResolution, 0, Double.NaN);
            rrdDef.addDatasource("dt", "GAUGE", 5 * dataResolution, 0, Double.NaN);
            rrdDef.addDatasource("concurrency", "GAUGE", 5 * dataResolution, 0, Double.NaN);
            rrdDef.addDatasource("aconcurrency", "GAUGE", 5 * dataResolution, 0, Double.NaN);

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

    public void updateRRD(long timestamp, double tps, double dt, double concurrency, double averageConcurrency)
            throws IOException, RrdException {

        RrdDb rrdDb = rrdPool.requestRrdDb(rrdPath);
        Sample sample = rrdDb.createSample(timestamp);
        sample.setValue("tps", tps);
        sample.setValue("dt", dt);
        sample.setValue("concurrency", concurrency);
        sample.setValue("aconcurrency", averageConcurrency);
        sample.update();
        rrdPool.release(rrdDb);

    }

    public void createGraphs(long startTime, long endTime, String title, String file) throws RrdException, IOException {

        int height = 0;
        int width = 0;

        BufferedImage[] bi = new BufferedImage[3];
        Dimension dim = new Dimension(600, 200);
        bi[0] = createTpsGraph(startTime, endTime, title, dim);
        bi[1] = createDtGraph(startTime, endTime, title, dim);
        bi[2] = createConcurrencyGraph(startTime, endTime, title, dim);
        for (BufferedImage b : bi) {
            width = Math.max(width, b.getWidth());
            height += b.getHeight();
        }
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = result.getGraphics();
        int x = 0;
        int y = 0;
        for (BufferedImage b : bi) {
            g.drawImage(b, x, y, null);
            y += b.getHeight();
        }

        ImageIO.write(result, "png", new File(file));
    }

    public BufferedImage createDtGraph(long startTime, long endTime, String title, Dimension dimension)
            throws RrdException, IOException {
        final RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.datasource("dt", rrdPath, "dt", "AVERAGE");
        graphDef.line("dt", Color.black, "");
        graphDef.gprint("dt", "AVERAGE", "Average: @0@r");
        graphDef.setVerticalLabel("Download time in us.");
        // graphDef.setTitle(title);
        return produceImage(startTime, endTime, dimension, graphDef);

    }

    public BufferedImage createConcurrencyGraph(long startTime, long endTime, String title, Dimension dimension)
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

    /**
     * @param startTime
     * @param endTime
     * @param graphDef
     */
    private void setTicks(long startTime, long endTime, final RrdGraphDef graphDef) {
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

    public BufferedImage createTpsGraph(long startTime, long endTime, String title, Dimension dimension)
            throws RrdException, IOException {

        final RrdGraphDef graphDef = new RrdGraphDef();
        graphDef.datasource("tps", rrdPath, "tps", "AVERAGE");
        graphDef.area("tps", Color.PINK, "");
        graphDef.gprint("tps", "AVERAGE", "Average: @0@r");
        graphDef.setVerticalLabel("Transactions per second");
        graphDef.setTitle(title);
        return produceImage(startTime, endTime, dimension, graphDef);

    }

    /**
     * @param startTime
     * @param endTime
     * @param title
     * @param dimension
     * @param graphDef
     * @return
     * @throws RrdException
     * @throws IOException
     */
    private BufferedImage produceImage(long startTime, long endTime, Dimension dimension, final RrdGraphDef graphDef)
            throws RrdException, IOException {
        graphDef.setBackColor(Color.white);
        graphDef.setShowSignature(false);
        setTicks(startTime, endTime, graphDef);
        graphDef.setTimePeriod(startTime - 1, endTime + 1);
        // graphDef.setChartLeftPadding(10);
        graphDef.setImageBorder(Color.white, 0);
        setTicks(startTime, endTime, graphDef);

        graphDef.setTimePeriod(startTime - 1, endTime + 1);

        RrdGraph rrdGraph = new RrdGraph(graphDef, true); // uses pool

        return rrdGraph.getBufferedImage(dimension.getWidth(), dimension.getHeight());
    }

    void multiple() throws IOException {
        int height = 300;
        int width = 800;
        BufferedImage result = new BufferedImage(width, height, // work these
                                                                // out
                BufferedImage.TYPE_INT_RGB);
        Graphics g = result.getGraphics();
        String[] images = new String[0];
        int x = 0;
        int y = 0;
        int last_y = 0;
        for (String image : images) {
            BufferedImage bi = ImageIO.read(new File(image));
            if (x + bi.getWidth() > result.getWidth()) {
                x = 0;
                y += bi.getHeight();
            }

            g.drawImage(bi, x, y, null);
            x += bi.getWidth();
            last_y += bi.getHeight();

        }
        ImageIO.write(result, "png", new File("result.png"));

    }

    // reports exception by printing it on the stderr device
    private static void reportException(Exception e) {
        System.err.println("ERROR [" + new Date() + "]: " + e);
    }

}
