package com.dolfdijkstra.dab;

import java.net.URI;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

public class SingleUriScript implements Script {

    private final URI uri;

    public SingleUriScript(URI uri) {
        this.uri = uri;
    }

    @Override
    public HttpUriRequest next() {
        return new HttpGet(uri);
    }

    @Override
    public String getHost() {
        return uri.getHost();
    }

}
