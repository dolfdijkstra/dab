package com.dolfdijkstra.dab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;

public class FileScript implements Script {

    private final AtomicInteger i = new AtomicInteger();
    private final HttpUriRequest[] uris;
    private final int waitTime;

    public FileScript(File f, int interval) {
        this.waitTime = interval;
        BufferedReader r = null;
        LinkedList<URI> list = new LinkedList<URI>();
        try {
            r = new BufferedReader(new FileReader(f));
            String s;
            while ((s = r.readLine()) != null) {
                if (StringUtils.isNotBlank(s)) {
                    try {
                        URI u = new URI(s);
                        if (StringUtils.isNotBlank(u.getHost())) {
                            list.add(u);
                        }
                    } catch (URISyntaxException e) {
                        System.err.println("line '" + s + "' is not a URI");
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                r.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        uris = new HttpUriRequest[list.size()];
        for (int i = 0; i < uris.length; i++) {
            uris[i] = new HttpGet(list.pollFirst());
            uris[i].addHeader("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            uris[i].addHeader("Accept-Encoding ", "gzip, deflate");
            uris[i].addHeader("Accept-Language", "en-us");
        }

        if (uris.length == 0)
            throw new IllegalArgumentException("The file" + f.getAbsolutePath()
                    + " does not contain any URIs");

    }

    @Override
    public HttpUriRequest next() {
        int j = i.getAndIncrement() % uris.length;
        return RequestBuilder.copy(uris[j]).build();
    }

    @Override
    public long waitTime() {
        return waitTime;
    }

}
