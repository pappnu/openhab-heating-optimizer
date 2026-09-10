package openhab.heating.optimizer.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

@NonNullByDefault
public class ContinuousPeriodOptimizerActionHandlerTest {
    @Test
    public void testContinuousPeriodOptimization() {
        double[] prices = new double[] { 9, 9, 4, 4, 5 };
        var result = ContinuousPeriodOptimizerActionHandler.findCheapestInterval(prices, 3);
        assertArrayEquals(new double[] { 0, 0, 1, 1, 1 }, result);
    }

    @Test
    public void testContinuousPeriodOptimizationWithEmptyArray() {
        double[] prices = new double[0];
        var result = ContinuousPeriodOptimizerActionHandler.findCheapestInterval(prices, 3);
        assertArrayEquals(prices, result);
    }

    @Test
    public void testContinuousPeriodOptimizationWithTooLongPeriod() {
        double[] prices = new double[] { 9 };
        var result = ContinuousPeriodOptimizerActionHandler.findCheapestInterval(prices, 2);
        assertArrayEquals(new double[] { 1 }, result);
    }

    @Test
    public void testContinuousPeriodOptimizationWithNegativePeriod() {
        double[] prices = new double[] { 9 };
        var result = ContinuousPeriodOptimizerActionHandler.findCheapestInterval(prices, -1);
        assertArrayEquals(new double[] { 0 }, result);
    }
}
