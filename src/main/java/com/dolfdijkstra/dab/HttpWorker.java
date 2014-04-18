package com.dolfdijkstra.dab;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.Header;
import org.apache.http.HttpConnection;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;

public class HttpWorker implements Runnable {
    private final CloseableHttpClient client;
    private final HttpClientConnectionManager connectionManager;
    private final Condition condition;

    private final ResultsCollector results;

    private final int id;
    private long interval = 0;
    private final WorkerManager manager;

    private AtomicInteger active = new AtomicInteger();

    private final boolean verbose;
    private final long startTime;

    private Script script;

    /**
     * @param manager
     * @param client
     * @param id identifier
     */
    public HttpWorker(final WorkerManager manager, final CloseableHttpClient client,
            HttpClientConnectionManager conmanager, final int id) {
        super();
        this.client = client;
        this.connectionManager = conmanager;
        this.condition = manager.getCondition();
        this.results = manager.getResults();
        this.script = manager.getScript();
        this.id = id;
        this.manager = manager;
        this.interval = manager.getInterval();
        this.active = manager.getActive();
        this.startTime = manager.getStartTime();
        this.verbose = manager.getVerbose() > 5;
    }

    @Override
    public void run() {
        final HttpContext context = createContext();

        manager.startWorker(id);
        boolean stop = false;
        try {
            while (condition.isValid() && stop == false) {

                final HttpUriRequest request = script.next();
                final URI uri = request.getURI();
                final String url = uri.toASCIIString();
                HttpEntity entity = null;
                try {
                    final long t = System.nanoTime();
                    final long time = System.currentTimeMillis();
                    int concurrent = active.incrementAndGet();
                    final HttpResponse response = client.execute(request, context);
                    final HttpRequest req = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
                    final HttpConnection conn = (HttpConnection) context.getAttribute(HttpCoreContext.HTTP_CONNECTION);
                    final HttpConnectionMetrics metrics = conn.getMetrics();

                    if (verbose) {
                        print(req, response);
                    }
                    entity = response.getEntity();
                    final long bytes = entityLength(entity);

                    final long sent = metrics.getSentBytesCount();
                    final long received = metrics.getReceivedBytesCount();

                    final long elapsed = (System.nanoTime() - t) / 1000; // in
                    // microseconds

                    results.collect(time, (time - startTime), elapsed, response.getStatusLine().getStatusCode(), bytes,
                            id, concurrent, sent, received, url);
                } catch (final ClientProtocolException e) {
                    results.exeption(e, this, uri);
                    request.abort();
                    stop=true;
                } catch (final IOException e) {
                    results.exeption(e, this, uri);
                    request.abort();
                    stop=true;
                } finally {
                    try {
                        EntityUtils.consume(entity);
                    } catch (final IOException e) {
                        // ignore
                    }
                    active.decrementAndGet();
                }
                try {
                    if (interval > 0) {
                        condition.sleep(interval);
                    }
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        } finally {
            manager.finishWorker(id);
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connectionManager.shutdown();// .closeIdleConnections(-5,
                                         // TimeUnit.SECONDS);
        }

    }

    public long entityLength(final HttpEntity entity) throws IOException {
        if (entity == null) {
            throw new IllegalArgumentException("HTTP entity may not be null");
        }
        final InputStream instream = entity.getContent();
        if (instream == null) {
            return -1;
        }
        try {
            int i = 0;
            final byte[] tmp = new byte[8 * 1024];
            int l;
            while ((l = instream.read(tmp)) != -1) {
                i = i + l;
            }
            return i;
        } finally {
            instream.close();
        }
    }

    private void print(final HttpRequest request, final HttpResponse response) {
        System.out.println(">---");
        for (final Header h : request.getAllHeaders()) {
            System.out.println(h.toString());
        }
        System.out.println("<---");
        for (final Header h : response.getAllHeaders()) {
            System.out.println(h.toString());
        }

    }

    private HttpContext createContext() {
        final BasicHttpContext context = new BasicHttpContext();
        final BasicCookieStore store = new BasicCookieStore();
        context.setAttribute(HttpClientContext.COOKIE_STORE, store);
        context.setAttribute(HttpClientContext.COOKIE_SPEC, new BrowserCompatSpec());
        return context;
    }

}
