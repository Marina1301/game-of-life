package org.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FaultyDeadlockSimulation {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Object left = new Object();
    private final Object right = new Object();

    public void runDeadlockScenario() throws InterruptedException {
        Thread thread1 = new Thread(() -> {
            System.out.printf("[%s] Thread 1 trying to lock LEFT%n", timestamp());
            synchronized (left) {
                System.out.printf("[%s] Thread 1 acquired LEFT%n", timestamp());
                sleepQuietly(200);
                System.out.printf("[%s] Thread 1 waiting for RIGHT%n", timestamp());
                synchronized (right) {
                    System.out.printf("[%s] Thread 1 acquired RIGHT%n", timestamp());
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            System.out.printf("[%s] Thread 2 trying to lock RIGHT%n", timestamp());
            synchronized (right) {
                System.out.printf("[%s] Thread 2 acquired RIGHT%n", timestamp());
                sleepQuietly(200);
                System.out.printf("[%s] Thread 2 waiting for LEFT%n", timestamp());
                synchronized (left) {
                    System.out.printf("[%s] Thread 2 acquired LEFT%n", timestamp());
                }
            }
        });

        thread1.setDaemon(true);
        thread2.setDaemon(true);

        thread1.start();
        thread2.start();

        thread1.join(3000L);
        thread2.join(3000L);

        if (thread1.isAlive() || thread2.isAlive()) {
            System.out.printf("[%s] Deadlock detected: threads are blocked waiting for each other's locks.%n", timestamp());
        } else {
            System.out.printf("[%s] The faulty scenario completed without deadlock (rare on fast machines).%n", timestamp());
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void runDemo() throws InterruptedException {
        FaultyDeadlockSimulation demo = new FaultyDeadlockSimulation();
        System.out.println("[" + timestamp() + "] Starting faulty deadlock simulation...");
        demo.runDeadlockScenario();
        System.out.println("[" + timestamp() + "] Reason: two threads acquire locks in opposite order, leading to a classic deadlock.");
    }

    private static String timestamp() {
        return LocalTime.now().format(TIME_FORMATTER);
    }
}
