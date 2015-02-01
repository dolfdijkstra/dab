package com.dolfdijkstra.dab.script;

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

import com.dolfdijkstra.dab.Script;

public class FileScript implements Script {

    private final AtomicInteger i = new AtomicInteger();
    private final HttpUriRequest[] uris;
    private final int waitTime;

    public FileScript(final File f, final int interval) {
        waitTime = interval;
        BufferedReader r = null;
        final LinkedList<URI> list = new LinkedList<URI>();
        try {
            r = new BufferedReader(new FileReader(f));
            String s;
            while ((s = r.readLine()) != null) {
                if (StringUtils.isNotBlank(s)) {
                    try {
                        final URI u = new URI(s);
                        if (StringUtils.isNotBlank(u.getHost())) {
                            list.add(u);
                        }
                    } catch (final URISyntaxException e) {
                        System.err.println("line '" + s + "' is not a URI");
                    }
                }

            }
        } catch (final IOException e) {
            e.printStackTrace();
        } finally {
            try {
                r.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        uris = new HttpUriRequest[list.size()];
        for (int i = 0; i < uris.length; i++) {
            uris[i] = new HttpGet(list.pollFirst());
            uris[i].addHeader("Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            uris[i].addHeader("Accept-Encoding ", "gzip, deflate");
        }

        if (uris.length == 0) {
            throw new IllegalArgumentException("The file" + f.getAbsolutePath()
                    + " does not contain any URIs");
        }

    }

    @Override
    public ScriptItem next() {
        final int j = i.getAndIncrement() % uris.length;
        return new ConstantWaitItem(RequestBuilder.copy(uris[j]).build(), waitTime);
    }

}
