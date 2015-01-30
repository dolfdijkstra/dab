package com.dolfdijkstra.dab;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BooleanCondition implements Condition {
    final AtomicBoolean bool = new AtomicBoolean(true);
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public boolean isValid() {
        return bool.get();
    }

    @Override
    public void sleep(long interval) throws InterruptedException {
        if (interval > 0)
            latch.await(interval, TimeUnit.MILLISECONDS);

    }

    public void flip() {
        if (isValid()) {
            bool.set(false);
            latch.countDown();
        }
    }

}
