package com.dolfdijkstra.dab;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

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
import org.apache.http.util.EntityUtils;

import com.dolfdijkstra.dab.Script.ScriptItem;

public class HttpWorker implements Runnable {
    private final CloseableHttpClient client;
    private final HttpClientConnectionManager connectionManager;

    private final ResultsCollector collector;

    private final int id;
    private final WorkerManager manager;

    private final boolean verbose;
    private final long startTime;

    private Script script;

    /**
     * @param manager
     * @param client
     * @param conmanager
     * @param id
     */
    public HttpWorker(final WorkerManager manager, final CloseableHttpClient client,
            HttpClientConnectionManager conmanager, final int id) {
        super();
        this.client = client;
        this.connectionManager = conmanager;
        this.collector = manager.getResults();
        this.script = manager.getScript();
        this.id = id;
        this.manager = manager;

        this.startTime = manager.getStartTime();
        this.verbose = manager.getVerbose() > 5;
    }

    @Override
    public void run() {

        boolean stop = false;
        try {
            manager.startWorker(id);
            final HttpClientContext context = createContext();
            while (manager.isValid() && stop == false) {
                ScriptItem item = script.next();
                final HttpUriRequest request = item.request();
                final URI uri = request.getURI();

                HttpEntity entity = null;
                final RequestResult r = new RequestResult();
                r.url = uri.toASCIIString();
                r.method = request.getMethod();

                try {
                    final long t = System.nanoTime();
                    final long time = System.currentTimeMillis();
                    int concurrent = manager.startRequest();
                    r.concurrency = concurrent;
                    r.time = time;
                    r.relativeStart = time - startTime;
                    r.id = id;

                    final HttpResponse response = client.execute(request, context);
                    r.status = response.getStatusLine().getStatusCode();

                    final HttpRequest req = context.getRequest();

                    final HttpConnection conn = context.getConnection();

                    final HttpConnectionMetrics metrics = conn.getMetrics();

                    if (verbose) {
                        print(req, response);
                    }
                    entity = response.getEntity();

                    r.length = entityLength(entity);
                    r.elapsed = (System.nanoTime() - t) / 1000; // in
                                                                // microseconds
                    r.sendLength = metrics.getSentBytesCount();
                    r.receivedLength = metrics.getReceivedBytesCount();

                } catch (final ClientProtocolException e) {
                    r.exeption = e;
                    request.abort();
                    stop = true;
                } catch (final IOException e) {
                    r.exeption = e;
                    request.abort();
                    stop = true;
                } finally {
                    try {
                        EntityUtils.consume(entity);
                    } catch (final IOException e) {
                        // ignore
                    }
                    manager.endRequest();
                    collector.collect(r);
                }
                try {
                    long interval = 0;
                    if (!stop && (interval = item.waitTime()) > 0) {
                        manager.sleep(interval);
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
            connectionManager.shutdown();
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

    private HttpClientContext createContext() {

        final HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());

        context.setAttribute(HttpClientContext.COOKIE_SPEC, new BrowserCompatSpec());
        return context;
    }

}
