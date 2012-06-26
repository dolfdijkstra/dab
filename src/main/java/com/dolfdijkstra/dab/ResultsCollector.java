package com.dolfdijkstra.dab;

import java.io.IOException;
import java.net.URI;

import org.apache.http.client.ClientProtocolException;

public interface ResultsCollector {

    void exeption(ClientProtocolException e, HttpWorker httpWorker, URI uri);

    void exeption(IOException e, HttpWorker httpWorker, URI uri);

    void collect(Object...results);


}
