package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodResourceTest {
    @Test
    void consumeOne_shouldDecrementAndReturnTrue() {
        FoodResource resource = new FoodResource(3);

        assertTrue(resource.consumeOne());
        assertEquals(2, resource.getAvailableFood());
    }

    @Test
    void consumeOne_shouldReturnFalseWhenEmpty() {
        FoodResource resource = new FoodResource(0);

        assertFalse(resource.consumeOne());
        assertEquals(0, resource.getAvailableFood());
    }

    @Test
    void generateFoodOnStarvation_shouldReturnAmountBetweenOneAndFive() {
        Simulation simulation = new Simulation(0, 10, 1000, 200);
        Cell cell = new Cell(simulation, 1, CellType.ASEXUAL, 200);

        int foodAmount = cell.generateFoodOnStarvation();

        assertTrue(foodAmount >= 1 && foodAmount <= 5);
    }
}
