package com.dolfdijkstra.dab;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;

public class PerSecondRunnableTest {

    @Test
    public void testFormatSmall() {

        String s = String.format(Locale.US,
                "Throughput: %,8d, RT: %,6.0f %s, concurrency: %,7d, this machine load-avg: %,8.2f", 9, 12.9d, "μs",
                2, 0.7999d);
        //System.out.println(s);
        assertEquals("Throughput:        9, RT:     13 μs, concurrency:       2, this machine load-avg:     0.80",
                s);
    }

    @Test
    public void testFormatLarge() {

        String s = String.format(Locale.US,
                "Throughput: %,8d, RT: %,6.0f %s, concurrency: %,7d, this machine load-avg: %,8.2f", 99999,
                1200.9d, " s", 1500, 99.9d);
        //System.out.println(s);
        assertEquals("Throughput:   99,999, RT:  1,201  s, concurrency:   1,500, this machine load-avg:    99.90",
                s);

    }

}
