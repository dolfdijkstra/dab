package com.dolfdijkstra.dab;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;

public class MultipleUriScript implements Script {

    private final HttpUriRequest[] uris;
    private final int waitTime;

    public MultipleUriScript(List<URI> s, int interval) {
        uris = s.stream().map(u -> build(u))
                .toArray(size -> new HttpUriRequest[size]);
        waitTime = Math.max(0, interval);
    }

    protected HttpUriRequest build(URI u) {
        HttpGet g = new HttpGet(u);
        g.addHeader("Accept", accept());
        g.addHeader("Accept-Encoding ", encoding());

        return g;
    }

    protected String encoding() {
        return "gzip, deflate";
    }

    protected String accept() {
        // "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        return "text/html";
    }

    @Override
    public HttpUriRequest next() {

        int n = uris.length == 1 ? 0 : ThreadLocalRandom.current().nextInt(
                uris.length);
        return RequestBuilder.copy(uris[n]).build();

    }

    @Override
    public long waitTime() {
        return waitTime;
    }

}
