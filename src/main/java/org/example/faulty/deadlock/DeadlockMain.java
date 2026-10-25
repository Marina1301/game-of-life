package org.example.faulty.deadlock;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class DeadlockMain {
    private static final long RUN_MS = 6000L;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Game of Life");
        DeadlockSimulation simulation = new DeadlockSimulation(4, 1000, 100L, 1500L);
        simulation.start();

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long end = System.currentTimeMillis() + RUN_MS;
        boolean detected = false;
        while (System.currentTimeMillis() < end && !detected) {
            Thread.sleep(200L);
            long[] ids = threads.findDeadlockedThreads();
            if (ids != null) {
                detected = true;
                System.out.println("[deadlock] Detected by ThreadMXBean.findDeadlockedThreads():");
                for (ThreadInfo info : threads.getThreadInfo(ids)) {
                    System.out.printf("  %s is %s waiting for %s held by %s%n",
                            info.getThreadName(), info.getThreadState(), info.getLockName(),
                            info.getLockOwnerName());
                }
                System.out.println("[deadlock] " + simulation.status());
            }
        }
        if (!detected) {
            System.out.println("[deadlock] No deadlock this run (timing-dependent); " + simulation.status());
        }

        simulation.stop();
        simulation.report();
    }
}
