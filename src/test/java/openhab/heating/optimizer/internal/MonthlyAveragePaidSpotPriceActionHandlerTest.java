package openhab.heating.optimizer.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.TimeSeries;

public class MonthlyAveragePaidSpotPriceActionHandlerTest {
    @Test
    public void calculateMonthlyAveragePricesUsingLocalMonthBoundaries() {
        var zone = ZoneId.of("Europe/Helsinki");
        var costs = new TreeMap<Instant, Double>();
        costs.put(Instant.parse("2026-01-01T00:00:00+02:00"), 120.0);
        costs.put(Instant.parse("2026-02-01T00:00:00+02:00"), 80.0);
        var consumption = new TreeMap<Instant, Double>();
        consumption.put(Instant.parse("2026-01-01T00:00:00+02:00"), 100.0);
        consumption.put(Instant.parse("2026-01-31T23:45:00+02:00"), 20.0);
        consumption.put(Instant.parse("2026-02-01T00:00:00+02:00"), 40.0);
        consumption.put(Instant.parse("2026-02-28T23:45:00+02:00"), 40.0);

        var result = MonthlyAveragePaidSpotPriceActionHandler.calculateMonthlyAveragePrices(costs, consumption, zone);

        var states = result.getStates().toList();
        assertEquals(TimeSeries.Policy.REPLACE, result.getPolicy());
        assertEquals(List.of(new TimeSeries.Entry(Instant.parse("2025-12-31T22:00:00Z"), new DecimalType(100.0)),
                new TimeSeries.Entry(Instant.parse("2026-01-31T22:00:00Z"), new DecimalType(100.0))), states);
    }

    @Test
    public void returnsZeroForMonthsWithoutPositiveConsumption() {
        var zone = ZoneId.of("UTC");
        var costs = java.util.Map.of(Instant.parse("2026-01-01T00:00:00Z"), 120.0,
                Instant.parse("2026-02-01T00:00:00Z"), 80.0);
        var consumption = java.util.Map.of(Instant.parse("2026-01-01T00:00:00Z"), 0.0);

        var result = MonthlyAveragePaidSpotPriceActionHandler.calculateMonthlyAveragePrices(costs, consumption, zone);

        assertEquals(
                List.of(new TimeSeries.Entry(Instant.parse("2026-01-01T00:00:00Z"), new DecimalType(0.0)),
                        new TimeSeries.Entry(Instant.parse("2026-02-01T00:00:00Z"), new DecimalType(0.0))),
                result.getStates().toList());
    }
}
