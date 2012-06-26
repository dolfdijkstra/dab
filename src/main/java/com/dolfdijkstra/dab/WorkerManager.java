package com.dolfdijkstra.dab;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;

public class WorkerManager {
    private AtomicInteger counter = new AtomicInteger();
    private CountDownLatch latch = new CountDownLatch(1);

    private int workers = 1;
    private int rampup = 0;
    private int runtime = 5;

    private BooleanCondition condition;

    private ResultsCollector results;

    private AtomicInteger active = new AtomicInteger();

    private URI uri;
    private long interval;
    private long startTime;
    private int verbose;

    HttpClient createClient() {
        HttpParams params = new BasicHttpParams();
        params.setParameter(HttpProtocolParams.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        params.setBooleanParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
        params.setBooleanParameter(HttpConnectionParams.STALE_CONNECTION_CHECK, false);
        params.setIntParameter(HttpConnectionParams.SOCKET_BUFFER_SIZE, 8 * 1024);
        params.setIntParameter(HttpConnectionParams.SO_TIMEOUT, 15000);

        OperatingSystemMXBean o = ManagementFactory.getOperatingSystemMXBean();
        HttpProtocolParams.setUserAgent(params, "dab/1.0 (" + o.getName() + "; " + o.getVersion() + "; " + o.getArch()
                + ")");

        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

        SingleClientConnManager conman = new SingleClientConnManager(schemeRegistry);
        DefaultHttpClient client = new DefaultHttpClient(conman, params);

        client.setHttpRequestRetryHandler(new HttpRequestRetryHandler() {

            public boolean retryRequest(final IOException exception, int executionCount, final HttpContext context) {
                return false;
            }

        });
        return client;
    }

    void work() throws Exception {
        this.startTime = System.currentTimeMillis();
        condition = new BooleanCondition();

        int pace = workers == 1 ? 0 : (rampup * 1000) / (workers - 1);
        long start = System.currentTimeMillis();
        long end = start + (runtime * 1000L);
        for (int i = 0; i < workers; i++) {
            if (i > 0 && pace > 0) {
                Thread.sleep(pace);
            }
            HttpWorker worker = new HttpWorker(this, createClient(), i);
            Thread t = new Thread(worker, "HttpWorker-" + i);
            t.start();
            System.out.println("Started " + t.getName());

        }
        // System.out.println("Started " + (System.currentTimeMillis() -
        // start));
        while (System.currentTimeMillis() < end) {
            Thread.sleep(250);
        }
        condition.flip();
        latch.await();
        System.out.println("All workers finished at " + (System.currentTimeMillis() - start));

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
     * @return the uri
     */
    public URI getUri() {
        return uri;
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
    public int getWorkers() {
        return workers;
    }

    /**
     * @param workers the workers to set
     */
    public void setWorkers(int workers) {
        this.workers = Math.max(0, workers);
    }

    /**
     * @return the rampup
     */
    public int getRampup() {
        return rampup;
    }

    /**
     * @param rampup the rampup to set
     */
    public void setRampup(int rampup) {
        this.rampup = Math.max(0, rampup);
    }

    /**
     * @return the runtime
     */
    public int getRuntime() {
        return runtime;
    }

    /**
     * @param runtime the runtime to set
     */
    public void setRuntime(int runtime) {
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
     * @param uri the uri to set
     */
    public void setUri(URI uri) {
        this.uri = uri;
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
}
