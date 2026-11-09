package openhab.heating.optimizer.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;

@NonNullByDefault
public class EntsoeHistoricalPriceActionType extends ActionType {
    public static final String UID = "EntsoeHistoricalPrice";

    public static final String CONFIG_ENTSOE_THING_UID = "entsoeThingUID";
    public static final String CONFIG_PERSISTENCE_SERVICE_ID = "persistenceServiceId";
    public static final String CONFIG_SPOT_PRICES_ITEM = "spotPricesItem";
    public static final String CONFIG_PAST_DAYS = "pastDays";
    public static final String CONFIG_START_TIME = "startTime";
    public static final String CONFIG_END_TIME = "endTime";
    public static final String CONFIG_PRICE_MULTIPLIER = "priceMultiplier";
    public static final String CONFIG_CURRENCY = "currency";

    public EntsoeHistoricalPriceActionType(List<ConfigDescriptionParameter> configDescriptions, List<Input> inputs) {
        super(UID, configDescriptions, "Fetch historical Entso-E spot prices",
                "Fetches historical Entso-E spot prices and persists them to an item.", null, Visibility.VISIBLE,
                inputs, null);
    }

    public static ActionType initialize() {
        final var entsoeThingUID = ConfigDescriptionParameterBuilder.create(CONFIG_ENTSOE_THING_UID, Type.TEXT)
                .withRequired(true).withContext("thing").withLabel("Entso-E thing").build();
        final var persistenceServiceId = ConfigDescriptionParameterBuilder
                .create(CONFIG_PERSISTENCE_SERVICE_ID, Type.TEXT).withRequired(true).withContext("persistence")
                .withLabel("Persistence service").build();
        final var spotPricesItem = ConfigDescriptionParameterBuilder.create(CONFIG_SPOT_PRICES_ITEM, Type.TEXT)
                .withRequired(true).withContext("item").withLabel("Spot prices item").build();
        final var pastDays = ConfigDescriptionParameterBuilder.create(CONFIG_PAST_DAYS, Type.INTEGER)
                .withRequired(false).withLabel("Number of past days to fetch").build();
        final var startTime = ConfigDescriptionParameterBuilder.create(CONFIG_START_TIME, Type.TEXT).withRequired(false)
                .withLabel("Start time (ISO-8601 with offset)").build();
        final var endTime = ConfigDescriptionParameterBuilder.create(CONFIG_END_TIME, Type.TEXT).withRequired(false)
                .withLabel("End time (ISO-8601 with offset)").build();
        final var priceMultiplier = ConfigDescriptionParameterBuilder.create(CONFIG_PRICE_MULTIPLIER, Type.DECIMAL)
                .withRequired(false).withDefault("1.0").withLabel("Price multiplier").build();
        final var currency = ConfigDescriptionParameterBuilder.create(CONFIG_CURRENCY, Type.TEXT).withRequired(false)
                .withLabel("Currency override").build();

        var config = new ArrayList<ConfigDescriptionParameter>();
        Collections.addAll(config, entsoeThingUID, persistenceServiceId, spotPricesItem, pastDays, startTime, endTime,
                priceMultiplier, currency);
        return new EntsoeHistoricalPriceActionType(config, new ArrayList<>());
    }
}
