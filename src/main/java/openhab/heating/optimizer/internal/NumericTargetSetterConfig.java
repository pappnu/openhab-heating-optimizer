package openhab.heating.optimizer.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class NumericTargetSetterConfig {
    public String targetItem = "";
    public String backupItem = "";
    public double targetValue;
    public boolean restore;
}
