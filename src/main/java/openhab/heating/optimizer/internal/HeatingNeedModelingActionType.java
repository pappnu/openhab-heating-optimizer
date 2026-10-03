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
public class HeatingNeedModelingActionType extends ActionType {
    public static final String UID = "HeatingNeedModeling";

    public static final String CONFIG_PERSISTENCE_SERVICE_ID = "persistenceServiceId";
    public static final String CONFIG_HEATPUMP_ITEM = "heatpumpItem";
    public static final String CONFIG_OUTSIDE_TEMPERATURE_ITEM = "outsideTemperatureItem";
    public static final String CONFIG_SOLAR_FORECAST_ITEM = "solarForecastItem";
    public static final String CONFIG_INSIDE_TEMPERATURE_ITEM = "insideTemperatureItem";
    public static final String CONFIG_MAX_DATA_GAP_DAYS = "maxDataGapDays";
    public static final String CONFIG_OUTPUT_ITEM = "outputItem";

    public HeatingNeedModelingActionType(List<ConfigDescriptionParameter> configDescriptions, List<Input> inputs) {
        super(UID, configDescriptions, "Model heating need",
                "Models daily heating need based on inside-outside temperature delta and solar exposure.", null,
                Visibility.VISIBLE, inputs, null);
    }

    public static ActionType initialize() {
        var persistenceServiceId = ConfigDescriptionParameterBuilder.create(CONFIG_PERSISTENCE_SERVICE_ID, Type.TEXT)
                .withRequired(true).withContext("persistence").withLabel("Persistence service").build();
        var heatpumpItem = ConfigDescriptionParameterBuilder.create(CONFIG_HEATPUMP_ITEM, Type.TEXT).withRequired(true)
                .withContext("item").withLabel("Heatpump on/off item").build();
        var insideTemperatureItem = ConfigDescriptionParameterBuilder.create(CONFIG_INSIDE_TEMPERATURE_ITEM, Type.TEXT)
                .withRequired(true).withContext("item").withLabel("Inside temperature item").build();
        var outsideTemperatureItem = ConfigDescriptionParameterBuilder
                .create(CONFIG_OUTSIDE_TEMPERATURE_ITEM, Type.TEXT).withRequired(true).withContext("item")
                .withLabel("Outside temperature item").build();
        var solarForecastItem = ConfigDescriptionParameterBuilder.create(CONFIG_SOLAR_FORECAST_ITEM, Type.TEXT)
                .withRequired(true).withContext("item").withLabel("Solar forecast item").build();
        var maxDataGapDays = ConfigDescriptionParameterBuilder.create(CONFIG_MAX_DATA_GAP_DAYS, Type.INTEGER)
                .withRequired(true).withLabel("Maximum data gap in days").build();
        var outputItem = ConfigDescriptionParameterBuilder.create(CONFIG_OUTPUT_ITEM, Type.TEXT).withRequired(true)
                .withContext("item").withLabel("Output item").build();

        var config = new ArrayList<ConfigDescriptionParameter>();
        Collections.addAll(config, persistenceServiceId, heatpumpItem, insideTemperatureItem, outsideTemperatureItem,
                solarForecastItem, maxDataGapDays, outputItem);
        return new HeatingNeedModelingActionType(config, new ArrayList<>());
    }
}
