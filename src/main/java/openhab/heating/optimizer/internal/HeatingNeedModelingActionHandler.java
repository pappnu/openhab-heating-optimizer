package openhab.heating.optimizer.internal;

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
import org.openhab.core.persistence.extensions.PersistenceExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import openhab.heating.utils.Items;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.regression.LinearModel;
import smile.regression.OLS;

@NonNullByDefault
public class HeatingNeedModelingActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    private static final int POLYNOMIAL_DEGREE = 2;

    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;
    private final Gson gson = new Gson();
    private final Logger logger = LoggerFactory.getLogger(HeatingNeedModelingActionHandler.class);

    public HeatingNeedModelingActionHandler(Action module, ItemRegistry itemRegistry, EventPublisher eventPublisher) {
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

        if (results.size() > POLYNOMIAL_DEGREE) {
            var model = fit(results, POLYNOMIAL_DEGREE);
            eventPublisher
                    .post(ItemEventFactory.createStateEvent(outputItem.getName(), new StringType(gson.toJson(model))));
            logger.info("Heating need model updated based on {} data points", results.size());
        } else {
            logger.error(
                    "Heating need modeling found {} valid records. At least {} records are required to fit the model.",
                    results.size(), POLYNOMIAL_DEGREE + 1);
        }
        return null;
    }

    protected static PolynomialHeatingNeedModel fit(List<DailyHeatingNeed> data, int degree) {
        if (data.size() <= degree) {
            throw new IllegalArgumentException(
                    "At least %d records are required to fit the heating need model. Found %d records"
                            .formatted(degree + 1, data.size()));
        }

        var temperatureDeltas = new double[data.size()];
        var solarExposures = new double[data.size()];
        var heatingHours = new double[data.size()];
        int sampleIndex = 0;
        for (var point : data) {
            temperatureDeltas[sampleIndex] = point.averageInsideOutsideTemperatureDelta();
            solarExposures[sampleIndex] = point.dailySolarExposure();
            heatingHours[sampleIndex] = point.heatpumpOnHours();
            sampleIndex++;
        }
        return fit(new double[][] { temperatureDeltas, solarExposures }, heatingHours, degree);
    }

    protected static PolynomialHeatingNeedModel fit(double[][] independentVariables, double[] dependentVariable,
            int degree) {
        var scaling = calculateScaling(independentVariables);
        var preparedData = preparePolynomialData(scale(independentVariables, scaling), degree);
        var trainingData = new double[preparedData.length][preparedData[0].length + 1];
        var columnNames = new String[trainingData[0].length];
        for (int featureIndex = 0; featureIndex < preparedData[0].length; featureIndex++) {
            columnNames[featureIndex] = "x" + featureIndex;
        }
        columnNames[preparedData[0].length] = "y";
        for (int sampleIndex = 0; sampleIndex < preparedData.length; sampleIndex++) {
            System.arraycopy(preparedData[sampleIndex], 0, trainingData[sampleIndex], 0,
                    preparedData[sampleIndex].length);
            trainingData[sampleIndex][preparedData[sampleIndex].length] = dependentVariable[sampleIndex];
        }

        LinearModel model = OLS.fit(Formula.lhs("y"), DataFrame.of(trainingData, columnNames));
        var coefficients = model.coefficients();
        var modelCoefficients = new double[coefficients.length + 1];
        modelCoefficients[0] = model.intercept();
        System.arraycopy(coefficients, 0, modelCoefficients, 1, coefficients.length);
        return new PolynomialHeatingNeedModel(modelCoefficients, independentVariables.length, degree, scaling.means,
                scaling.standardDeviations);
    }

    private static Scaling calculateScaling(double[][] independentVariables) {
        int variableCount = independentVariables.length;
        int sampleCount = independentVariables[0].length;
        var means = new double[variableCount];
        var standardDeviations = new double[variableCount];
        for (int variableIndex = 0; variableIndex < variableCount; variableIndex++) {
            for (double value : independentVariables[variableIndex]) {
                means[variableIndex] += value;
            }
            means[variableIndex] /= sampleCount;
            for (double value : independentVariables[variableIndex]) {
                double difference = value - means[variableIndex];
                standardDeviations[variableIndex] += difference * difference;
            }
            standardDeviations[variableIndex] = Math.sqrt(standardDeviations[variableIndex] / sampleCount);
            if (standardDeviations[variableIndex] == 0) {
                standardDeviations[variableIndex] = 1;
            }
        }
        return new Scaling(means, standardDeviations);
    }

    private static double[][] scale(double[][] independentVariables, Scaling scaling) {
        var scaled = new double[independentVariables.length][];
        for (int variableIndex = 0; variableIndex < independentVariables.length; variableIndex++) {
            scaled[variableIndex] = new double[independentVariables[variableIndex].length];
            for (int sampleIndex = 0; sampleIndex < scaled[variableIndex].length; sampleIndex++) {
                scaled[variableIndex][sampleIndex] = (independentVariables[variableIndex][sampleIndex]
                        - scaling.means[variableIndex]) / scaling.standardDeviations[variableIndex];
            }
        }
        return scaled;
    }

    protected static double[][] preparePolynomialData(double[][] independentVariables, int degree) {
        if (independentVariables.length == 0) {
            throw new IllegalArgumentException("At least one independent variable is required");
        }
        if (degree < 0) {
            throw new IllegalArgumentException("Polynomial degree must not be negative");
        }

        int sampleCount = independentVariables[0].length;
        for (var variable : independentVariables) {
            if (variable.length != sampleCount) {
                throw new IllegalArgumentException("All independent variables must have the same number of samples");
            }
        }

        var exponents = new ArrayList<int[]>();
        for (int totalDegree = 1; totalDegree <= degree; totalDegree++) {
            addExponents(exponents, new int[independentVariables.length], 0, totalDegree);
        }

        var preparedData = new double[sampleCount][exponents.size()];
        for (int sampleIndex = 0; sampleIndex < sampleCount; sampleIndex++) {
            for (int featureIndex = 0; featureIndex < exponents.size(); featureIndex++) {
                var featureExponents = exponents.get(featureIndex);
                double featureValue = 1;
                for (int variableIndex = 0; variableIndex < independentVariables.length; variableIndex++) {
                    featureValue *= Math.pow(independentVariables[variableIndex][sampleIndex],
                            featureExponents[variableIndex]);
                }
                preparedData[sampleIndex][featureIndex] = featureValue;
            }
        }
        return preparedData;
    }

    /**
     * Lists the exponents for all the variables in each term.
     * E.g. y = x + z + x^2 + xz + z^2 has exponents:
     * [(1,0), (0,1), (2,0), (1,1), (0,2)]
     * for variables x and z.
     */
    private static void addExponents(List<int[]> exponents, int[] current, int variableIndex, int remainingDegree) {
        if (variableIndex == current.length - 1) {
            current[variableIndex] = remainingDegree;
            exponents.add(current.clone());
            return;
        }

        for (int exponent = remainingDegree; exponent >= 0; exponent--) {
            current[variableIndex] = exponent;
            addExponents(exponents, current, variableIndex + 1, remainingDegree - exponent);
        }
    }

    protected static List<DailyHeatingNeed> collectData(Item heatpumpItem, Item outsideTemperatureItem,
            Item solarForecastItem, Item insideTemperatureItem, int maxDataGapDays, ZonedDateTime latestDay,
            String persistenceServiceId) {
        maxDataGapDays = Math.max(0, maxDataGapDays);

        var results = new ArrayList<DailyHeatingNeed>();
        var day = latestDay.truncatedTo(ChronoUnit.DAYS);
        int secondsInHour = 3600;
        var missingDays = 0;
        while (missingDays <= maxDataGapDays) {
            var end = day.plusDays(1);
            var insideAverage = PersistenceExtensions.averageBetween(insideTemperatureItem, day, end,
                    persistenceServiceId);
            var outsideAverage = PersistenceExtensions.averageBetween(outsideTemperatureItem, day, end,
                    persistenceServiceId);
            var solarExposure = PersistenceExtensions.riemannSumBetween(solarForecastItem, day, end,
                    persistenceServiceId);
            var heatingSeconds = PersistenceExtensions.riemannSumBetween(heatpumpItem, day, end, persistenceServiceId);

            if (insideAverage == null || outsideAverage == null || solarExposure == null || heatingSeconds == null) {
                missingDays++;
            } else {
                missingDays = 0;
                var averageInside = Items.getStateDouble(insideAverage);
                var averageOutside = Items.getStateDouble(outsideAverage);
                var temperatureDelta = Math.max(averageInside - averageOutside, 0);
                results.add(new DailyHeatingNeed(day, Items.getStateDouble(heatingSeconds) / secondsInHour,
                        temperatureDelta, Items.getStateDouble(solarExposure)));
            }
            day = day.minusDays(1);
        }
        return results;
    }

    public record DailyHeatingNeed(ZonedDateTime day, double heatpumpOnHours,
            double averageInsideOutsideTemperatureDelta, double dailySolarExposure) {
    }

    private record Scaling(double[] means, double[] standardDeviations) {
    }

    public record PolynomialHeatingNeedModel(double[] coefficients, int variableCount, int degree, double[] means,
            double[] standardDeviations) {
        public double estimate(double[] independentVariables) {
            if (independentVariables.length != variableCount) {
                throw new IllegalArgumentException("The number of variables must match the fitted model");
            }

            var variables = new double[variableCount][1];
            for (int variableIndex = 0; variableIndex < variableCount; variableIndex++) {
                variables[variableIndex][0] = (independentVariables[variableIndex] - means[variableIndex])
                        / standardDeviations[variableIndex];
            }
            var preparedData = preparePolynomialData(variables, degree)[0];
            double result = coefficients[0];
            for (int featureIndex = 0; featureIndex < preparedData.length; featureIndex++) {
                result += coefficients[featureIndex + 1] * preparedData[featureIndex];
            }
            return result;
        }
    }
}
