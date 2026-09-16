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
public class HeatingOptimizerActionType extends ActionType {
    public static final String UID = "HeatingOptimizerActionType";

    public static final String CONFIG_PERSISTENCE_SERVICE_ID = "persistenceServiceId";
    public static final String CONFIG_SPOT_PRICES = "spotPricesItem";
    public static final String CONFIG_AIR_TEMPERATURES = "airTemperaturesItem";
    public static final String CONFIG_HEATING_TEMPERATURES = "heatingTemperatures";
    public static final String CONFIG_HEATING_NEEDS = "heatingNeeds";
    public static final String CONFIG_GAP_TEMPERATURES = "gapTemperatures";
    public static final String CONFIG_GAPS = "gaps";
    public static final String CONFIG_MAX_STARTS_TEMPERATURES = "maxStartsTemperatures";
    public static final String CONFIG_MAX_STARTS = "maxStarts";
    public static final String CONFIG_MIN_CONTINUOUS_HEATING_PERIOD = "minHeatingPeriod";
    public static final String CONFIG_MAX_SOLVING_TIME = "maxSolvingTime";
    public static final String CONFIG_PRICE_FLOOR_SOFT = "priceFloorSoft";
    public static final String CONFIG_PRICE_FLOOR_HARD = "priceFloorHard";
    public static final String CONFIG_HEATING_CONTROL_OUTPUT_ITEM = "heatingControlItem";

    public HeatingOptimizerActionType(List<ConfigDescriptionParameter> configDescriptions, List<Input> inputs) {
        super(UID, configDescriptions, "Calculate optimal heating periods",
                "Calculates optimal heating periods based on outside temperature and electricity spot prices.", null,
                Visibility.VISIBLE, inputs, null);
    }

    public static ActionType initialize() {
        final ConfigDescriptionParameter persistenceServiceId = ConfigDescriptionParameterBuilder
                .create(CONFIG_PERSISTENCE_SERVICE_ID, Type.TEXT).withRequired(true).withContext("persistence")
                .withLabel("Persistence service").build();
        final ConfigDescriptionParameter spotPrices = ConfigDescriptionParameterBuilder
                .create(CONFIG_SPOT_PRICES, Type.TEXT).withRequired(true).withContext("item")
                .withLabel("Spot prices item").build();
        final ConfigDescriptionParameter airTemperatures = ConfigDescriptionParameterBuilder
                .create(CONFIG_AIR_TEMPERATURES, Type.TEXT).withRequired(true).withContext("item")
                .withLabel("Air temperatures item").build();
        final ConfigDescriptionParameter heatingTemperatures = ConfigDescriptionParameterBuilder
                .create(CONFIG_HEATING_TEMPERATURES, Type.TEXT).withRequired(true)
                .withLabel("Ascending average temperature levels for heating needs, e.g. '-20.0,0.5,15'.").build();
        final ConfigDescriptionParameter heatingNeeds = ConfigDescriptionParameterBuilder
                .create(CONFIG_HEATING_NEEDS, Type.TEXT).withRequired(true)
                .withLabel("Heating hours required at each temperature level, e.g. '24.0,10,0.0'.").build();
        final ConfigDescriptionParameter gapTemperatures = ConfigDescriptionParameterBuilder
                .create(CONFIG_GAP_TEMPERATURES, Type.TEXT).withRequired(true)
                .withLabel("Ascending average temperature levels for max heating gaps, e.g. '-20.0,0.5,15'.").build();
        final ConfigDescriptionParameter gaps = ConfigDescriptionParameterBuilder.create(CONFIG_GAPS, Type.TEXT)
                .withRequired(true)
                .withLabel("The hours heating may be continuously off at each temperature level, e.g. '1.0,4.5,24.0'.")
                .build();
        final ConfigDescriptionParameter maxStartsTemperatures = ConfigDescriptionParameterBuilder
                .create(CONFIG_MAX_STARTS_TEMPERATURES, Type.TEXT)
                .withLabel("Ascending average temperature levels for max starts, e.g. '-20.0,0.5,15'.").build();
        final ConfigDescriptionParameter maxStarts = ConfigDescriptionParameterBuilder
                .create(CONFIG_MAX_STARTS, Type.TEXT)
                .withLabel(
                        "The number of times heating may start during a day at each temperature level, e.g. '6,4,3'.")
                .build();
        final ConfigDescriptionParameter minContHeatingPeriod = ConfigDescriptionParameterBuilder
                .create(CONFIG_MIN_CONTINUOUS_HEATING_PERIOD, Type.DECIMAL).withRequired(true)
                .withLabel("Minimum length of a heating period in hours").build();
        final ConfigDescriptionParameter maxSolvingTime = ConfigDescriptionParameterBuilder
                .create(CONFIG_MAX_SOLVING_TIME, Type.DECIMAL).withRequired(true)
                .withLabel("Maximum time to use for solving the linear programming problem in seconds").build();
        final ConfigDescriptionParameter priceFloorSoft = ConfigDescriptionParameterBuilder
                .create(CONFIG_PRICE_FLOOR_SOFT, Type.DECIMAL).withRequired(false)
                .withLabel("Allow heating when price is below average price and given level").build();
        final ConfigDescriptionParameter priceFloorHard = ConfigDescriptionParameterBuilder
                .create(CONFIG_PRICE_FLOOR_HARD, Type.DECIMAL).withRequired(false)
                .withLabel("Allow heating when price is below given level").build();
        final ConfigDescriptionParameter heatingControlItem = ConfigDescriptionParameterBuilder
                .create(CONFIG_HEATING_CONTROL_OUTPUT_ITEM, Type.TEXT).withRequired(true).withContext("item")
                .withLabel("Result item").build();

        List<ConfigDescriptionParameter> config = new ArrayList<ConfigDescriptionParameter>();
        Collections.addAll(config, persistenceServiceId, spotPrices, airTemperatures, heatingTemperatures, heatingNeeds,
                gapTemperatures, gaps, maxStartsTemperatures, maxStarts, minContHeatingPeriod, maxSolvingTime,
                priceFloorSoft, priceFloorHard, heatingControlItem);
        List<Input> input = new ArrayList<>();
        return new HeatingOptimizerActionType(config, input);
    }
}
