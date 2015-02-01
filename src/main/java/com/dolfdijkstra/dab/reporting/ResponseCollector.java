package com.dolfdijkstra.dab.reporting;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.time.FastDateFormat;

import com.dolfdijkstra.dab.RequestResult;
import com.dolfdijkstra.dab.ResultsCollector;
import com.dolfdijkstra.dab.Util;

public final class ResponseCollector implements Closeable, ResultsCollector {

    private final AtomicBoolean condition = new AtomicBoolean(true);
    private final FastDateFormat df = FastDateFormat
            .getInstance("yyyy-MM-dd'T'HH:mm:ss.S");
    private final PrintWriter writer;
    private final ResultsCollector delegate;

    public ResponseCollector(final File file, final ResultsCollector delegate)
            throws IOException {
        this.delegate = delegate;

        final FileOutputStream fos = new FileOutputStream(file);
        final GZIPOutputStream dos = new GZIPOutputStream(fos);

        writer = new PrintWriter(new OutputStreamWriter(dos,
                Charset.forName("UTF-8")));
        writer.println("time,t0,elapsed,status,content-length,id,concurrency,sent,received,url");
        writer.flush();

    }

    private String format(final RequestResult result) {
        final StringBuilder b = new StringBuilder(1024);
        b.append(df.format(result.time)).append(',');
        b.append(result.relativeStart).append(',');
        b.append(result.elapsed).append(',');
        b.append(result.status).append(',');
        b.append(result.length).append(',');
        b.append(result.id).append(',');
        b.append(result.concurrency).append(',');
        b.append(result.sendLength).append(',');
        b.append(result.receivedLength).append(',');
        b.append(result.url).append(',');
        if (result.exeption != null) {
            b.append(result.exeption.getClass().getName());
        }
        return b.toString();
    }

    @Override
    public void collect(final RequestResult results) {
        if (condition.get()) {
            writer.println(format(results));
        }
        delegate.collect(results);
    }

    @Override
    public void close() throws IOException {
        condition.set(false);
        writer.close();
        Util.closeSilent(delegate);

    }

}
