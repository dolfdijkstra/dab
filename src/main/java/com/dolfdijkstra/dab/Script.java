package com.dolfdijkstra.dab;

import org.apache.http.client.methods.HttpUriRequest;

public interface Script {

    public interface ScriptItem {
        long waitTime();

        HttpUriRequest request();
    }

    ScriptItem next();

}
