package org.example;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Game of Life");
        runSimulation();
    }

    private static void runSimulation() throws InterruptedException {
        Simulation simulation = new Simulation(8, 400, 800L, 1500L);
        simulation.start();

        Thread.sleep(80000L);

        simulation.stop();

        System.out.println("Simulation stopped. Alive cells remaining: " + simulation.getAliveCellCount()
                + ", food available: " + simulation.getFoodAvailable());
    }
}
