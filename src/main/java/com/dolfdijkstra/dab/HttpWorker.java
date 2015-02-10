package com.dolfdijkstra.dab;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
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

    private final Iterable<ScriptItem> script;
    private final byte[] tmp = new byte[8 * 1024];

    /**
     * @param manager
     * @param client
     * @param conmanager
     * @param id
     */
    public HttpWorker(final WorkerManager manager, final CloseableHttpClient client,
            final HttpClientConnectionManager conmanager, final int id) {
        super();
        this.client = client;
        connectionManager = conmanager;
        collector = manager.getCollector();
        script = manager.getScript();
        this.id = id;
        this.manager = manager;

        startTime = manager.getStartTime();
        verbose = manager.getVerbose() > 5;
    }

    @Override
    public void run() {

        boolean stop = false;
        try {
            manager.workerStarted(id);
            final HttpClientContext context = createContext();
            Iterator<ScriptItem> i = script.iterator();
            while (manager.isValid() && stop == false && i.hasNext()) {
                final ScriptItem item = i.next();
                try {
                    stop = !executeItem(context, item);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                    stop = true;
                }
            }
        } finally {
            manager.workerFinished(id);
            try {
                client.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
            connectionManager.shutdown();
        }

    }

    private boolean executeItem(HttpClientContext context, ScriptItem item)
            throws InterruptedException {
        final HttpUriRequest request = item.request();
        final URI uri = request.getURI();

        HttpEntity entity = null;
        final RequestResult r = new RequestResult();
        r.url = uri.toASCIIString();
        r.method = request.getMethod();

        try {
            final long t = System.nanoTime();
            final long time = System.currentTimeMillis();
            final int concurrent = manager.startRequest();
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

            metrics.reset();
        } catch (final ClientProtocolException e) {
            r.exeption = e;
            request.abort();
            return false;
        } catch (final IOException e) {
            r.exeption = e;
            request.abort();
            return false;
        } finally {
            try {

                EntityUtils.consume(entity);
            } catch (final IOException e) {
                // ignore
            }
            manager.endRequest();
            collector.collect(r);
        }
        long interval = item.waitTime();
        if (interval > 0) {
            manager.sleep(interval);
        }
        return true;

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

        final HttpClientContext context = new HttpClientContext(
                new UnsafeHttpContext());
        context.setCookieStore(new BasicCookieStore());

        context.setAttribute(HttpClientContext.COOKIE_SPEC, new BrowserCompatSpec());
        return context;
    }

}

/**
 * A not thread-safe verion of HttpContext using a HashMap instead of a
 * ConcurrentHashMap
 * 
 * @author dolf
 *
 */
class UnsafeHttpContext implements HttpContext {

    private final Map<String, Object> map;

    public UnsafeHttpContext() {
        this.map = new HashMap<String, Object>();
    }

    public Object getAttribute(final String id) {
        Args.notNull(id, "Id");
        Object obj = this.map.get(id);
        return obj;
    }

    public void setAttribute(final String id, final Object obj) {
        Args.notNull(id, "Id");
        if (obj != null) {
            this.map.put(id, obj);
        } else {
            this.map.remove(id);
        }
    }

    public Object removeAttribute(final String id) {
        Args.notNull(id, "Id");
        return this.map.remove(id);
    }

    /**
     * @since 4.2
     */
    public void clear() {
        this.map.clear();
    }

    @Override
    public String toString() {
        return this.map.toString();
    }

}
