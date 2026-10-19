package org.example;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Game of Life ");
        runWorkingSolution();
    }

    private static void runWorkingSolution() throws InterruptedException {
        Simulation simulation = new Simulation(8, 20, 1800L);
        simulation.start();

        Thread.sleep(6000L);
        simulation.stop();
    }
}

