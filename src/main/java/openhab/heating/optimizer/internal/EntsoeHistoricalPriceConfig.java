package openhab.heating.optimizer.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class EntsoeHistoricalPriceConfig {
    public String entsoeThingUID = "";
    public String persistenceServiceId = "";
    public String spotPricesItem = "";
    public int pastDays;
    public String startTime = "";
    public String endTime = "";
    public double priceMultiplier = 1.0;
    public String currency = "";
}
