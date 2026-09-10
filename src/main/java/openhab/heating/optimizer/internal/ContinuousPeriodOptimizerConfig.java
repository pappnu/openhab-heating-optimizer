package openhab.heating.optimizer.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class ContinuousPeriodOptimizerConfig {
    public String persistenceServiceId = "";
    public String spotPricesItem = "";
    /**
     * In hours
     */
    public float periodLength;
    public String controlItem = "";
}
