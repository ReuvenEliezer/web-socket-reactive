package com.reuven.websocketreactive;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;

public class PerformanceMonitor {

    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final CentralProcessor processor;

    public PerformanceMonitor() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        SystemInfo si = new SystemInfo();
        this.processor = si.getHardware().getProcessor();
    }

    public void printMetrics() {
        long[] prevTick = processor.getSystemCpuLoadTicks();// שמור את הטיקים הקודמים
        double load = processor.getSystemCpuLoadBetweenTicks(prevTick);
        System.out.printf("CPU Load: %.2f%%\n", load); // אם load[0] הוא double, תוודא לא להמיר אותו ל-Long
        double cpuLoad = osBean.getSystemLoadAverage();
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();
        long usedHeapMemory = heapMemoryUsage.getUsed();

//        System.out.printf("CPU Load: %.2f, Used Heap Memory: %d bytes%n", cpuLoad, usedHeapMemory);
    }

    public void startMonitoring(long intervalMillis) {
        new Thread(() -> {
            while (true) {
                printMetrics();
                try {
                    Thread.sleep(intervalMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
}