package openhab.heating.optimizer.internal;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.StringType;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import openhab.heating.utils.Items;

@NonNullByDefault
public class TemperatureDropTimeModelingActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    private static final int POLYNOMIAL_DEGREE = 2;
    private static final int NUMBER_OF_PREDICTOR_VARIABLES = 3;

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;
    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(TemperatureDropTimeModelingActionHandler.class);

    public TemperatureDropTimeModelingActionHandler(Action module, ItemRegistry itemRegistry,
            EventPublisher eventPublisher) {
        super(module);
        this.itemRegistry = itemRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public @Nullable Map<String, @Nullable Object> execute(Map<String, Object> context) {
        var config = module.getConfiguration().as(HeatingNeedModelingConfig.class);
        var heatpumpItem = Items.getItem(itemRegistry, config.heatpumpItem);
        var outsideTemperatureItem = Items.getItem(itemRegistry, config.outsideTemperatureItem);
        var solarForecastItem = Items.getItem(itemRegistry, config.solarForecastItem);
        var insideTemperatureItem = Items.getItem(itemRegistry, config.insideTemperatureItem);
        var outputItem = Items.getItem(itemRegistry, config.outputItem);
        var results = collectData(heatpumpItem, outsideTemperatureItem, solarForecastItem, insideTemperatureItem,
                config.maxDataGapDays, ZonedDateTime.now().minusDays(1), config.persistenceServiceId);

        if (results.size() > NUMBER_OF_PREDICTOR_VARIABLES) {
            var independentVariables = new double[NUMBER_OF_PREDICTOR_VARIABLES][results.size()];
            var timeToDropHours = new double[results.size()];
            for (int i = 0; i < results.size(); i++) {
                var sample = results.get(i);
                independentVariables[0][i] = sample.temperatureDrop();
                independentVariables[1][i] = sample.averageOutsideTemperature();
                independentVariables[2][i] = sample.dailySolarExposure();
                timeToDropHours[i] = sample.timeToDropHours();
            }
            var model = HeatingNeedModelingActionHandler.fit(independentVariables, timeToDropHours, POLYNOMIAL_DEGREE);
            eventPublisher.post(
                    ItemEventFactory.createCommandEvent(outputItem.getName(), new StringType(gson.toJson(model))));
            logger.info("Temperature drop time model updated based on {} data points", results.size());
        } else {
            logger.error("Temperature drop time modeling found {} valid records. At least {} records are required.",
                    results.size(), NUMBER_OF_PREDICTOR_VARIABLES + 1);
        }
        return null;
    }

    protected static ArrayList<TemperatureDropSample> collectData(Item heatpumpItem, Item outsideTemperatureItem,
            Item solarForecastItem, Item insideTemperatureItem, int maxDataGapDays, ZonedDateTime latestDay,
            String persistenceServiceId) {
        var results = new ArrayList<TemperatureDropSample>();
        var day = latestDay.truncatedTo(ChronoUnit.DAYS);
        int missingDays = 0;
        while (missingDays <= maxDataGapDays) {
            var end = day.plusDays(1);
            var outsideAverage = PersistenceExtensions.averageBetween(outsideTemperatureItem, day, end,
                    persistenceServiceId);
            var solarExposure = PersistenceExtensions.riemannSumBetween(solarForecastItem, day, end,
                    persistenceServiceId);
            var heatpumpStates = Items.getAllStatesBetweenIncludingStartState(heatpumpItem, day, end,
                    persistenceServiceId);
            var insideTemperatureStates = Items.getAllStatesBetweenIncludingStartState(insideTemperatureItem, day, end,
                    persistenceServiceId);

            if (outsideAverage == null || solarExposure == null || heatpumpStates == null
                    || insideTemperatureStates == null) {
                missingDays++;
            } else {
                missingDays = 0;
                results.addAll(
                        collectCoolingSamples(toTimedValues(heatpumpStates), toTimedValues(insideTemperatureStates),
                                day, Items.getStateDouble(outsideAverage), Items.getStateDouble(solarExposure)));
            }
            day = day.minusDays(1);
        }
        return results;
    }

    protected static List<TemperatureDropSample> collectCoolingSamples(List<TimedValue> heatpumpValues,
            List<TimedValue> insideTemperatureValues, ZonedDateTime day, double averageOutsideTemperature,
            double dailySolarExposure) {
        var dayStart = day.truncatedTo(ChronoUnit.DAYS);
        var dayEnd = dayStart.plusDays(1);
        var heatpumpPoints = withStartState(heatpumpValues, dayStart, dayEnd);
        var temperaturePoints = withStartState(insideTemperatureValues, dayStart, dayEnd);
        var results = new ArrayList<TemperatureDropSample>();
        @Nullable
        ZonedDateTime offStart = null;
        boolean heatingOff = false;

        for (var point : heatpumpPoints) {
            boolean pointIsOff = point.value() == 0;
            if (pointIsOff && !heatingOff) {
                offStart = point.timestamp();
            } else if (!pointIsOff && heatingOff && offStart != null) {
                collectIntervalSamples(results, temperaturePoints, offStart, point.timestamp(), dayStart,
                        averageOutsideTemperature, dailySolarExposure);
                offStart = null;
            }
            heatingOff = pointIsOff;
        }
        if (heatingOff && offStart != null) {
            collectIntervalSamples(results, temperaturePoints, offStart, dayEnd, dayStart, averageOutsideTemperature,
                    dailySolarExposure);
        }
        return results;
    }

    private static void collectIntervalSamples(List<TemperatureDropSample> results, List<TimedValue> temperaturePoints,
            ZonedDateTime intervalStart, ZonedDateTime intervalEnd, ZonedDateTime day, double averageOutsideTemperature,
            double dailySolarExposure) {
        var intervalPoints = withStartState(temperaturePoints, intervalStart, intervalEnd);
        if (intervalPoints.size() < 2) {
            return;
        }
        var first = intervalPoints.get(0);
        var last = intervalPoints.get(intervalPoints.size() - 1);
        double temperatureDrop = first.value() - last.value();
        double elapsedHours = Duration.between(first.timestamp(), last.timestamp()).toNanos()
                / (double) Duration.ofHours(1).toNanos();
        if (temperatureDrop > 0 && elapsedHours > 0) {
            results.add(new TemperatureDropSample(day, temperatureDrop, averageOutsideTemperature, dailySolarExposure,
                    elapsedHours));
        }
    }

    /**
     * Truncates the values to start from the `start` timestamp and end before the `end` timestamp.
     * The most recent value before `start`, if such exists, is used as the first value
     * and is given the `start` timestamp.
     */
    private static List<TimedValue> withStartState(List<TimedValue> values, ZonedDateTime start, ZonedDateTime end) {
        // var orderedValues = values.stream().sorted(Comparator.comparing(TimedValue::timestamp)).toList();
        var points = new ArrayList<TimedValue>();
        @Nullable
        TimedValue startState = null;
        for (var value : values) {
            if (!value.timestamp().isAfter(start)) {
                startState = value;
            } else if (value.timestamp().isBefore(end)) {
                points.add(value);
            }
        }
        if (startState != null) {
            points.add(0, new TimedValue(start, startState.value()));
        }
        return points;
    }

    private static List<TimedValue> toTimedValues(List<HistoricItem> states) {
        return states.stream()
                .map(state -> new TimedValue(state.getTimestamp(), Items.getStateDouble(state.getState()))).toList();
    }

    public record TimedValue(ZonedDateTime timestamp, double value) {
    }

    public record TemperatureDropSample(ZonedDateTime day, double temperatureDrop, double averageOutsideTemperature,
            double dailySolarExposure, double timeToDropHours) {
    }
}
