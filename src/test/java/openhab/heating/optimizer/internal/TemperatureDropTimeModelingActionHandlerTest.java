package openhab.heating.optimizer.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import openhab.heating.optimizer.internal.TemperatureDropTimeModelingActionHandler.TimedValue;

public class TemperatureDropTimeModelingActionHandlerTest {
    @Test
    public void collectsTemperatureDropsOnlyWhileHeatingIsOff() {
        var day = ZonedDateTime.parse("2026-01-01T00:00:00Z");
        var heatpump = List.of(new TimedValue(day, 1), new TimedValue(day.plusHours(1), 0),
                new TimedValue(day.plusHours(5), 1), new TimedValue(day.plusHours(6), 0));
        var insideTemperature = List.of(new TimedValue(day, 21), new TimedValue(day.plusHours(1).plusMinutes(30), 21.5),
                new TimedValue(day.plusHours(2), 21.2), new TimedValue(day.plusHours(3), 21),
                new TimedValue(day.plusHours(4), 20.8), new TimedValue(day.plusHours(6), 20.9),
                new TimedValue(day.plusHours(7), 20.6), new TimedValue(day.plusHours(8), 20.4));

        var samples = TemperatureDropTimeModelingActionHandler.collectCoolingSamples(heatpump, insideTemperature, day,
                3, 12);

        assertEquals(2, samples.size());
        assertEquals(0.2, samples.get(0).temperatureDrop(), 0.000001);
        assertEquals(3, samples.get(0).timeToDropHours(), 0.000001);
        assertEquals(0.5, samples.get(1).temperatureDrop(), 0.000001);
        assertEquals(2, samples.get(1).timeToDropHours(), 0.000001);
        for (var sample : samples) {
            assertEquals(3, sample.averageOutsideTemperature(), 0.000001);
            assertEquals(12, sample.dailySolarExposure(), 0.000001);
        }
    }

    @Test
    public void doesNotCollectIntervalWithNetTemperatureIncrease() {
        var day = ZonedDateTime.parse("2026-01-01T00:00:00Z");
        var heatpump = List.of(new TimedValue(day, 1), new TimedValue(day.plusHours(1), 0));
        var insideTemperature = List.of(new TimedValue(day, 20), new TimedValue(day.plusHours(2), 20.5),
                new TimedValue(day.plusHours(3), 20.2));

        var samples = TemperatureDropTimeModelingActionHandler.collectCoolingSamples(heatpump, insideTemperature, day,
                0, 0);

        assertEquals(0, samples.size());
    }

    @Test
    public void polynomialModelPredictsTimeFromDropAndDailyConditions() {
        var independentVariables = new double[3][27];
        var timeToDropHours = new double[27];
        int sampleIndex = 0;
        for (double drop : new double[] { 0.1, 0.4, 0.9 }) {
            for (double outsideTemperature : new double[] { -5, 5, 15 }) {
                for (double solarExposure : new double[] { 0, 4, 10 }) {
                    independentVariables[0][sampleIndex] = drop;
                    independentVariables[1][sampleIndex] = outsideTemperature;
                    independentVariables[2][sampleIndex] = solarExposure;
                    timeToDropHours[sampleIndex] = 4 + 2 * drop - 0.5 * outsideTemperature + 0.1 * solarExposure
                            + 0.03 * drop * outsideTemperature + 0.01 * solarExposure * solarExposure;
                    sampleIndex++;
                }
            }
        }

        var model = HeatingNeedModelingActionHandler.fit(independentVariables, timeToDropHours, 2);

        assertEquals(2.92, model.estimate(new double[] { 0.4, 5, 4 }), 0.000001);
    }
}
