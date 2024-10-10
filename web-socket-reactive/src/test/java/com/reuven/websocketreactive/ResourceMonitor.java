package com.reuven.websocketreactive;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ResourceMonitor {

    private static final Logger logger = LoggerFactory.getLogger(ResourceMonitor.class);

    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private static final com.sun.management.OperatingSystemMXBean osMXBean = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final StringBuilder sb = new StringBuilder("Resource Monitor:").append(System.lineSeparator());

    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public void startMonitoring() {
        executor.scheduleAtFixedRate(() -> {
            // Memory usage
            long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
            long freeMemory = maxMemory - usedMemory;

            double cpuLoad = osMXBean.getCpuLoad();
            sb.append(LocalDateTime.now())
                    .append(System.lineSeparator())
                    .append("CPU Load: ")
                    .append(String.format("%.2f", cpuLoad * 100))
                    .append("%")
                    .append(System.lineSeparator())
                    .append("Memory Used: ")
                    .append(usedMemory / (1024 * 1024))
                    .append(" MB")
                    .append(System.lineSeparator())
                    .append("Free Memory: ")
                    .append(freeMemory / (1024 * 1024))
                    .append(" MB")
                    .append(System.lineSeparator())
                    .append("-----------------------------------")
            ;
            System.out.println("CPU Load: " + String.format("%.2f", cpuLoad * 100) + "%");

            System.out.println("Memory Used: " + usedMemory / (1024 * 1024) + " MB");
            System.out.println("Free Memory: " + freeMemory / (1024 * 1024) + " MB");
            System.out.println("-----------------------------------");

        }, 0, 1, TimeUnit.SECONDS); // Samples every 1 second
    }

    public void stopMonitoring() {
        executor.shutdown();
        logger.info(sb.toString());
    }

    @PreDestroy
    public void destroy() {
        stopMonitoring();
    }

}

