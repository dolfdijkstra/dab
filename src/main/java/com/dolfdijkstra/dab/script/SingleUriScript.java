package com.dolfdijkstra.dab.script;

import java.net.URI;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import com.dolfdijkstra.dab.Script;

public class SingleUriScript implements Script {

    private final ScriptItem item;

    public SingleUriScript(final URI uri, final int interval) {

        final int waitTime = Math.max(0, interval);
        item = new ScriptItem() {

            @Override
            public long waitTime() {
                return waitTime;
            }

            @Override
            public HttpUriRequest request() {
                return new HttpGet(uri);
            }
        };
    }

    @Override
    public ScriptItem next() {
        return item;
    }

}
