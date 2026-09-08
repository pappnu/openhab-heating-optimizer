package openhab.heating.optimizer.internal;

public class ContinuousPeriodOptimizerConfig {
    public String persistenceServiceId;
    public String spotPricesItem;
    /**
     * In hours
     */
    public float periodLength;
    public String controlItem;
}
