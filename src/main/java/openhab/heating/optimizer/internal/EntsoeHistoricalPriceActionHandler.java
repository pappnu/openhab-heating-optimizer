package openhab.heating.optimizer.internal;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import openhab.heating.utils.Items;

@NonNullByDefault
public class EntsoeHistoricalPriceActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    private final ItemRegistry itemRegistry;
    private final ThingRegistry thingRegistry;
    private final Logger logger = Objects
            .requireNonNull(LoggerFactory.getLogger(EntsoeHistoricalPriceActionHandler.class));

    public EntsoeHistoricalPriceActionHandler(Action module, ItemRegistry itemRegistry, ThingRegistry thingRegistry) {
        super(module);
        this.itemRegistry = itemRegistry;
        this.thingRegistry = thingRegistry;
    }

    @Override
    public @Nullable Map<String, @Nullable Object> execute(Map<String, Object> context) {
        var config = module.getConfiguration().as(EntsoeHistoricalPriceConfig.class);
        var thing = thingRegistry.get(new ThingUID(config.entsoeThingUID));
        if (thing == null) {
            throw new IllegalArgumentException("Entso-E thing '" + config.entsoeThingUID + "' was not found");
        }
        var thingConfiguration = thing.getConfiguration();
        var range = getRange(config);
        String token = getConfigValue(thingConfiguration, "securityToken");
        String area = getConfigValue(thingConfiguration, "area");
        String configuredResolution = getConfigValue(thingConfiguration, "resolution");
        int timeoutSeconds = getConfigValueInt(thingConfiguration, "requestTimeout", 30);
        @Nullable
        Duration targetResolution = configuredResolution.isBlank() ? null : Duration.parse(configuredResolution);

        try {
            var priceSeries = EntsoeHistoricalPriceFetcher.fetch(token, area, range.start(), range.end(),
                    targetResolution, timeoutSeconds);
            var item = Items.getItem(itemRegistry, config.spotPricesItem);
            var timeSeries = new TimeSeries(TimeSeries.Policy.REPLACE);
            String currency = config.currency.isBlank() ? priceSeries.currency() : config.currency;
            for (var entry : priceSeries.prices().entrySet()) {
                if (entry.getKey().isBefore(range.start()) || !entry.getKey().isBefore(range.end())) {
                    continue;
                }
                double adjustedPrice = entry.getValue() * config.priceMultiplier;
                String quantity = adjustedPrice + " " + currency + "/kWh";
                State state;
                try {
                    @Nullable
                    State quantityState = QuantityType.valueOf(quantity);
                    state = quantityState == null ? new DecimalType(adjustedPrice) : quantityState;
                } catch (IllegalArgumentException e) {
                    state = new DecimalType(adjustedPrice);
                }
                timeSeries.add(entry.getKey(), state);
            }
            if (timeSeries.size() == 0) {
                throw new IOException("Entso-E returned no prices for the requested period");
            }
            PersistenceExtensions.persist(item, timeSeries, config.persistenceServiceId);
            logger.info("Persisted {} historical Entso-E prices to {}", timeSeries.size(), item.getName());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch historical Entso-E prices", e);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while fetching historical Entso-E prices", e);
        }
        return null;
    }

    private static DateRange getRange(EntsoeHistoricalPriceConfig config) {
        if (config.pastDays < 0) {
            throw new IllegalArgumentException("Past days must not be negative");
        }
        boolean hasDays = config.pastDays > 0;
        boolean hasStart = !config.startTime.isBlank();
        boolean hasEnd = !config.endTime.isBlank();
        if (hasDays == (hasStart || hasEnd) || hasStart != hasEnd) {
            throw new IllegalArgumentException("Configure either past days or both start and end times");
        }
        if (hasDays) {
            var zone = ZoneId.systemDefault();
            var end = LocalDate.now(zone).atStartOfDay(zone).toInstant();
            var start = LocalDate.now(zone).minusDays(config.pastDays).atStartOfDay(zone).toInstant();
            return new DateRange(Objects.requireNonNull(start), Objects.requireNonNull(end));
        }
        var start = parseTime(config.startTime);
        var end = parseTime(config.endTime);
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        return new DateRange(Objects.requireNonNull(start), Objects.requireNonNull(end));
    }

    private static Instant parseTime(String value) {
        try {
            return Objects.requireNonNull(ZonedDateTime.parse(value).toInstant());
        } catch (RuntimeException e) {
            try {
                return Objects.requireNonNull(OffsetDateTime.parse(value).toInstant());
            } catch (RuntimeException parseException) {
                throw new IllegalArgumentException("Time must be ISO-8601 and include a UTC offset: " + value,
                        parseException);
            }
        }
    }

    private static String getConfigValue(Configuration thingConfiguration, String key) {
        Object value = thingConfiguration.get(key);
        return Objects.requireNonNull(Objects.toString(value, ""));
    }

    private static int getConfigValueInt(Configuration thingConfiguration, String key, int defaultValue) {
        String value = getConfigValue(thingConfiguration, key);
        return value.isBlank() ? defaultValue : Integer.parseInt(value);
    }

    private record DateRange(Instant start, Instant end) {
    }
}
