package openhab.heating.optimizer.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class MonthlyAveragePaidSpotPriceConfig {
    public String persistenceServiceId = "";
    public String monthlyCostsItem = "";
    public String consumptionItem = "";
    public String outputItem = "";
}
