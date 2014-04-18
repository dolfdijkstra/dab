package com.dolfdijkstra.dab;

import java.io.IOException;
import java.net.URI;
import java.util.Queue;

import org.apache.http.client.ClientProtocolException;

public class ResultsWriterCollector implements ResultsCollector {

    private final Queue<Object[]> queue;

    ResultsWriterCollector(Queue<Object[]> queue) throws Exception {
        this.queue = queue;
    }

    @Override
    public void exeption(ClientProtocolException e, HttpWorker httpWorker, URI uri) {

        queue.add(new Object[] { e });

    }

    @Override
    public void exeption(IOException e, HttpWorker httpWorker, URI uri) {
        queue.add(new Object[] { e });

    }

    @Override
    public void collect(Object... results) {
        queue.add(results);

    }
}
