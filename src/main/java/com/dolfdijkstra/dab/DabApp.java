package com.dolfdijkstra.dab;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.dolfdijkstra.dab.Statistics.Snapshot;

public class DabApp {
    public static final String FILENAME_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HHmmss";
    private static int availableCores = ManagementFactory.getOperatingSystemMXBean()
            .getAvailableProcessors() / 2;

    static String toFilename(String prefix, Date ts, String ext) {
        return toFilename(prefix, ts.getTime(), ext);
    }

    static File toFile(File dir, String prefix, Date ts, String ext) {
        return new File(dir, toFilename(prefix, ts.getTime(), ext));
    }

    static String toFilename(String prefix, long ts, String ext) {
        return prefix + "-"
                + FastDateFormat.getInstance(FILENAME_TIMESTAMP_FORMAT).format(ts)
                + "." + ext;

    }

    public static final class WriterSummary implements PeriodicSummaryCollector {
        private final PrintWriter writer;
        private final MBeanServer server;
        private final ObjectName osBeanName;
        int i = 0;
        FastDateFormat df = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

        public WriterSummary(FileWriter writer) throws MalformedObjectNameException,
                IOException {

            this.writer = new PrintWriter(writer, true);
            this.server = ManagementFactory.getPlatformMBeanServer();
            this.osBeanName = ObjectName
                    .getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
            writer.write(String
                    .format("count,timestamp,transactions_per_second,errors,response_time_avg,response_time_min,response_time_max,response_time_stddev,min_concurrency,max_concurrency,average_concurrency,system_cpu_load,process_cpu_load,system_load_average%n"));

        }

        @Override
        public void update(long timestamp, long elapsed, Snapshot snapshot) {
            StatisticalSummary responseTimeStat = snapshot.responseTime;
            StatisticalSummary concurrencyStat = snapshot.concurrency;
            long tps = responseTimeStat.getN() * 1000 / elapsed;
            double mean = responseTimeStat.getMean();
            double min = responseTimeStat.getMin();
            double max = responseTimeStat.getMax();
            double stddev = responseTimeStat.getStandardDeviation();

            int maxConcurrency = (int) concurrencyStat.getMax();
            int minConcurrency = (int) concurrencyStat.getMin();
            double cMean = concurrencyStat.getMean();
            double load = Double.NaN;
            double cpu = Double.NaN;
            double proc = Double.NaN;
            try {
                load = (double) server.getAttribute(osBeanName, "SystemLoadAverage");
                cpu = (double) server.getAttribute(osBeanName, "SystemCpuLoad");
                proc = (double) server.getAttribute(osBeanName, "ProcessCpuLoad");
            } catch (Exception e) {
                // silence
            }

            writer.printf(
                    "%d,%s,%d,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%.2f,%.2f,%.2f,%.2f%n",
                    i++, df.format(timestamp * 1000), tps, mean, min, max, stddev,
                    minConcurrency, maxConcurrency, snapshot.errors, cMean,
                    cpu * 100, proc * 100, load);

        }
    }

    public static final class ConsoleSummary implements PeriodicSummaryCollector {

        private final Console console = System.console();
        private final WorkerManager manager;
        private MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory
                .getOperatingSystemMXBean();
        private final ObjectName beanName;

        public ConsoleSummary(WorkerManager manager)
                throws MalformedObjectNameException {
            this.manager = manager;
            beanName = ObjectName
                    .getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
        }

