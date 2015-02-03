package com.dolfdijkstra.dab.reporting;

import java.lang.management.ManagementFactory;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;

public class LoadMetrics {

    private static final String PROCESS_CPU_LOAD = "ProcessCpuLoad";
    private static final String SYSTEM_CPU_LOAD = "SystemCpuLoad";
    private static final String SYSTEM_LOAD_AVERAGE = "SystemLoadAverage";
    private static final String[] METRICS = new String[] { SYSTEM_LOAD_AVERAGE,
            SYSTEM_CPU_LOAD, PROCESS_CPU_LOAD };
    private final MBeanServer mbs;
    private final ObjectName beanName;

    private volatile long last = Long.MIN_VALUE;
    private volatile double systemLoadAverage;
    private volatile double systemCpuLoad;
    private volatile double processCpuLoad;
    public static final LoadMetrics INSTANCE = new LoadMetrics();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private LoadMetrics() {
        mbs = ManagementFactory.getPlatformMBeanServer();
        beanName = ManagementFactory.getOperatingSystemMXBean().getObjectName();
    }

    private void reload() {
        // when SystemCpuLoad is read in fast succession it reports an incorrect
        // value.
        // read once as part of the periodic swap operation.

        rwl.readLock().lock();
        try {
            if (isStale()) {
                // Must release read lock before acquiring write lock

                rwl.readLock().unlock();
                rwl.writeLock().lock();
                try {
                    // Recheck state because another thread might have
                    // acquired write lock and changed state before we did.
                    if (isStale()) {
                        loadMetrics();
                        last = System.nanoTime();
                    }
                    // Downgrade by acquiring read lock before releasing write
                    // lock
                    rwl.readLock().lock();
                } finally {
                    rwl.writeLock().unlock(); // Unlock write, still hold read
                }
            }

        } finally {
            rwl.readLock().unlock();
        }

    }

    /**
     * @return
     */
    boolean isStale() {
        return System.nanoTime() >= last + 1E6;
    }

    private void loadMetrics() {
        try {

            AttributeList list = mbs.getAttributes(beanName, METRICS);
            for (Attribute item : list.asList()) {
                String name = item.getName();
                if (SYSTEM_LOAD_AVERAGE.equals(name)) {
                    systemLoadAverage = asDouble(item.getValue());
                }
                if (SYSTEM_CPU_LOAD.equals(name)) {
                    systemCpuLoad = asDouble(item.getValue()) * 100.0;
                }
                if (PROCESS_CPU_LOAD.equals(name)) {
                    processCpuLoad = asDouble(item.getValue()) * 100.0;
                }

            }

        } catch (final Exception e) {
            e.printStackTrace();
        }

    }

    private double asDouble(Object v) {
        if (v instanceof Double) {
            double load = (double) v;
            if (load < 0.0) {
                return Double.NaN;
            }
            return load;
        }
        return Double.NaN;
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
