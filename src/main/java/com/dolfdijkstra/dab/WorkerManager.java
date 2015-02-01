package com.dolfdijkstra.dab;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

public class WorkerManager {

    private final AtomicInteger workerCount = new AtomicInteger();
    private final CountDownLatch latch = new CountDownLatch(1);

    private int workers = 1;
    private int rampup = 0;
    private int runtime = 5;
    private int startedWorkers = 0;

    private BooleanCondition condition;

    private ResultsCollector collector;

    private final AtomicInteger active = new AtomicInteger();

    private Script script;
    private long startTime;
    private int verbose;
    private final AtomicLong requestCount = new AtomicLong();

    private double targetTPS;

    CloseableHttpClient createClient(final HttpClientConnectionManager cm) {
        final org.apache.http.client.config.RequestConfig.Builder requestConfigBuilder = RequestConfig
                .custom();

        requestConfigBuilder.setStaleConnectionCheckEnabled(false);
        requestConfigBuilder.setConnectionRequestTimeout(1000); // time to wait
                                                                // for a
                                                                // connection
                                                                // from the pool
        requestConfigBuilder.setConnectTimeout(1000); // fail fast on a socket
                                                      // connection attempt
        requestConfigBuilder.setSocketTimeout(15000);
        final RequestConfig requestConfig = requestConfigBuilder.build();

        final OperatingSystemMXBean o = ManagementFactory.getOperatingSystemMXBean();

        final HttpClientBuilder builder = HttpClients.custom().setConnectionManager(
                cm);
        builder.disableAutomaticRetries();
        builder.setDefaultRequestConfig(requestConfig);

        builder.setUserAgent("dab/1.0 (" + o.getName() + "; " + o.getVersion()
                + "; " + o.getArch() + ")");
        // builder.disableContentCompression();
        return builder.build();
    }

    void work() throws Exception {
        startTime = System.currentTimeMillis();
        condition = new BooleanCondition();

        final int pace = workers == 1 ? 0 : rampup * 1000 / (workers - 1);
        final long start = System.currentTimeMillis();
        final long end = start + runtime * 1000L;
        for (startedWorkers = 0; condition.isValid() && startedWorkers < workers;) {
            if (startedWorkers > 0 && pace > 0) {
                sleep(pace);
            }
            startWorker();
        }
        long now;
        long last = System.currentTimeMillis();
        final double[] last_tps = new double[5];
        int i = 0;
        while (condition.isValid() && (now = System.currentTimeMillis()) < end) {
            Thread.sleep(250);
            if (targetTPS > 0 && now - last >= 1000) {
                final double tps = requestCount.getAndSet(0) * 1000.0 / (now - last);
                last_tps[i] = tps;
                if (StatUtils.max(last_tps) < targetTPS) {
                    startWorker();
                }
                i = i++ % last_tps.length;
                last = now;
            }
        }
        condition.flip();
        latch.await();
        System.out.println("\nAll workers finished at "
                + (System.currentTimeMillis() - start));

    }

    protected void startWorker() {
        final HttpClientConnectionManager cm = createHttpClientConnectionManager();
        final HttpWorker worker = new HttpWorker(this, createClient(cm), cm,
                startedWorkers);
        final Thread t = new Thread(worker);
        t.setName("HttpWorker-" + startedWorkers);
        startedWorkers++;
        t.start();
    }

    public HttpClientConnectionManager createHttpClientConnectionManager() {
        return new BasicHttpClientConnectionManager();
    }

    public void startWorker(final int id) {
        workerCount.incrementAndGet();

    }

    public void finishWorker(final int id) {
        if (workerCount.decrementAndGet() == 0) {
            latch.countDown();
        }
    }

    /**
     * @return the current number of concurrent downloads
     */
    public int getConcurrentRequestsNumber() {
        return active.get();
    }

    /**
     * @return the collector
     */
    public ResultsCollector getCollector() {
        return collector;
    }

    /**
     * @return the active
     */
    public int startRequest() {
        requestCount.incrementAndGet();
        return active.incrementAndGet();
    }

    /**
     * @return the active
     */
    public int endRequest() {
        return active.decrementAndGet();
    }

    /**
     * @return the workers
     */
    public int getWorkerNumber() {
        return workers;
    }

    /**
     * @param workers
     *            the workers to set
     */
    public void setWorkerNumber(final int workers) {
        this.workers = Math.max(0, workers);
    }

    /**
     * @return the rampup
     */
    public int getRampupTime() {
        return rampup;
    }

    /**
     * Start all the works in rampup seconds
     *
     * @param rampup
     *            the rampup to set
     */
    public void setRampupTime(final int rampup) {
        this.rampup = Math.max(0, rampup);
    }

    /**
     * @return the runtime
     */
    public int getTestRuntime() {
        return runtime;
    }

    /**
     * @param runtime
     *            the runtime to set
     */
    public void setTestRuntime(final int runtime) {
        this.runtime = Math.max(0, runtime);
    }

    /**
     * @param collector
     *            the collector to set
     */
    public void setCollector(final ResultsCollector collector) {
        this.collector = collector;
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    public void setVerbose(final int verbose) {
        this.verbose = verbose;

    }

    /**
     * @return the verbose
     */
    public int getVerbose() {
        return verbose;
    }

    public Script getScript() {
        return script;
    }

    public void setScript(final Script script) {
        this.script = script;

    }

    /**
     * @return the startedWorkers, always increasing number
     */
    public int getStartedWorkers() {
        return startedWorkers;
    }

    /**
     * @return the target number of transactions per second
     */
    public double getTargetTPS() {
        return targetTPS;
    }

    /**
     * Set the target number of transactions per second
     *
     * @param targetTPS
     */
    public void setTargetTPS(final double targetTPS) {
        this.targetTPS = targetTPS;
    }

    public boolean isValid() {
        return condition.isValid();
    }

    public void sleep(final long interval) throws InterruptedException {
        condition.sleep(interval);
    }

    /**
     * the active number of workers, this number may go up and down when workers
     * are started and finished
     *
     * @return the active number of workers
     */
    public int getWorkerCount() {
        return workerCount.get();
    }

    public void stop() {
        condition.flip();

    }

}
