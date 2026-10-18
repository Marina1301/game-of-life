package org.example;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class FaultyDataRaceSimulation {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private int foodBank = 100;

    public void runConcurrentUpdates() throws InterruptedException {
        Thread[] workers = new Thread[20];

        for (int i = 0; i < workers.length; i++) {
            final int workerId = i;
            workers[i] = new Thread(() -> {
                System.out.printf("[%s] Worker %d started%n", timestamp(), workerId);
                for (int j = 0; j < 5000; j++) {
                    foodBank++;
                    foodBank--;
                }
                System.out.printf("[%s] Worker %d finished%n", timestamp(), workerId);
            });
            workers[i].start();
        }

        for (Thread worker : workers) {
            worker.join();
        }
    }

    public int getFoodBank() {
        return foodBank;
    }

    public static void runDemo() throws InterruptedException {
        FaultyDataRaceSimulation demo = new FaultyDataRaceSimulation();
        System.out.println("[" + timestamp() + "] Starting faulty data-race simulation...");
        demo.runConcurrentUpdates();

        System.out.println("[" + timestamp() + "] Unsafe final food bank value: " + demo.getFoodBank());
        System.out.println("[" + timestamp() + "] Reason: shared variable is updated concurrently without synchronization, creating a data race.");
    }

    private static String timestamp() {
        return LocalTime.now().format(TIME_FORMATTER);
    }
}
