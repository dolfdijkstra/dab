package com.dolfdijkstra.dab.reporting;

import static org.junit.Assert.*;

import org.junit.Test;

public class LoadMetricsTest {

    @Test
    public void read_multiple() {
        double v1 = LoadMetrics.INSTANCE.getSystemCpuLoad();
        for (int i = 0; i < 100; i++) {
            double v2 = LoadMetrics.INSTANCE.getSystemCpuLoad();
            assertEquals(v1, v2, 0.000001);
        }

    }

}
