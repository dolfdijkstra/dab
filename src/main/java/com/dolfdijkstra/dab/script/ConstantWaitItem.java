package com.dolfdijkstra.dab.script;

import org.apache.http.client.methods.HttpUriRequest;

import com.dolfdijkstra.dab.Script.ScriptItem;

public class ConstantWaitItem implements ScriptItem {

    private final long waitTime;
    private final HttpUriRequest request;

    public ConstantWaitItem(final HttpUriRequest request, long waitTime) {
        this.request = request;
        this.waitTime = waitTime;
    }

    @Override
    public long waitTime() {
        return waitTime;
    }

    @Override
    public HttpUriRequest request() {
        return request;
    }

}
