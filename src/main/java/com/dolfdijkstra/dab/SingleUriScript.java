package com.dolfdijkstra.dab;

import java.net.URI;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

public class SingleUriScript implements Script {

    private final URI uri;
    private final int waitTime;

    public SingleUriScript(URI uri, int interval) {
        this.uri = uri;
        this.waitTime = Math.max(0, interval);
    }

    @Override
    public HttpUriRequest next() {
        return new HttpGet(uri);
    }

    @Override
    public long waitTime() {
        return waitTime;
    }

}
