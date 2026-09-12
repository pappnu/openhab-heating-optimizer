package openhab.heating.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

public class TransformTest {
    @Test
    public void testMakePricesQuarterly() {
        double[] result = Transform.makePricesQuarterly(new double[] { 1, 2 }, 60, 0);
        assertArrayEquals(new double[] { 1, 1, 1, 1, 2, 2, 2, 2 }, result);
    }

    @Test
    public void testMakePricesQuarterlyWithQuarterlyPrices() {
        double[] result = Transform.makePricesQuarterly(new double[] { 1, 2 }, 15, 2);
        assertArrayEquals(new double[] { 1, 2 }, result);
    }

    @Test
    public void testHourlyPricesStartAtQuarterPast() {
        double[] result = Transform.makePricesQuarterly(new double[] { 10, 20, 30 }, 60, 3);
        assertArrayEquals(new double[] { 10, 10, 10, 20, 20, 20, 20, 30, 30, 30, 30 }, result);
    }

    @Test
    public void testHalfHourlyPricesStartAtQuarterPast() {
        double[] result = Transform.makePricesQuarterly(new double[] { 10, 20, 30 }, 30, 3);
        assertArrayEquals(new double[] { 10, 20, 20, 30, 30 }, result);
    }

    @Test
    public void testFirstBatchAtSourceBoundaryUsesFullInterval() {
        double[] result = Transform.makePricesQuarterly(new double[] { 10, 20 }, 30, 2);
        assertArrayEquals(new double[] { 10, 10, 20, 20 }, result);
    }

    @Test
    public void testStartTimeDeterminesFirstBatchSize() {
        ZonedDateTime start = ZonedDateTime.of(2026, 9, 12, 9, 15, 0, 0, ZoneId.of("UTC"));
        double[] result = Transform.makePricesQuarterly(new double[] { 10, 20, 30 }, 60, start);
        assertArrayEquals(new double[] { 10, 10, 10, 20, 20, 20, 20, 30, 30, 30, 30 }, result);
    }
}
