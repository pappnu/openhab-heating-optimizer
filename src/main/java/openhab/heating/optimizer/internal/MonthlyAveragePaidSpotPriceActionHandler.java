package openhab.heating.optimizer.internal;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.persistence.HistoricItem;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.openhab.core.types.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import openhab.heating.utils.Items;

@NonNullByDefault
public class MonthlyAveragePaidSpotPriceActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    private final ItemRegistry itemRegistry;
    private final Logger logger = Objects.requireNonNull(LoggerFactory.getLogger(getClass()));

    public MonthlyAveragePaidSpotPriceActionHandler(Action module, ItemRegistry itemRegistry) {
        super(module);
        this.itemRegistry = itemRegistry;
    }

    @Override
    public @Nullable Map<String, @Nullable Object> execute(Map<String, Object> context) {
        var config = module.getConfiguration().as(MonthlyAveragePaidSpotPriceConfig.class);
        var costsItem = Items.getItem(itemRegistry, config.monthlyCostsItem);
        var consumptionItem = Items.getItem(itemRegistry, config.consumptionItem);
        var outputItem = Items.getItem(itemRegistry, config.outputItem);

        var monthlyCosts = new TreeMap<Instant, Double>();
        var zone = ZoneId.systemDefault();
        var historyStart = Instant.EPOCH.atZone(zone);
        var costStates = PersistenceExtensions.getAllStatesSince(costsItem, historyStart, config.persistenceServiceId);
        if (costStates != null) {
            for (HistoricItem item : costStates) {
                monthlyCosts.put(item.getTimestamp().toInstant(), Items.getStateDouble(item.getState()));
            }
        }
        if (monthlyCosts.isEmpty()) {
            return null;
        }

        var firstMonth = YearMonth.from(monthlyCosts.firstKey().atZone(zone));
        var lastMonth = YearMonth.from(monthlyCosts.lastKey().atZone(zone));
        var start = firstMonth.atDay(1).atStartOfDay(zone);
        var end = lastMonth.plusMonths(1).atDay(1).atStartOfDay(zone);
        var consumptionStates = PersistenceExtensions.getAllStatesBetween(consumptionItem, start, end,
                config.persistenceServiceId);
        var consumption = new TreeMap<Instant, Double>();
        if (consumptionStates != null) {
            for (HistoricItem item : consumptionStates) {
                consumption.put(item.getTimestamp().toInstant(), Items.getStateDouble(item.getState()));
            }
        }

        var timeSeries = calculateMonthlyAveragePrices(monthlyCosts, consumption, zone);
        if (timeSeries.size() > 0) {
            PersistenceExtensions.persist(outputItem, timeSeries, config.persistenceServiceId);
            logger.info("Persisted {} monthly average paid spot prices to {}", timeSeries.size(), outputItem.getName());
        }
        return null;
    }

    static TimeSeries calculateMonthlyAveragePrices(Map<Instant, Double> monthlyCosts,
            Map<Instant, Double> consumptions, ZoneId zone) {
        var timeSeries = new TimeSeries(TimeSeries.Policy.REPLACE);
        var monthlyConsumption = new TreeMap<YearMonth, Double>();
        for (var entry : consumptions.entrySet()) {
            var time = entry.getKey();
            var kwh = entry.getValue();
            var month = YearMonth.from(time.atZone(zone));
            monthlyConsumption.merge(month, kwh, Double::sum);
        }

        for (var entry : monthlyCosts.entrySet()) {
            var time = entry.getKey();
            var cost = entry.getValue();
            var month = YearMonth.from(time.atZone(zone));
            double consumption = monthlyConsumption.getOrDefault(month, 0.0);
            double averagePrice = consumption > 0 ? 100 * cost / consumption : 0.0;
            timeSeries.add(month.atDay(1).atStartOfDay(zone).toInstant(), new DecimalType(averagePrice));
        }
        return timeSeries;
    }
}
