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
public class MonthlyAveragePaidSpotPriceActionType extends ActionType {
    public static final String UID = "MonthlyAveragePaidSpotPrice";
    public static final String CONFIG_PERSISTENCE_SERVICE_ID = "persistenceServiceId";
    public static final String CONFIG_MONTHLY_COSTS_ITEM = "monthlyCostsItem";
    public static final String CONFIG_CONSUMPTION_ITEM = "consumptionItem";
    public static final String CONFIG_OUTPUT_ITEM = "outputItem";

    public MonthlyAveragePaidSpotPriceActionType(List<ConfigDescriptionParameter> configDescriptions,
            List<Input> inputs) {
        super(UID, configDescriptions, "Calculate monthly average paid spot prices",
                "Calculates average monthly electricity cost.", null, Visibility.VISIBLE, inputs, null);
    }

    public static ActionType initialize() {
        final var persistenceServiceId = ConfigDescriptionParameterBuilder
                .create(CONFIG_PERSISTENCE_SERVICE_ID, Type.TEXT).withRequired(true).withContext("persistence")
                .withLabel("Persistence service").build();
        final var monthlyCostsItem = ConfigDescriptionParameterBuilder.create(CONFIG_MONTHLY_COSTS_ITEM, Type.TEXT)
                .withRequired(true).withContext("item").withLabel("Electricity costs item").build();
        final var consumptionItem = ConfigDescriptionParameterBuilder.create(CONFIG_CONSUMPTION_ITEM, Type.TEXT)
                .withRequired(true).withContext("item").withLabel("Electricity consumption item").build();
        final var outputItem = ConfigDescriptionParameterBuilder.create(CONFIG_OUTPUT_ITEM, Type.TEXT)
                .withRequired(true).withContext("item").withLabel("Output item").build();
        var config = new ArrayList<ConfigDescriptionParameter>();
        Collections.addAll(config, persistenceServiceId, monthlyCostsItem, consumptionItem, outputItem);
        return new MonthlyAveragePaidSpotPriceActionType(config, new ArrayList<>());
    }
}
