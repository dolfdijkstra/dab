package com.dolfdijkstra.dab;

import org.apache.http.client.methods.HttpUriRequest;

public interface Script {

    HttpUriRequest next();
    
    long waitTime();

}
