package openhab.heating.optimizer.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import openhab.heating.optimizer.internal.HeatingNeedModelingActionHandler.DailyHeatingNeed;

public class HeatingNeedModelingActionHandlerTest {
    @Test
    public void fitPolynomialAndEstimateHeatingNeed() {
        var now = ZonedDateTime.now();
        var data = List.of(new DailyHeatingNeed(now, 5, 0, 0), new DailyHeatingNeed(now, 11, 1, 0),
                new DailyHeatingNeed(now, 14, 0, 1), new DailyHeatingNeed(now, 25, 1, 1),
                new DailyHeatingNeed(now, 25, 2, 0), new DailyHeatingNeed(now, 35, 0, 2),
                new DailyHeatingNeed(now, 44, 2, 1), new DailyHeatingNeed(now, 51, 1, 2));

        var model = HeatingNeedModelingActionHandler.fit(data, 2);

        assertEquals(25, model.estimate(new double[] { 1, 1 }), 0.000001);
    }

    @Test
    public void prepareAndFitMultivariatePolynomialWithInteractions() {
        var independentVariables = new double[][] { { 0, 1, 0, 1, 2, 0, 2, 1 }, { 0, 0, 1, 1, 0, 2, 1, 2 } };
        var dependentVariable = new double[] { 5, 11, 14, 25, 25, 35, 44, 51 };

        var preparedData = HeatingNeedModelingActionHandler.preparePolynomialData(independentVariables, 2);
        var model = HeatingNeedModelingActionHandler.fit(independentVariables, dependentVariable, 2);

        assertArrayEquals(new double[] { 1, 0, 1, 0, 0 }, preparedData[1], 0.000001);
        assertEquals(25, model.estimate(new double[] { 1, 1 }), 0.000001);
    }

    @Test
    public void preparePolynomialDataWithDegreeOneAndOneVariable() {
        var preparedData = HeatingNeedModelingActionHandler.preparePolynomialData(new double[][] { { 2 } }, 1);

        assertArrayEquals(new double[] { 2 }, preparedData[0], 0.000001);
    }

    @Test
    public void preparePolynomialDataWithDegreeTwoAndTwoVariables() {
        var preparedData = HeatingNeedModelingActionHandler.preparePolynomialData(new double[][] { { 2 }, { 3 } }, 2);

        assertArrayEquals(new double[] { 2, 3, 4, 6, 9 }, preparedData[0], 0.000001);
    }

    @Test
    public void preparePolynomialDataWithDegreeThreeAndTwoVariables() {
        var preparedData = HeatingNeedModelingActionHandler.preparePolynomialData(new double[][] { { 2 }, { 3 } }, 3);

        assertArrayEquals(new double[] { 2, 3, 4, 6, 9, 8, 12, 18, 27 }, preparedData[0], 0.000001);
    }

    @Test
    public void fitAndCheckIndependentVariableMeansAndStandardDeviations() {
        var model = HeatingNeedModelingActionHandler.fit(new double[][] { { 10, 20, 30, 40 }, { 0, 1, 2, 3 } },
                new double[] { 1, 2, 3, 4 }, 1);

        assertArrayEquals(new double[] { 25, 1.5 }, model.means(), 0.000001);
        assertArrayEquals(new double[] { Math.sqrt(125), Math.sqrt(1.25) }, model.standardDeviations(), 0.000001);
        assertEquals(2, model.estimate(new double[] { 20, 1 }), 0.000001);
    }
}
