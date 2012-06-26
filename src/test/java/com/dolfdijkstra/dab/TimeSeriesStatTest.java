package com.dolfdijkstra.dab;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeSeriesStatTest {

    @Test
    public void testAdd() {
        TimeSeriesStat s = new TimeSeriesStat(3);
        for (int i = 0; i < 4; i++) {
            s.add(i, i);
        }
        assertEquals(4, s.size());
    }

}
