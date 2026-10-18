package org.example;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Stage 1 - Java Concurrency Lab ===");
        System.out.println("1) Correct solution: Game of Life simulation");
        runWorkingSolution();

        System.out.println("\n2) Faulty solution A: data race / consistency issue");
        FaultyDataRaceSimulation.runDemo();

        System.out.println("\n3) Faulty solution B: deadlock / livelock demonstration");
        FaultyDeadlockSimulation.runDemo();
    }

    private static void runWorkingSolution() throws InterruptedException {
        Simulation simulation = new Simulation(8, 20, 1200L, 1800L);
        simulation.start();

        Thread.sleep(6000L);
        simulation.stop();
        System.out.println("Correct simulation terminated.");
    }
}

