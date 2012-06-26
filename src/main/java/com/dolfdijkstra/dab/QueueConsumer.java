package com.dolfdijkstra.dab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang.time.FastDateFormat;

final class QueueConsumer implements Runnable {

    private AtomicBoolean condition = new AtomicBoolean(true);
    private FastDateFormat df = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.S");
    private final PrintWriter writer;
    private StatisticsCollector col;
    private final BlockingQueue<Object[]> queue;

    public QueueConsumer(BlockingQueue<Object[]> queue, StatisticsCollector col) {
        writer = null;
        this.col = col;
        this.queue = queue;
    }

    public QueueConsumer(File file, BlockingQueue<Object[]> queue, StatisticsCollector col) throws IOException {
        this.col = col;
        this.queue = queue;
        FileOutputStream fos = new FileOutputStream(file);
        GZIPOutputStream dos = new GZIPOutputStream(fos);

        writer = new PrintWriter(new OutputStreamWriter(dos, Charset.forName("UTF-8")));
    }

    @Override
    public void run() {
        if (writer != null) {
            writer.println("time,t0,elapsed,status,content-length,id,concurrency,sent,received,url");
            writer.flush();
        }
        while (condition.get()) {
            try {
                Object[] line;
                while ((line = queue.poll(25, TimeUnit.MILLISECONDS)) != null) {
                    col.addToStats(line);
                    if (writer != null)
                        writer.println(format(line));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (writer != null) {
            writer.flush();
            writer.close();
        }
    }

    public void stop() {
        condition.set(false);
    }

    String format(Object[] result) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < result.length; i++) {
            if (i > 0)
                b.append(',');
            if (i == 0) {
                b.append(df.format((Long) result[i]));
            } else {
                b.append(result[i]);
            }
        }
        return b.toString();
    }

}
