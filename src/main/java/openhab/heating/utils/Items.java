package openhab.heating.utils;

import java.time.Duration;
import java.time.ZonedDateTime;
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

@NonNullByDefault
public class Items {
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
}
