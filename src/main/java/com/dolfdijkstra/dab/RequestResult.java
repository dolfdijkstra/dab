package com.dolfdijkstra.dab;


/**
 * @author dolf
 *
 */
public class RequestResult {
    /**
     * timestamp, wall clock
     */
    long time;

    /**
     * time in ms elapsed since start of program
     */
    long relativeStart;
    /**
     * response time in micro seconds
     */
    long elapsed;
    /**
     * 
     */
    int status;
    /**
     * 
     */
    long length;
    /**
     * 
     */
    int id;
    /**
     * 
     */
    int concurrency;
    /**
     * 
     */
    long sendLength;
    /**
     * 
     */
    long receivedLength;
    /**
     * the URL
     */
    String url;

    /**
     * 
     */
    String method;

    public Exception exeption;

}
