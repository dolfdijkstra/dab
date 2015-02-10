package com.dolfdijkstra.dab;

import org.apache.http.client.methods.HttpUriRequest;

public interface Script extends Iterable<Script.ScriptItem> {

    public interface ScriptItem {
        long waitTime();

        HttpUriRequest request();
    }

}
