package openhab.heating.utils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class Items {
    private final static Logger logger = LoggerFactory.getLogger(Items.class);
    private static final int MAX_ATTEMPTS = 4;
    /**
     * In seconds
     */
    private static final int ATTEMPT_DELAY = 5;

    public static Item getItem(ItemRegistry registry, String id) {
        var item = registry.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Item with ID '%s' not found from registry".formatted(id));
        }
        return item;
    }

    public static DecimalType getStateDecimal(@Nullable State state) {
        if (state == null) {
            throw new IllegalArgumentException("State may not be null");
        }
        var stateDecimal = state.as(DecimalType.class);
        if (stateDecimal == null) {
            throw new IllegalArgumentException("State has to be of DecimalType");
        }
        return stateDecimal;
    }

    public static double getStateDouble(@Nullable State state) {
        return getStateDecimal(state).doubleValue();
    }

    public static float getStateFloat(@Nullable State state) {
        return getStateDecimal(state).floatValue();
    }

    public static int getStateInt(@Nullable State state) {
        return getStateDecimal(state).intValue();
    }

    /**
     * Finds the timestamp of a persisted value equal to 1 closest to timeUpperBoundary.
     * 
     * @param item The openHAB Item to query
     * @param timeLowerBound Inclusice lower time boundary
     * @param timeUpperBound Exclusive upper time boundary
     * @return ZonedDateTime of the matching state, or null if not found
     */
    public static @Nullable ZonedDateTime getClosestTimestampEqualsOne(Item item, ZonedDateTime timeLowerBound,
            ZonedDateTime timeUpperBound, String serviceId) {
        var statesIter = PersistenceExtensions.getAllStatesBetween(item, timeLowerBound, timeUpperBound.minusSeconds(1),
                serviceId);
        if (statesIter == null)
            return null;
        var states = StreamSupport.stream(statesIter.spliterator(), false).toList();

        // Iterate backwards
        for (HistoricItem historicItem : states.reversed()) {
            var state = historicItem.getState().as(DecimalType.class);
            if (state == null) {
                break;
            }
            if (state.intValue() == 1) {
                return historicItem.getTimestamp();
            }
        }

        return null;
    }

    public static void persistControlPoints(Item item, double[] points, ZonedDateTime startTime, Duration timeStep,
            String persistenceServiceId) {
        TimeSeries timeSeries = new TimeSeries(TimeSeries.Policy.REPLACE);
        for (int i = 0; i < points.length; i++) {
            timeSeries.add(startTime.plus(timeStep.multipliedBy(i)).toInstant(), new DecimalType(points[i]));
        }
        PersistenceExtensions.persist(item, timeSeries, persistenceServiceId);
    }

    public record TomorrowPersistenceWaitResult(ZonedDateTime now, ZonedDateTime today, ZonedDateTime tomorrow) {
    }

    private static void waitForTomorrowPersistence(Item item, ScheduledExecutorService scheduler,
            Consumer<TomorrowPersistenceWaitResult> whenReady, int attempt, int maxAttempts, int attemptDelay) {
        if (attempt > MAX_ATTEMPTS) {
            logger.error("Failed to optimize heating since there are no spot prices available");
            return;
        }

        var now = ZonedDateTime.now();
        var today = now.truncatedTo(ChronoUnit.DAYS);
        var tomorrow = today.plusDays(1);

        var lastPriceTimeStamp = PersistenceExtensions.lastUpdate(item);

        // Keep waiting for prices
        if (lastPriceTimeStamp == null || (!lastPriceTimeStamp.isAfter(tomorrow) && attempt < MAX_ATTEMPTS)) {
            scheduler.schedule(() -> waitForTomorrowPersistence(item, scheduler, whenReady, attempt + 1, maxAttempts,
                    attemptDelay), ATTEMPT_DELAY, TimeUnit.SECONDS);
            return;
        }

        whenReady.accept(new TomorrowPersistenceWaitResult(now, today, tomorrow));
    }

    public static void waitForTomorrowPersistence(Item item, ScheduledExecutorService scheduler,
            Consumer<TomorrowPersistenceWaitResult> whenReady) {
        waitForTomorrowPersistence(item, scheduler, whenReady, 1, MAX_ATTEMPTS, ATTEMPT_DELAY);
    }
}