        @Override
        public void update(long timestamp, long elapsed, Snapshot snapshot) {
            if (console == null)
                return;
            StatisticalSummary responseTimeStat = snapshot.responseTime;
            StatisticalSummary concurrencyStat = snapshot.concurrency;
            long tps = responseTimeStat.getN() * 1000 / elapsed;

            double mean = responseTimeStat.getMean();
            int maxConcurrency = (int) concurrencyStat.getMax();
            int minConcurrency = (int) concurrencyStat.getMin();
            double cMean = concurrencyStat.getMean();
            manager.getConcurrentRequestsNumber();
            manager.getWorkerNumber();

            long tps_per_core = tps / availableCores;

            double load = operatingSystemMXBean.getSystemLoadAverage();

            if (load < 0) {
                load = Double.NaN;
            }

            double cpu = loadMetric("SystemCpuLoad");
            double proc = loadMetric("ProcessCpuLoad");

            // console.format(CONSOLE_FORMAT1, tps, tps_per_core, m, ps);
            console.format("\033[5A\r\033[K%-15s: %,9d TPS Errors: %,d%n",
                    "Throughput", tps, snapshot.errors);
            console.format("\r\033[K%-15s: %,9d TPS/core of %d cores%n",
                    "Throughput", tps_per_core, availableCores);
            console.format("\r\033[K%-15s: %s min: %s max: %s%n", "Response time",
                    format("%,9.0f %s", mean),
                    format("%,4.0f %s", responseTimeStat.getMin()),
                    format("%,4.0f %s", responseTimeStat.getMax()));

            console.format(
                    "\r\033[KConcurrency    : now %,d of %,d [ %,d / %,.1f / %,d] [min/avg/max]%n",
                    manager.getConcurrentRequestsNumber(), manager.getWorkerCount(),
                    minConcurrency, cMean, maxConcurrency);
            console.format("\r\033[KLoad: %4.2f cpu: %4.2f%% process: %4.2f%%%n",
                    load, cpu * 100, proc * 100);

        }

        private String format(final String format, final double mean) {
            double m = mean;
            String ps = "μs";
            if (m > 10000000) {
                m = mean / 1000000;
                ps = " beanName";
            } else if (mean > 10000) {
                m = mean / 1000;
                ps = "ms";
            }
            return String.format(format, m, ps);
        }

        protected double loadMetric(String metric) {

            try {
                double load = (double) server.getAttribute(beanName, metric);
                if (load < 0) {
                    load = Double.NaN;
                }
                return load;
            } catch (Exception e) {
                return Double.NaN;
            }

        }

    }

    public static class Args {

        @Option(name = "--dir", aliases = "-d", required = false, usage = "set the reporing directory. Default is './test-results'.", metaVar = " ")
        public File dir = new File("./test-results");

        @Option(name = "--workers", aliases = "-c", required = false, usage = "set the number of workers to start. Default is: 1.", metaVar = "INT")
        public int workers = 1;

        @Option(name = "--runtime", aliases = "-t", required = false, usage = "set the runtime, for instance 1:05:00 or 120 or 5:30. Default is: 15 seconds.")
        public void runtime(String val) {
            String v = val.trim();
            runtime = Util.toSecs(v);
        }

        public int runtime = 15;

        @Option(name = "--rampup", aliases = "-r", usage = "set rampup time in seconds. This is the time in which all the initial workers are started, default is: 0", metaVar = " INT ")
        public int rampup = 0;

        @Option(name = "--interval", aliases = "-i", usage = "set wait time between requests per worker, default is: 0", metaVar = " INT ")
        public int interval = 0;

        @Option(name = "--tps", usage = "set target number of transactions per second, default is: 0.0", metaVar = " DECIMAL ")
        public double tps = 0.0;

        @Option(name = "--verbosity", aliases = "-v", usage = "set the verbosity level for reporting, default is: 0", metaVar = "[0-9]")
        public int verbosity = 0;

        @Option(name = "--log-all", usage = "creates a log file with all the individual response statistics. Default is: false")
        public boolean all = false;

        @Option(name = "--help", aliases = "-h", usage = "help", help = true)
        public boolean help = false;

        @Argument(usage = "set the URIs to download", metaVar = " URI ", multiValued = true, handler = org.kohsuke.args4j.spi.StringArrayOptionHandler.class)
        public void url(String uri) throws URISyntaxException {
            urls.add(URI.create(uri));
        }

        @Option(name = "--script", usage = "set the URIs to download, read from a file", metaVar = " FILE ")
        public void urls(String location) throws IOException, URISyntaxException {
            if (new File(location).canRead()) {
                readFromFile(location);
            } else {
                throw new IOException("File '" + location + "' cannot be read");
            }
        }

        private void readFromFile(String location) throws IOException,
                URISyntaxException {

            Files.lines(Paths.get(location))
                    .filter(s -> StringUtils.isNotBlank(s)
                            && !StringUtils.startsWith(s.trim(), "#"))
                    .map(s -> URI.create(s))
                    .filter(u -> StringUtils.isNotBlank(u.getHost()))
                    .forEach(e -> urls.add(e));
        }

        public List<URI> urls = new ArrayList<URI>();

