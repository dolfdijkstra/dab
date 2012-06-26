package com.dolfdijkstra.dab;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.URI;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import com.dolfdijkstra.dab.snmp.AbstractMetric;
import com.dolfdijkstra.dab.snmp.CpuMetric;
import com.dolfdijkstra.dab.snmp.Grapher;
import com.dolfdijkstra.dab.snmp.LoadAvgMetric;
import com.dolfdijkstra.dab.snmp.Network;
import com.dolfdijkstra.dab.snmp.SnmpGet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.jrobin.core.RrdException;
import org.snmp4j.CommunityTarget;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;

public class App {
    public static final String FILENAME_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HHmmss";

    static String toFilename(String prefix, Date ts, String ext) {
        return toFilename(prefix, ts.getTime(), ext);
    }

    static File toFile(File dir, String prefix, Date ts, String ext) {
        return new File(dir, toFilename(prefix, ts.getTime(), ext));
    }

    static String toFilename(String prefix, long ts, String ext) {
        return prefix + "-" + FastDateFormat.getInstance(FILENAME_TIMESTAMP_FORMAT).format(ts) + "." + ext;

    }

    public static void main(String[] args) throws Exception {

        // -c threads -t runtime -r rampup -i interval -d dir -v level uri

        URI uri = URI.create(args[args.length - 1]);

        WorkerManager manager = new WorkerManager();
        manager.setUri(uri);

        File dir = new File("./test-results");

        for (int i = 0; i < args.length - 2; i = i + 2) {
            if ("-c".equals(args[i])) {
                manager.setWorkers(Integer.parseInt(args[i + 1]));
            } else if ("-t".equals(args[i])) {
                manager.setRuntime(Integer.parseInt(args[i + 1]));
            } else if ("-r".equals(args[i])) {
                manager.setRampup(Integer.parseInt(args[i + 1]));
            } else if ("-i".equals(args[i])) {
                manager.setInterval(Integer.parseInt(args[i + 1]));
            } else if ("-d".equals(args[i])) {
                dir = new File(args[i + 1]).getAbsoluteFile();
            } else if ("-v".equals(args[i])) {
                manager.setVerbose(Integer.parseInt(args[i + 1]));
            }

        }
        dir = dir.getAbsoluteFile();
        dir.mkdirs();

        String ipAddress = InetAddress.getByName(uri.getHost()).getHostAddress();

        int port = 161;
        // Create TransportMapping and Listen
        Date now = new Date();
        AbstractMetric[] metrics = createMetrics(dir, now);

        // Create Target Address object
        String community = "public";
        CommunityTarget comtarget = createCommunityTarget(ipAddress, port, community);

        SnmpGet snmp = new SnmpGet(comtarget, metrics);
        snmp.initMetrics();

        TimeSeriesStat timeSeriesStat = new TimeSeriesStat(manager.getRuntime());

        File tpsFile = toFile(dir, "tps", now, "rrd");
        TpsCollector rrd = new TpsCollector(tpsFile, 1);
        rrd.createRrdFileIfNecessary();

        Statistics statistics = new Statistics(timeSeriesStat);

        BlockingQueue<Object[]> queue = new LinkedBlockingQueue<Object[]>();

        Runnable perSecondTask = new PerSecondRunnable(statistics.getStatPerSecond(),
                statistics.getConcurrencyStatPerSecond(), rrd);

        ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(2);

        timer.scheduleAtFixedRate(perSecondTask, 1, 1, TimeUnit.SECONDS);
        timer.scheduleAtFixedRate(snmp, 1, 1, TimeUnit.SECONDS);
        timer.setKeepAliveTime(1, TimeUnit.MINUTES);

        String filename = toFilename("results", now, "log.gz");

        File file = new File(dir, filename);

        QueueConsumer queueConsumer = new QueueConsumer(queue, statistics);

        Thread t = new Thread(queueConsumer, "QueueConsumer");
        t.start();

        try {
            ResultsWriterCollector results = new ResultsWriterCollector(queue);

            manager.setResults(results);
            manager.work();
        } finally {
            queueConsumer.stop();
            timer.shutdown();
            t.join();
        }
        long end = System.currentTimeMillis();

        snmp.disconnect();

        String infoLine = StringUtils.join(args, ' ');
        if (statistics.getTransactions() > 0) {
            String b = statistics.createSummary(now.getTime(), end);
            try {
                String summaryFilename = toFilename("summary", now, "txt");

                writeSummary(b, new File(dir, summaryFilename), infoLine);
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            System.out.println(infoLine);
            System.out.println();
            System.out.println(b);
            System.out.println();

        }

        file = toFile(dir, "tps", now, "png");
        String title = StringUtils.join(args, ' ', 0, args.length - 1) + "\n"
                + StringUtils.right(args[args.length - 1], 80);

        createGraphs(now.getTime() / 1000L, end / 1000L, title, file, rrd.createGraphers());

        file = toFile(dir, "system", now, "png");
        createGraphs(now.getTime() / 1000L, end / 1000L, title, file, metrics);

        System.out.println("finished " + uri);

    }

    /**
     * @param ipAddress
     * @param port
     * @param community
     * @return
     */
    private static CommunityTarget createCommunityTarget(String ipAddress, int port, String community) {
        CommunityTarget comtarget = new CommunityTarget();

        comtarget.setCommunity(new OctetString(community));
        comtarget.setVersion(SnmpConstants.version1);
        comtarget.setAddress(new UdpAddress(ipAddress + "/" + port));
        comtarget.setRetries(2);
        comtarget.setTimeout(1000);
        return comtarget;
    }

    /**
     * @param b
     * @param infoLine
     * @throws IOException
     */
    private static void writeSummary(String b, File file, String infoLine) throws IOException {
        PrintWriter summaryWriter = new PrintWriter(new FileWriter(file));
        summaryWriter.println(infoLine);
        summaryWriter.println();
        summaryWriter.println(b.toString());
        summaryWriter.flush();
        summaryWriter.close();
    }

    public static void createGraphs(long startTime, long endTime, String title, File file, Grapher[] graphers)
            throws RrdException, IOException {

        int height = 0;
        int width = 0;

        BufferedImage[] bi = new BufferedImage[graphers.length];

        Dimension dim = new Dimension(600, 200);
        for (int i = 0; i < graphers.length; i++) {
            bi[i] = graphers[i].createGraph(startTime, endTime, i == 0 ? title : null, dim);
        }
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

        ImageIO.write(result, "png", file);
    }

    private static AbstractMetric[] createMetrics(File dir, Date start) throws IOException, RrdException {
        AbstractMetric[] metrics = new AbstractMetric[3];
        metrics[0] = new CpuMetric(toFile(dir, "cpuRaw", start, "rrd"));
        metrics[1] = new LoadAvgMetric(toFile(dir, "cpu", start, "rrd"));
        metrics[2] = new Network(toFile(dir, "net", start, "rrd"));
        return metrics;
    }

}
