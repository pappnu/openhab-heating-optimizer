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
public class ContinuousPeriodOptimizerActionType extends ActionType {
    public static final String UID = "ContinuousPeriodOptimizerActionType";

    public static final String CONFIG_PERSISTENCE_SERVICE_ID = "persistenceServiceId";
    public static final String CONFIG_SPOT_PRICES = "spotPricesItem";
    public static final String CONFIG_INTERVAL_LENGTH_HOURS = "intervalLength";
    public static final String CONFIG_CONTROL_ITEM = "controlItem";

    public ContinuousPeriodOptimizerActionType(List<ConfigDescriptionParameter> configDescriptions,
            List<Input> inputs) {
        super(UID, configDescriptions, "Find cheapest continuous period during a day",
                "Finds the cheapest continuous period during a day based on spot prices.", null, Visibility.VISIBLE,
                inputs, null);
    }

    public static ActionType initialize() {
        final ConfigDescriptionParameter persistenceServiceId = ConfigDescriptionParameterBuilder
                .create(CONFIG_PERSISTENCE_SERVICE_ID, Type.TEXT).withRequired(true).withContext("persistence")
                .withLabel("ID of persistence service to use").build();
        final ConfigDescriptionParameter spotPrices = ConfigDescriptionParameterBuilder
                .create(CONFIG_SPOT_PRICES, Type.TEXT).withRequired(true).withContext("item")
                .withLabel("Spot prices item name").build();
        final ConfigDescriptionParameter intervalLength = ConfigDescriptionParameterBuilder
                .create(CONFIG_INTERVAL_LENGTH_HOURS, Type.DECIMAL).withRequired(true)
                .withLabel("Length of the period to search for in hours").build();
        final ConfigDescriptionParameter controlItem = ConfigDescriptionParameterBuilder
                .create(CONFIG_CONTROL_ITEM, Type.TEXT).withRequired(true).withContext("item")
                .withLabel("The item to store DHW heating control signal in").build();

        List<ConfigDescriptionParameter> config = new ArrayList<ConfigDescriptionParameter>();
        Collections.addAll(config, persistenceServiceId, spotPrices, intervalLength, controlItem);
        List<Input> input = new ArrayList<>();
        return new ContinuousPeriodOptimizerActionType(config, input);
    }
}
