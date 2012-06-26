package com.dolfdijkstra.dab;

public interface Condition {

    boolean isValid();

    void sleep(long interval) throws InterruptedException;

}
