package com.dolfdijkstra.dab;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import com.dolfdijkstra.dab.reporting.ConsoleSummary;
import com.dolfdijkstra.dab.reporting.ResponseCollector;
import com.dolfdijkstra.dab.reporting.Statistics;
import com.dolfdijkstra.dab.reporting.WriterSummary;
import com.dolfdijkstra.dab.script.MultipleUriScript;
import com.dolfdijkstra.dab.script.SingleUriScript;

public class DabApp {
    public static final String FILENAME_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HHmmss";
    public static int availableCores = ManagementFactory.getOperatingSystemMXBean()
            .getAvailableProcessors() / 2;

    static String toFilename(final String prefix, final Date ts, final String ext) {
        return toFilename(prefix, ts.getTime(), ext);
    }

    static File toFile(final File dir, final String prefix, final Date ts,
            final String ext) {
        return new File(dir, toFilename(prefix, ts.getTime(), ext));
    }

    static String toFilename(final String prefix, final long ts, final String ext) {
        return prefix + "-"
                + FastDateFormat.getInstance(FILENAME_TIMESTAMP_FORMAT).format(ts)
                + "." + ext;

    }

    public static class Args {

        @Option(name = "--dir", aliases = "-d", required = false, usage = "set the reporing directory. Default is './test-results'.", metaVar = " ")
        public File dir = new File("./test-results");

        @Option(name = "--workers", aliases = "-c", required = false, usage = "set the number of workers to start. Default is: 1.", metaVar = "INT")
        public int workers = 1;

        @Option(name = "--runtime", aliases = "-t", required = false, usage = "set the runtime, for instance 1:05:00 or 120 or 5:30. Default is: 15 seconds.")
        public void runtime(final String val) {
            final String v = val.trim();
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
        public void url(final String uri) throws URISyntaxException {
            urls.add(URI.create(uri));
        }

        @Option(name = "--script", usage = "set the URIs to download, read from a file", metaVar = " FILE ")
        public void urls(final String location) throws IOException,
                URISyntaxException {
            if (new File(location).canRead()) {
                readFromFile(location);
            } else {
                throw new IOException("File '" + location + "' cannot be read");
            }
        }

        private void readFromFile(final String location) throws IOException,
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
            final int cap = 6;
            String uris = urls.stream().limit(cap).map(u -> u.toASCIIString())
                    .collect(Collectors.joining(", "));
            if (urls.size() > cap) {
                uris += ", " + (urls.size() - cap) + " more ...";
            }
            return String
                    .format("workers: %d, runtime: %s, rampup: %d, interval: %d, tps: %.1f, urls: %s",
                            workers, runtime, rampup, interval, tps, uris);
        }

    }

    public static void main(final String[] args) throws Exception {

        final Args arguments = new Args();
        final CmdLineParser parser = new CmdLineParser(arguments);
        try {
            parser.parseArgument(args);
            if (arguments.urls.isEmpty()) {
                throw new CmdLineException(parser, "No URIs provided.", null);
            }
            if (arguments.help) {
                displayUsage(parser, System.out);
            } else {
                run(arguments);
            }
        } catch (final CmdLineException e) {
            // e.printStackTrace();
            System.err.println(e.getMessage());
            displayUsage(parser, System.err);
            System.exit(-1);
        }
    }

    protected static void displayUsage(final CmdLineParser parser,
            final PrintStream out) {
        out.println("Usage:");
        final String program = "java -jar dab-with-dependencies.jar";
        out.println("  " + program + " [-option value]");
        out.println("Options:");
        parser.printUsage(out);
    }

    public static void run(final Args args) throws Exception {
        final Console console = System.console();

        console.printf("\033[2J"); // clear screen
        console.printf("\033[H"); // home cursor home
        console.printf("Starting..."); // home cursor home

        final WorkerManager manager = new WorkerManager();

        final Script script = createScript(args.urls, args.interval);
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

        final File dir = args.dir.getAbsoluteFile();
        dir.mkdirs();

        final Date now = new Date();

        final File tpsFile = toFile(dir, "tps", now, "csv");
        try (final FileWriter writer = new FileWriter(tpsFile)) {
            final Statistics statistics = new Statistics(args.runtime);
            final Runnable perSecondTask = new PerSecondRunnable(statistics,
                    new WriterSummary(writer), new ConsoleSummary(manager));

            final ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(
                    2);

            timer.scheduleAtFixedRate(perSecondTask, 1, 1, TimeUnit.SECONDS);
            timer.setKeepAliveTime(1, TimeUnit.MINUTES);
            ResultsCollector collector = statistics;

            if (args.all) {
                final String filename = toFilename("results", now, "log.gz");
                final File file = new File(dir, filename);
                collector = new ResponseCollector(file, collector);
            }
            final QueueConsumer queueConsumer = new QueueConsumer(collector);

            final Thread t = new Thread(queueConsumer, "QueueConsumer");
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
            final long end = System.currentTimeMillis();

            final String infoLine = "configuration: " + args.toInfo();
            if (statistics.getTransactions() > 0) {
                try {
                    final String summaryFilename = toFilename("summary", now, "txt");
                    final String summary = statistics.createSummary(now.getTime(),
                            end, 9);

                    writeSummary(summary, new File(dir, summaryFilename), infoLine);
                } catch (final IOException e1) {
                    e1.printStackTrace();
                }

                console.writer().print(infoLine);
                console.writer().println();

                console.writer().print(
                        statistics.createSummary(now.getTime(), end,
                                manager.getVerbose()));
                console.writer().println();

            }
            console.printf("finished%n");
        }

    }

    private static Script createScript(final List<URI> s, final int interval) {
        if (s.isEmpty()) {
            throw new IllegalArgumentException("No URIs provided to create a script");
        }
        return s.size() == 1 ? new SingleUriScript(s.get(0), interval)
                : new MultipleUriScript(s, interval);

    }

    /**
     * @param b
     * @param infoLine
     * @throws IOException
     */
    private static void writeSummary(final String b, final File file,
            final String infoLine) throws IOException {
        final PrintWriter summaryWriter = new PrintWriter(new FileWriter(file));
        summaryWriter.println(infoLine);
        summaryWriter.println();
        summaryWriter.println(b);
        summaryWriter.flush();
        summaryWriter.close();
    }

}
