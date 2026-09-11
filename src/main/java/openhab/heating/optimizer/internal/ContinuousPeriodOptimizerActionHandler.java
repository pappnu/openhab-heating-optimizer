package openhab.heating.optimizer.internal;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import openhab.heating.utils.Items;
import openhab.heating.utils.TimeUtils;
import openhab.heating.utils.Transform;

@NonNullByDefault
public class ContinuousPeriodOptimizerActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    private final ItemRegistry itemRegistry;
    private final ScheduledExecutorService scheduler;
    private final Logger logger = LoggerFactory.getLogger(ContinuousPeriodOptimizerActionHandler.class);

    public ContinuousPeriodOptimizerActionHandler(Action module, ItemRegistry itemRegistry,
            ScheduledExecutorService scheduler) {
        super(module);
        this.itemRegistry = itemRegistry;
        this.scheduler = scheduler;
    }

    @Override
    public @Nullable Map<String, @Nullable Object> execute(Map<String, Object> context) {
        logger.info("Scheduling continuous period optimization");

        var conf = module.getConfiguration().as(ContinuousPeriodOptimizerConfig.class);
        var spotPricesItem = Items.getItem(itemRegistry, conf.spotPricesItem);

        Items.waitForTomorrowPersistence(spotPricesItem, scheduler,
                (result) -> optimize(conf, spotPricesItem, result.now(), result.today(), result.tomorrow()));

        return null;
    }

    private void optimize(ContinuousPeriodOptimizerConfig conf, Item spotPricesItem, ZonedDateTime now,
            ZonedDateTime today, ZonedDateTime tomorrow) {
        try {
            var dayAfterTomorrow = today.plusDays(2);
            var optStart = TimeUtils.truncateToNextQuarterHour(now);

            var controlItem = Items.getItem(itemRegistry, conf.controlItem);

            // Get spot prices spanning multiple days
            var pricesIter = PersistenceExtensions.getAllStatesBetween(spotPricesItem, optStart,
                    dayAfterTomorrow.minusSeconds(1), conf.persistenceServiceId);
            if (pricesIter == null) {
                throw new IllegalArgumentException("Spot prices iterator is null");
            }
            HistoricItem[] priceItems = StreamSupport.stream(pricesIter.spliterator(), false)
                    .toArray(HistoricItem[]::new);
            if (priceItems.length < 2) {
                throw new IllegalArgumentException("Not enough spot prices for continuous period optimization");
            }

            var secondToLastPrice = priceItems[priceItems.length - 2];
            var lastPrice = priceItems[priceItems.length - 1];

            var lastTime = lastPrice.getTimestamp();
            var timeStep = Duration.between(secondToLastPrice.getTimestamp(), lastTime);

            // Convert prices to double array
            double[] prices = Arrays.stream(priceItems).mapToDouble(item -> Items.getStateDouble(item.getState()))
                    .toArray();

            // Adjust price points to 15 minute frequency
            prices = Transform.makePricesQuarterly(prices, timeStep, 4 - optStart.getMinute() / 15);

            timeStep = Duration.ofMinutes(15);

            int periodLength = TimeUtils.convertHoursToTimeSteps(conf.periodLength, timeStep);
            int firstDaySteps = TimeUtils.convertToTimeSteps(Duration.between(optStart, tomorrow), timeStep);

            // Find cheapest period for today
            if (firstDaySteps > 0) {
                var firstDayResult = findCheapestPeriod(Arrays.copyOf(prices, firstDaySteps), periodLength);
                Items.persistControlPoints(controlItem, firstDayResult, optStart, timeStep, conf.persistenceServiceId);
            }

            // Find cheapest period for tomorrow
            var secondDayResult = findCheapestPeriod(
                    Arrays.copyOfRange(prices, Math.max(0, firstDaySteps - 1), prices.length), periodLength);
            Items.persistControlPoints(controlItem, secondDayResult, today.plusDays(1), timeStep,
                    conf.persistenceServiceId);

        } catch (Exception e) {
            logger.error("Failed to find optimal continuous heating period", e);
            throw e;
        }
    }

    /**
     * Find the cheapest continuous period of specified length in the price array
     * 
     * @param prices array of spot prices
     * @param timeSteps length of cheapest period to seek
     * @return array of zeroes and ones that matches prices in length. Ones indicate the cheapest period.
     */
    protected static double[] findCheapestPeriod(double[] prices, int timeSteps) {

        if (prices.length <= timeSteps) {
            double[] result = new double[prices.length];
            Arrays.fill(result, 1d);
            return result;
        }
        if (timeSteps < 1) {
            return new double[prices.length];
        }

        double minCost = Double.MAX_VALUE;
        int bestStartIndex = -1;

        // Use sliding window to find minimum cost period
        for (int i = 0; i <= prices.length - timeSteps; i++) {
            double periodCost = 0;
            for (int j = i; j < i + timeSteps; j++) {
                periodCost += prices[j];
            }

            if (periodCost < minCost) {
                minCost = periodCost;
                bestStartIndex = i;
            }
        }

        double[] result = new double[prices.length];
        for (int i = bestStartIndex; i < bestStartIndex + timeSteps; i++) {
            result[i] = 1d;
        }
        return result;
    }
}
