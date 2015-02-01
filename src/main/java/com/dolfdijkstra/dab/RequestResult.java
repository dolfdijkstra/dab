package com.dolfdijkstra.dab;

/**
 * @author dolf
 *
 */
public class RequestResult {
    /**
     * timestamp, wall clock
     */
    public long time;

    /**
     * time in ms elapsed since start of program
     */
    public long relativeStart;
    /**
     * response time in micro seconds
     */
    public long elapsed;
    /**
     *
     */
    public int status;
    /**
     *
     */
    public long length;
    /**
     *
     */
    public int id;
    /**
     *
     */
    public int concurrency;
    /**
     *
     */
    public long sendLength;
    /**
     *
     */
    public long receivedLength;
    /**
     * the URL
     */
    public String url;

    /**
     *
     */
    public String method;

    public Exception exeption;

}
