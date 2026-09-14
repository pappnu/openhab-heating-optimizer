package openhab.heating.optimizer.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

@NonNullByDefault
public class HeatingOptimizerActionHandlerTest {
    protected static final long SOLVING_TIME_LIMIT = 10000;

    @Test
    public void testHeatingContinuation() {
        double[] prices = { 2, 10, 10, 10, 1, -1 };
        var heating = testSuccessfulHeatingOptimization(prices, 3, prices.length, prices.length, -1, -1, 2,
                prices.length, prices.length, 3, true);
        assertArrayEquals(new double[] { 1, 0, 0, 0, 1, 1 }, heating, 0.000001);
    }

    @Test
    public void testHeatingNoncontinuation() {
        double[] prices = { 2, 10, 10, 10, 1, -1 };
        var heating = testSuccessfulHeatingOptimization(prices, 3, prices.length, prices.length, -1, -1, 2,
                prices.length, prices.length, 3, false);
        assertArrayEquals(new double[] { 0, 0, 0, 1, 1, 1 }, heating, 0.000001);
    }

    @Test
    public void testHeatingPeriods() {
        double[] prices = { -1, 10, 10, 10, 10, -2, -2, -2 };
        var heating = testSuccessfulHeatingOptimization(prices, 4, prices.length, prices.length, -1, -1, 3,
                prices.length, 4, 2, false);
        assertArrayEquals(new double[] { 1, 1, 1, 0, 0, 1, 1, 1 }, heating, 0.000001);
    }

    @Test
    public void testHeatingStartsSinglePeriod() {
        double[] prices = { 1, 10, 2, 10 };
        var heating = testSuccessfulHeatingOptimization(prices, 2, prices.length, prices.length, 1, -1, 1,
                prices.length, prices.length, 2, false);
        assertArrayEquals(new double[] { 1, 1, 0, 0 }, heating, 0.000001);
    }

    @Test
    public void testHeatingStartsTwoPeriods() {
        double[] prices = { 1, 10, 2, 10, 10, 1, 9, 2, 10 };
        var heating = testSuccessfulHeatingOptimization(prices, 4, prices.length, prices.length, 1, 1, 1, prices.length,
                4, 2, false);
        assertArrayEquals(new double[] { 1, 1, 0, 0, 0, 1, 1, 0, 0 }, heating, 0.000001);
    }

    @Test
    public void testSecondaryObjectiveMinimizeStarts() {
        double[] prices = { 1, 5, 1, 5, 1, 1, 1 };
        var heating = testSuccessfulHeatingOptimization(prices, 3, prices.length, prices.length, -1, -1, 1,
                prices.length, prices.length, 3, false);
        assertArrayEquals(new double[] { 0, 0, 0, 0, 1, 1, 1 }, heating);
    }

    @Test
    public void testTertiaryObjectiveMinimizesLargestHeatingGap() {
        double[] prices = { 1, 1, 1, 1, 1, 1, 1, 1 };
        var heating = testSuccessfulHeatingOptimization(prices, 4, prices.length, prices.length, -1, -1, 1,
                prices.length, prices.length, 4, false);
        assertArrayEquals(new double[] { 0, 0, 1, 1, 1, 1, 0, 0 }, heating, 0.000001);
        // int largestGap = 0;
        // int currentGap = 0;
        // for (double variable : heating) {
        // if (variable == 0) {
        // currentGap++;
        // largestGap = Math.max(largestGap, currentGap);
        // } else {
        // currentGap = 0;
        // }
        // }
        // assertEquals(2, largestGap);
    }

    protected static double[] testSuccessfulHeatingOptimization(double[] prices, int totalHeatingNeed,
            int maxHeatingGapDuringFirstPeriod, int maxHeatingGapDuringSecondPeriod, int maxStartsDuringFirstPeriod,
            int maxStartsDuringSecondPeriod, int minHeatingPeriod, int firstGapSize, int firstPeriodLength,
            int heatingNeedDuringFirstPeriod, boolean previousPeriodEndsInHeating) {
        var result = HeatingOptimizerActionHandler.optimizeHeatingWithLinearProgramming(prices, totalHeatingNeed,
                maxHeatingGapDuringFirstPeriod, maxHeatingGapDuringSecondPeriod, maxStartsDuringFirstPeriod,
                maxStartsDuringSecondPeriod, minHeatingPeriod, firstGapSize, firstPeriodLength,
                heatingNeedDuringFirstPeriod, previousPeriodEndsInHeating, SOLVING_TIME_LIMIT);
        assertNotNull(result);
        assertTrue(
                result.status() == MPSolver.ResultStatus.OPTIMAL || result.status() == MPSolver.ResultStatus.FEASIBLE);
        var heating = result.heating();
        assertNotNull(heating);
        return Arrays.stream(heating).mapToDouble(MPVariable::solutionValue).toArray();
    }
}
