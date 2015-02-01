package com.dolfdijkstra.dab.reporting;

import java.lang.management.ManagementFactory;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.MBeanServer;
import javax.management.ObjectName;

public class LoadMetrics {

    private final MBeanServer mbs;
    private final ObjectName beanName;

    private volatile long last;
    private volatile double systemLoadAverage;
    private volatile double systemCpuLoad;
    private volatile double processCpuLoad;
    public static final LoadMetrics INSTANCE = new LoadMetrics();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private LoadMetrics() {
        mbs = ManagementFactory.getPlatformMBeanServer();
        beanName = ManagementFactory.getOperatingSystemMXBean().getObjectName();
    }

    private void reload() {
        /*
        rwl.readLock().lock();
        if (!cacheValid) {
          // Must release read lock before acquiring write lock
          rwl.readLock().unlock();
          rwl.writeLock().lock();
          try {
            // Recheck state because another thread might have
            // acquired write lock and changed state before we did.
            if (!cacheValid) {
              data = ...
              cacheValid = true;
            }
            // Downgrade by acquiring read lock before releasing write lock
            rwl.readLock().lock();
          } finally {
            rwl.writeLock().unlock(); // Unlock write, still hold read
          }
        }

        try {
          use(data);
        } finally {
          rwl.readLock().unlock();
        }
        }*/
        // when SystemCpuLoad is read in fast succession it reports an incorrect
        // value.
        // read once as part of the periodic swap operation.

        if (System.nanoTime() >= last + 1E6) {
            systemLoadAverage = loadMetric("SystemLoadAverage");
            systemCpuLoad = loadMetric("SystemCpuLoad") * 100.0;
            processCpuLoad = loadMetric("ProcessCpuLoad") * 100.0;
            last = System.nanoTime();
        }
    }

    protected double loadMetric(final String metric) {
        try {

            double load = (double) mbs.getAttribute(beanName, metric);
            if (load < 0.0) {
                load = Double.NaN;
            }
            return load;
        } catch (final Exception e) {
            return Double.NaN;
        }

    }

    public double getSystemLoadAverage() {
        reload();
        return systemLoadAverage;
    }

    public double getSystemCpuLoad() {
        reload();
        return systemCpuLoad;
    }

    public double getProcessCpuLoad() {
        reload();
        return processCpuLoad;
    }

}
