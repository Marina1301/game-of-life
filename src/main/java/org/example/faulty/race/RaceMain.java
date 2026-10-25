package org.example.faulty.race;

public class RaceMain {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Game of Life - FAULTY #1: data races & data consistency");
        RaceSimulation simulation = new RaceSimulation(40, 2000, 30L, 500L);
        simulation.start();

        for (int second = 1; second <= 6; second++) {
            Thread.sleep(1000L);
            System.out.println("[race] t=" + second + "s " + simulation.status());
        }

        simulation.stop();
        simulation.report();
    }
}