        public String toInfo() {
            int cap = 6;
            String uris = urls.stream().limit(cap).map(u -> u.toASCIIString())
                    .collect(Collectors.joining(", "));
            if (urls.size() > cap)
                uris += ", " + (urls.size() - cap) + " more ...";
            return String
                    .format("workers: %d, runtime: %s, rampup: %d, interval: %d, tps: %.1f, urls: %s",
                            workers, runtime, rampup, interval, tps, uris);
        }

    }

    public static void main(final String[] args) throws Exception {

        Args arguments = new Args();
        CmdLineParser parser = new CmdLineParser(arguments);
        try {
            parser.parseArgument(args);
            if (arguments.urls.isEmpty())
                throw new CmdLineException(parser, "No URIs provided.", null);
            if (arguments.help)
                displayUsage(parser, System.out);
            else
                run(arguments);
        } catch (CmdLineException e) {
            // e.printStackTrace();
            System.err.println(e.getMessage());
            displayUsage(parser, System.err);
            System.exit(-1);
        }
    }

    protected static void displayUsage(CmdLineParser parser, PrintStream out) {
        out.println("Usage:");
        String program = "java -jar dab-with-dependencies.jar";
        out.println("  " + program + " [-option value]");
        out.println("Options:");
        parser.printUsage(out);
    }

    public static void run(Args args) throws Exception {
        Console console = System.console();

        console.printf("\033[2J"); // clear screen
        console.printf("\033[H"); // home cursor home
        console.printf("Starting..."); // home cursor home

        WorkerManager manager = new WorkerManager();

        Script script = createScript(args.urls, args.interval);
        manager.setScript(script);

        manager.setWorkerNumber(args.workers);

        manager.setTestRuntime(args.runtime);

        manager.setRampupTime(args.rampup);

        manager.setTargetTPS(args.tps);

        manager.setVerbose(args.verbosity);
        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {

            @Override
            public void run() {
                manager.getCondition().flip();
            }
        });

        File dir = args.dir.getAbsoluteFile();
        dir.mkdirs();

        Date now = new Date();

        File tpsFile = toFile(dir, "tps", now, "csv");
        try (final FileWriter writer = new FileWriter(tpsFile)) {
            Statistics statistics = new Statistics(args.runtime);
            Runnable perSecondTask = new PerSecondRunnable(statistics,
                    new WriterSummary(writer), new ConsoleSummary(manager));

            ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(2);

            timer.scheduleAtFixedRate(perSecondTask, 1, 1, TimeUnit.SECONDS);
            timer.setKeepAliveTime(1, TimeUnit.MINUTES);
            ResultsCollector collector = statistics;

            if (args.all) {
                String filename = toFilename("results", now, "log.gz");
                File file = new File(dir, filename);
                collector = new ResponseCollector(file, collector);
            }
            QueueConsumer queueConsumer = new QueueConsumer(collector);

            Thread t = new Thread(queueConsumer, "QueueConsumer");
            t.start();
            try {
                manager.setResults(collector);
                console.printf("\r\033[KRamping up...");
                manager.work();
            } finally {
                Util.closeSilent(queueConsumer);
                timer.shutdown();
                t.join();
                Util.closeSilent(collector);

            }
            long end = System.currentTimeMillis();

            String infoLine = "configuration: " + args.toInfo();
            if (statistics.getTransactions() > 0) {
                String summary = statistics.createSummary(now.getTime(), end,
                        manager.getVerbose());
                try {
                    String summaryFilename = toFilename("summary", now, "txt");

                    writeSummary(summary, new File(dir, summaryFilename), infoLine);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                console.writer().print(infoLine);
                console.writer().println();
                console.writer().print(summary);
                console.writer().println();

            }
            console.printf("finished%n");
        }

    }

    private static Script createScript(List<URI> s, int interval) {
        if (s.isEmpty())
            throw new IllegalArgumentException("No URIs provided to create a script");
        return s.size() == 1 ? new SingleUriScript(s.get(0), interval)
                : new MultipleUriScript(s, interval);

    }

    /**
     * @param b
     * @param infoLine
     * @throws IOException
     */
    private static void writeSummary(String b, File file, String infoLine)
            throws IOException {
        PrintWriter summaryWriter = new PrintWriter(new FileWriter(file));
        summaryWriter.println(infoLine);
        summaryWriter.println();
        summaryWriter.println(b);
        summaryWriter.flush();
        summaryWriter.close();
    }

}
