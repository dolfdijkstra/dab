package com.dolfdijkstra.dab;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class QueueConsumer implements Runnable, ResultsCollector {

    private final AtomicBoolean condition = new AtomicBoolean(true);

    private final ResultsCollector delegate;
    private final BlockingQueue<RequestResult> queue = new LinkedBlockingQueue<RequestResult>();

    public QueueConsumer(final ResultsCollector delegate) {
        this.delegate = delegate;
    }

    @Override
    public void run() {
        while (condition.get()) {
            try {
                RequestResult line;
                while ((line = queue.poll(25, TimeUnit.MILLISECONDS)) != null) {
                    delegate.collect(line);
                }
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
        Util.closeSilent(delegate);
    }

    @Override
    public void collect(final RequestResult results) {
        queue.add(results);

    }

    @Override
    public void close() throws IOException {
        condition.set(false);

    }
}
