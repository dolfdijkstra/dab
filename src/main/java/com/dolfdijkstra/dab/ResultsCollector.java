package com.dolfdijkstra.dab;

import java.io.Closeable;

public interface ResultsCollector extends Closeable {

    void collect(RequestResult results);

}
