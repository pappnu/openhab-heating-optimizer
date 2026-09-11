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

import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;

import openhab.heating.utils.Items;
import openhab.heating.utils.MathUtils;
import openhab.heating.utils.OrToolsNativeLoader;
import openhab.heating.utils.TimeUtils;
import openhab.heating.utils.Transform;

@NonNullByDefault
public class HeatingOptimizerActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    private final ItemRegistry itemRegistry;
    private final ScheduledExecutorService scheduler;
    private final Logger logger = LoggerFactory.getLogger(HeatingOptimizerActionHandler.class);

    public HeatingOptimizerActionHandler(Action module, ItemRegistry itemRegistry, ScheduledExecutorService scheduler) {
        super(module);
        this.itemRegistry = itemRegistry;
        this.scheduler = scheduler;
    }

    @Override
    public @Nullable Map<String, @Nullable Object> execute(Map<String, Object> context) {
        logger.info("Scheduling heating optimization");

        var conf = module.getConfiguration().as(HeatingOptimizerConfig.class);
        var spotPricesItem = Items.getItem(itemRegistry, conf.spotPricesItem);

        Items.waitForTomorrowPersistence(spotPricesItem, scheduler,
                (result) -> optimize(conf, spotPricesItem, result.now(), result.today(), result.tomorrow()));

        return null;
    }

    private void optimize(HeatingOptimizerConfig conf, Item spotPricesItem, ZonedDateTime now, ZonedDateTime today,
            ZonedDateTime tomorrow) {
        try {
            var startTime = System.nanoTime();

            var airTemperaturesItem = Items.getItem(itemRegistry, conf.airTemperaturesItem);
            var heatingControlItem = Items.getItem(itemRegistry, conf.heatingControlItem);

            var optStart = TimeUtils.truncateToNextQuarterHour(now);

            var pricesIter = PersistenceExtensions.getAllStatesBetween(spotPricesItem, optStart,
                    today.plusDays(2).minusSeconds(1), conf.persistenceServiceId);
            if (pricesIter == null) {
                throw new IllegalStateException("Spot prices iterator is null");
            }
            HistoricItem[] priceItems = StreamSupport.stream(pricesIter.spliterator(), false)
                    .toArray(HistoricItem[]::new);
            if (priceItems.length < 2) {
                throw new IllegalStateException("Not enough spot prices for optimization");
            }
            var secondToLastPrice = priceItems[priceItems.length - 2];
            var lastPrice = priceItems[priceItems.length - 1];

            var lastTime = lastPrice.getTimestamp();
            var timeStep = Duration.between(secondToLastPrice.getTimestamp(), lastTime);
            var optimizationEnd = lastTime.plus(timeStep);

            // Adjust price points to 15 minute frequency
            double[] prices = Transform.makePricesQuarterly(
                    Arrays.stream(priceItems).mapToDouble(item -> Items.getStateDouble(item.getState())).toArray(),
                    timeStep);

            timeStep = Duration.ofMinutes(15);

            // TODO ensure that we have air temp forecasts for the duration of spotPrices
            // e.g. try getting average value from the last spot price interval?
            var avgAirTempFirstPeriod = Items
                    .getStateFloat(PersistenceExtensions.averageBetween(airTemperaturesItem, today, tomorrow));

            var heatingTemps = conf.getHeatingTemperatures();
            var heatingNeeds = conf.getHeatingNeeds();
            var gapTemps = conf.getGapTemperatures();
            var maxGaps = conf.getGaps();
            var maxStartsTemps = conf.getMaxStartsTemperatures();
            var maxStarts = conf.getMaxStarts();
            int timeStepsInADay = 96;
            int timeStepsInFirstPeriod = TimeUtils.convertToTimeSteps(Duration.between(optStart, today.plusDays(1)),
                    timeStep);

            int heatingNeedDuringFirstPeriod;
            int heatingNeedDuringSecondPeriod = 0;
            int maxHeatingGapDuringFirstPeriod = TimeUtils
                    .convertHoursToTimeSteps(MathUtils.interpolate(gapTemps, maxGaps, avgAirTempFirstPeriod), timeStep);
            int maxHeatingGapDuringSecondPeriod = timeStepsInADay;
            int maxStartsDuringFirstPeriod = -1;
            int maxStartsDuringSecondPeriod = -1;

            var realizedHeatingDuringFirstPeriodState = PersistenceExtensions.sumBetween(heatingControlItem, today,
                    optStart.minusSeconds(1), conf.persistenceServiceId);
            if (realizedHeatingDuringFirstPeriodState != null) {
                heatingNeedDuringFirstPeriod = TimeUtils.convertHoursToTimeSteps(
                        MathUtils.interpolate(heatingTemps, heatingNeeds, avgAirTempFirstPeriod), timeStep)
                        - Items.getStateInt(realizedHeatingDuringFirstPeriodState);
            } else {
                // Assume that past heating has been distributed in proportion to passed time
                double nanosecondsInADay = 86400000000000d;
                heatingNeedDuringFirstPeriod = Math
                        .toIntExact(Math.round((Duration.between(optStart, tomorrow).toNanos() / nanosecondsInADay)
                                * TimeUtils.convertHoursToTimeSteps(
                                        MathUtils.interpolate(heatingTemps, heatingNeeds, avgAirTempFirstPeriod),
                                        timeStep)));
            }

            if (maxStarts.length > 0) {
                int realizedStartsDuringFirstPeriod = 0;
                var pastHeatingControl = PersistenceExtensions.getAllStatesBetween(heatingControlItem, today, optStart,
                        conf.persistenceServiceId);
                if (pastHeatingControl != null) {
                    int[] pastHeatings = StreamSupport.stream(pastHeatingControl.spliterator(), false)
                            .mapToInt(item -> Items.getStateInt(item.getState())).toArray();
                    for (int i = 1; i < pastHeatings.length; i++) {
                        if (pastHeatings[i - 1] == 0 && pastHeatings[i] == 1) {
                            realizedStartsDuringFirstPeriod++;
                        }
                    }
                }
                maxStartsDuringFirstPeriod = Math.toIntExact(
                        Math.round(MathUtils.interpolate(maxStartsTemps, maxStarts, avgAirTempFirstPeriod)));
                if (realizedStartsDuringFirstPeriod > 0) {
                    maxStartsDuringFirstPeriod = Math.max(0, maxStartsDuringFirstPeriod);
                }
            }

            // 1379 minutes == 22 hours 59 minutes
            if (optimizationEnd.isAfter(tomorrow.plusMinutes(1379))) {
                // Optimize tomorrow as well
                var avgAirTempSecondPeriod = Items.getStateFloat(
                        PersistenceExtensions.averageBetween(airTemperaturesItem, tomorrow, tomorrow.plusDays(1)));
                heatingNeedDuringSecondPeriod = TimeUtils.convertHoursToTimeSteps(
                        MathUtils.interpolate(heatingTemps, heatingNeeds, avgAirTempSecondPeriod), timeStep);
                maxHeatingGapDuringSecondPeriod = TimeUtils.convertHoursToTimeSteps(
                        MathUtils.interpolate(gapTemps, maxGaps, avgAirTempSecondPeriod), timeStep);
                maxStartsDuringSecondPeriod = maxStarts.length > 0
                        ? Math.toIntExact(
                                Math.round(MathUtils.interpolate(maxStartsTemps, maxStarts, avgAirTempSecondPeriod)))
                        : -1;
            }

            int totalHeatingNeed = heatingNeedDuringFirstPeriod + heatingNeedDuringSecondPeriod;

            // Check if heating should begin more early
            int firstGapSize = maxHeatingGapDuringFirstPeriod;
            var prevHeatingPeriod = Items.getClosestTimestampEqualsOne(heatingControlItem,
                    optStart.minus(timeStep.multipliedBy(maxHeatingGapDuringFirstPeriod)), optStart,
                    conf.persistenceServiceId);
            if (prevHeatingPeriod != null) {
                firstGapSize = maxHeatingGapDuringFirstPeriod
                        - TimeUtils.convertToTimeSteps(Duration.between(prevHeatingPeriod, optStart), timeStep);
            }

            // Check whether previous heating period ends in heating or not
            var stateBeforeOptStart = PersistenceExtensions.persistedState(heatingControlItem, optStart.minus(timeStep),
                    conf.persistenceServiceId);
            boolean prevEndsInHeating = (stateBeforeOptStart != null)
                    && Items.getStateInt(stateBeforeOptStart.getState()) == 1;

            var minHeatingPeriod = Math.min(prices.length,
                    TimeUtils.convertHoursToTimeSteps(conf.minHeatingPeriod, timeStep));
            var maxSolvingTime = Math.round(1000 * conf.maxSolvingTime);

            var result = optimizeHeatingWithLinearProgramming(prices, totalHeatingNeed, maxHeatingGapDuringFirstPeriod,
                    maxHeatingGapDuringSecondPeriod, maxStartsDuringFirstPeriod, maxStartsDuringSecondPeriod,
                    minHeatingPeriod, firstGapSize, timeStepsInFirstPeriod, heatingNeedDuringFirstPeriod,
                    prevEndsInHeating, maxSolvingTime);
            var heating = result.heating();
            if ((result.status() == MPSolver.ResultStatus.OPTIMAL || result.status() == MPSolver.ResultStatus.FEASIBLE)
                    && heating != null) {
                Items.persistControlPoints(heatingControlItem,
                        Arrays.stream(heating).mapToDouble(entry -> entry.solutionValue()).toArray(), optStart,
                        timeStep, conf.persistenceServiceId);

                var endTime = System.nanoTime();
                logger.info(String.format("Optimized heating in %.3f seconds",
                        Duration.ofNanos(endTime - startTime).toNanos() / 1000000000d));
            } else {
                logger.error("Optimization didn't end with a usable result " + result.status());
            }
        } catch (Exception e) {
            logger.error("Failed to optimize heating", e);
            throw e;
        }
    }

    protected static OptimizationResult optimizeHeatingWithLinearProgramming(double[] prices, int totalHeatingNeed,
            int maxHeatingGapDuringFirstPeriod, int maxHeatingGapDuringSecondPeriod, int maxStartsDuringFirstPeriod,
            int maxStartsDuringSecondPeriod, int minHeatingPeriod, int firstGapSize, int firstPeriodLength,
            int heatingNeedDuringFirstPeriod, boolean previousPeriodEndsInHeating, long solvingTimeLimit) {
        int heatingNeedDuringSecondPeriod = totalHeatingNeed - heatingNeedDuringFirstPeriod;

        OrToolsNativeLoader.load();

        // Define Inputs
        int n = prices.length;

        // Create the solver using
        MPSolver solver = MPSolver.createSolver("SCIP");
        if (solver == null) {
            return new OptimizationResult(MPSolver.ResultStatus.NOT_SOLVED, null, null);
        }

        // Decision Variables
        // heating[t]: 1 if heating is ON at time t, 0 otherwise
        // heatingStart[t]: 1 if heating starts at time t, 0 otherwise
        MPVariable[] heating = new MPVariable[n];
        MPVariable[] heatingStart = new MPVariable[n];
        for (int t = 0; t < n; t++) {
            heating[t] = solver.makeBoolVar("x_" + t);
            heatingStart[t] = solver.makeBoolVar("y_" + t);
        }

        // Objective Function: Minimize Sum(prices[t] * heating[t])
        MPObjective objective = solver.objective();
        for (int t = 0; t < n; t++) {
            objective.setCoefficient(heating[t], prices[t]);
        }
        objective.setMinimization();

        // region Constraints

        // At least totalHeatingNeed amount of heating is needed
        // Sum(heating[t]) >= totalHeatingNeed
        MPConstraint totalHeating = solver.makeConstraint(totalHeatingNeed, MPSolver.infinity(), "TotalHeatingNeed");
        for (int t = 0; t < n; t++) {
            totalHeating.setCoefficient(heating[t], 1);
        }

        if (firstPeriodLength < n) {
            // At least heatingNeedDuringFirstPeriod of heating is needed during first period (day)
            MPConstraint totalHeatingDuringFirstPeriod = solver.makeConstraint(heatingNeedDuringFirstPeriod,
                    MPSolver.infinity(), "TotalHeatingDuringFirstPeriod");
            for (int t = 0; t < firstPeriodLength; t++) {
                totalHeatingDuringFirstPeriod.setCoefficient(heating[t], 1);
            }

            // At least heatingNeedDuringSecondPeriod of heating is needed during second period (day)
            MPConstraint totalHeatingDuringSecondPeriod = solver.makeConstraint(heatingNeedDuringSecondPeriod,
                    MPSolver.infinity(), "TotalHeatingDuringSecondPeriod");
            for (int t = firstPeriodLength; t < n; t++) {
                totalHeatingDuringSecondPeriod.setCoefficient(heating[t], 1);
            }
        }

        // Maximum gap without heating
        // For every rolling window of size maxHeatingGap + 1, sum(heating) >= 1
        // The first gap's size is adjustable because heating that has happened before the
        // period that is currently being optimized influences it
        addMaxGapConstraint(0, Math.max(1, firstGapSize), solver, heating);
        int firstPeriodEnd = Math.max(0,
                Math.min(firstPeriodLength - Math.round(maxHeatingGapDuringFirstPeriod / 2f) - 1,
                        n - maxHeatingGapDuringFirstPeriod - 1));
        for (int t = 1; t <= firstPeriodEnd; t++) {
            addMaxGapConstraint(t, maxHeatingGapDuringFirstPeriod, solver, heating);
        }
        int secondPeriodEnd = n - maxHeatingGapDuringSecondPeriod - 1;
        for (int t = firstPeriodEnd; t <= secondPeriodEnd; t++) {
            addMaxGapConstraint(t, maxHeatingGapDuringSecondPeriod, solver, heating);
        }

        // Minimum continuous heating period
        // Link heatingStart to heating
        // heatingStart[t] >= heating[t] - heating[t-1]
        // heatingStart[t] - heating[t] + heating[t-1] >= 0
        if (!previousPeriodEndsInHeating) {
            MPConstraint start0 = solver.makeConstraint(0, MPSolver.infinity(), "StartLink_0");
            start0.setCoefficient(heatingStart[0], 1);
            start0.setCoefficient(heating[0], -1);
        }

        for (int t = 1; t < n; t++) {
            MPConstraint startT = solver.makeConstraint(0, MPSolver.infinity(), "StartLink_" + t);
            startT.setCoefficient(heatingStart[t], 1);
            startT.setCoefficient(heating[t], -1);
            startT.setCoefficient(heating[t - 1], 1);
        }

        // Limit number of starts
        // First period
        if (maxStartsDuringFirstPeriod > -1) {
            MPConstraint firstPeriodStartsLimit = solver.makeConstraint(0, maxStartsDuringFirstPeriod,
                    "MaxStartsInFirstPeriod");
            for (int t = 0; t < firstPeriodLength; t++) {
                firstPeriodStartsLimit.setCoefficient(heatingStart[t], 1);
            }
        }
        // Second period
        if (maxStartsDuringSecondPeriod > -1 && firstPeriodLength < n) {
            MPConstraint secondPeriodStartsLimit = solver.makeConstraint(0, maxStartsDuringSecondPeriod,
                    "MaxStartsInSecondPeriod");
            for (int t = firstPeriodLength; t < n; t++) {
                secondPeriodStartsLimit.setCoefficient(heatingStart[t], 1);
            }
        }

        // Force subsequent periods ON if a start occurred
        // Sum(heating[t+i]) >= minHeatingPeriod * heatingStart[t]
        // Sum(heating[t+i]) - minHeatingPeriod * heatingStart[t] >= 0
        for (int t = previousPeriodEndsInHeating ? 1 : 0; t <= n - minHeatingPeriod; t++) {
            MPConstraint minRun = solver.makeConstraint(0, MPSolver.infinity(), "MinRun_" + t);
            minRun.setCoefficient(heatingStart[t], -minHeatingPeriod);
            for (int i = 0; i < minHeatingPeriod; i++) {
                minRun.setCoefficient(heating[t + i], 1);
            }
        }

        // Prevent starting a heating cycle if there isn't enough time left in the array
        // heatingStart[t] == 0 for the last M_min - 1 periods
        for (int t = n - minHeatingPeriod + 1; t < n; t++) {
            MPConstraint noStart = solver.makeConstraint(0, 0, "NoStartEnd_" + t);
            noStart.setCoefficient(heatingStart[t], 1);
        }

        // endregion Constraints

        // Solve
        solver.setTimeLimit(solvingTimeLimit);
        MPSolver.ResultStatus resultStatus = solver.solve();

        return new OptimizationResult(resultStatus, objective, heating);
    }

    protected static void addMaxGapConstraint(int t, int gap, MPSolver solver, MPVariable[] heating) {
        MPConstraint maxGap = solver.makeConstraint(1, MPSolver.infinity(), "MaxGap_" + t);
        int end = Math.min(gap + 1, heating.length - 1);
        for (int i = 0; i < end; i++) {
            maxGap.setCoefficient(heating[t + i], 1);
        }
    }

    public record OptimizationResult(MPSolver.ResultStatus status, @Nullable MPObjective objective,
            MPVariable @Nullable [] heating) {
    }
}
