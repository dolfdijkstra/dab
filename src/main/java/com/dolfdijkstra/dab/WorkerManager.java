package com.dolfdijkstra.dab;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

public class WorkerManager {
    private AtomicInteger counter = new AtomicInteger();
    private CountDownLatch latch = new CountDownLatch(1);

    private int workers = 1;
    private int rampup = 0;
    private int runtime = 5;
    private int startedWorkers = 0;

    private BooleanCondition condition;

    private ResultsCollector results;

    private AtomicInteger active = new AtomicInteger();

    private Script script;
    private long interval;
    private long startTime;
    private int verbose;

    CloseableHttpClient createClient(HttpClientConnectionManager cm) {
        org.apache.http.client.config.RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();

        requestConfigBuilder.setStaleConnectionCheckEnabled(false);
        requestConfigBuilder.setConnectionRequestTimeout(1000); // time to wait for a connection from the pool
        requestConfigBuilder.setConnectTimeout(1000); // fail fast on a socket connection attempt
        requestConfigBuilder.setSocketTimeout(15000);
        RequestConfig requestConfig = requestConfigBuilder.build();

        OperatingSystemMXBean o = ManagementFactory.getOperatingSystemMXBean();

        HttpClientBuilder builder = HttpClients.custom().setConnectionManager(cm);
        builder.disableAutomaticRetries();
        builder.setDefaultRequestConfig(requestConfig);

        builder.setUserAgent("dab/1.0 (" + o.getName() + "; " + o.getVersion() + "; " + o.getArch() + ")");
        CloseableHttpClient client = builder.build();

        return client;
    }

    void work() throws Exception {
        this.startTime = System.currentTimeMillis();
        condition = new BooleanCondition();

        int pace = workers == 1 ? 0 : (rampup * 1000) / (workers - 1);
        long start = System.currentTimeMillis();
        long end = start + (runtime * 1000L);
        for (startedWorkers = 0; startedWorkers < workers;) {
            if (startedWorkers > 0 && pace > 0) {
                Thread.sleep(pace);
            }
            HttpClientConnectionManager cm = createHttpClientConnectionManager();
            HttpWorker worker = new HttpWorker(this, createClient(cm), cm, startedWorkers);
            Thread t = new Thread(worker, "HttpWorker-" + startedWorkers);
            t.start();
            startedWorkers++;
            // System.out.println("Started " + t.getName());

        }
        while (System.currentTimeMillis() < end) {
            Thread.sleep(250);
        }
        condition.flip();
        latch.await();
        System.out.println("\nAll workers finished at " + (System.currentTimeMillis() - start));

    }

    private HttpClientConnectionManager createHttpClientConnectionManager() {
        BasicHttpClientConnectionManager conman = new BasicHttpClientConnectionManager();

        //SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(1000).build();
        //conman.setSocketConfig(socketConfig);

        return conman;
    }

    public void startWorker(int id) {
        counter.incrementAndGet();

    }

    public void finishWorker(int id) {
        if (counter.decrementAndGet() == 0)
            latch.countDown();
    }

    /**
     * @return the condition
     */
    public BooleanCondition getCondition() {
        return condition;
    }

    /**
     * @return the results
     */
    public ResultsCollector getResults() {
        return results;
    }

    /**
     * @return the active
     */
    public AtomicInteger getActive() {
        return active;
    }

    /**
     * @return the interval
     */
    public long getInterval() {
        return interval;
    }

    /**
     * @return the workers
     */
    public int getWorkerNumber() {
        return workers;
    }

    /**
     * @param workers the workers to set
     */
    public void setWorkerNumber(int workers) {
        this.workers = Math.max(0, workers);
    }

    /**
     * @return the rampup
     */
    public int getRampupTime() {
        return rampup;
    }

    /**
     * @param rampup the rampup to set
     */
    public void setRampupTime(int rampup) {
        this.rampup = Math.max(0, rampup);
    }

    /**
     * @return the runtime
     */
    public int getTestRuntime() {
        return runtime;
    }

    /**
     * @param runtime the runtime to set
     */
    public void setTestRuntime(int runtime) {
        this.runtime = Math.max(0, runtime);
    }

    /**
     * @param condition the condition to set
     */
    public void setCondition(BooleanCondition condition) {
        this.condition = condition;
    }

    /**
     * @param results the results to set
     */
    public void setResults(ResultsCollector results) {
        this.results = results;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(long interval) {
        this.interval = Math.max(0, interval);
    }

    /**
     * @return the startTime
     */
    public long getStartTime() {
        return startTime;
    }

    public void setVerbose(int verbose) {
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

    public void setScript(Script script) {
        this.script = script;

    }

    /**
     * @return the startedWorkers
     */
    public int getStartedWorkers() {
        return startedWorkers;
    }
}
