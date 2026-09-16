package openhab.heating.optimizer.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

import openhab.heating.utils.Config;

@NonNullByDefault
public class HeatingOptimizerConfig {
    public String persistenceServiceId = "";
    public String spotPricesItem = "";
    public String airTemperaturesItem = "";
    public String heatingTemperatures = "";
    public String heatingNeeds = "";
    public String gapTemperatures = "";
    public String gaps = "";
    public String maxStartsTemperatures = "";
    public String maxStarts = "";
    public float minHeatingPeriod;
    public float maxSolvingTime;
    public double priceFloorSoft;
    public double priceFloorHard;
    public String heatingControlItem = "";

    public double[] getHeatingTemperatures() {
        return Config.toDoubles(heatingTemperatures);
    }

    public double[] getHeatingNeeds() {
        return Config.toDoubles(heatingNeeds);
    }

    public double[] getGapTemperatures() {
        return Config.toDoubles(gapTemperatures);
    }

    public double[] getGaps() {
        return Config.toDoubles(gaps);
    }

    public double[] getMaxStartsTemperatures() {
        return Config.toDoubles(maxStartsTemperatures);
    }

    public double[] getMaxStarts() {
        return Config.toDoubles(maxStarts);
    }
}
