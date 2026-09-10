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
    // public Float avgTemperatureCeil;
    // public Float avgTemperatureFloor;
    public float minHeatingPeriod;
    // public Float maxNonHeatingPeriodCeil;
    // public Float maxNonHeatingPeriodFloor;
    public float maxSolvingTime;
    public float dhwHeatingPeriodLength;
    public String heatingControlItem = "";
    public String dhwControlItem = "";

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
}
