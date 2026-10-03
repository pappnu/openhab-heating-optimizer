package openhab.heating.optimizer.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class HeatingNeedModelingConfig {
    public String persistenceServiceId = "";
    public String heatpumpItem = "";
    public String outsideTemperatureItem = "";
    public String solarForecastItem = "";
    public String insideTemperatureItem = "";
    public int maxDataGapDays;
    public String outputItem = "";
}
