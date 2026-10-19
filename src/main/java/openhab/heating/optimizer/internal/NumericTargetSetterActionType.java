package openhab.heating.optimizer.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.automation.Visibility;
import org.openhab.core.automation.type.ActionType;
import org.openhab.core.automation.type.Input;
import org.openhab.core.config.core.ConfigDescriptionParameter;
import org.openhab.core.config.core.ConfigDescriptionParameter.Type;
import org.openhab.core.config.core.ConfigDescriptionParameterBuilder;

@NonNullByDefault
public class NumericTargetSetterActionType extends ActionType {
    public static final String UID = "NumericTargetSetter";

    public static final String CONFIG_TARGET_ITEM = "targetItem";
    public static final String CONFIG_BACKUP_ITEM = "backupItem";
    public static final String CONFIG_TARGET_VALUE = "targetValue";
    public static final String CONFIG_RESTORE = "restore";
    public static final String CONFIG_SKIP_IF_CURRENT_TARGET_IS_BELOW_TARGET_VALUE = "skipIfCurrentTargetIsBelowTargetValue";

    public NumericTargetSetterActionType(List<ConfigDescriptionParameter> configDescriptions, List<Input> inputs) {
        super(UID, configDescriptions, "Set numeric target", "Sets a numeric target.", null, Visibility.VISIBLE, inputs,
                null);
    }

    public static ActionType initialize() {
        final ConfigDescriptionParameter targetItem = ConfigDescriptionParameterBuilder
                .create(CONFIG_TARGET_ITEM, Type.TEXT).withRequired(true).withContext("item").withLabel("Target item")
                .build();
        final ConfigDescriptionParameter backupItem = ConfigDescriptionParameterBuilder
                .create(CONFIG_BACKUP_ITEM, Type.TEXT).withRequired(true).withContext("item").withLabel("Backup item")
                .build();
        final ConfigDescriptionParameter targetValue = ConfigDescriptionParameterBuilder
                .create(CONFIG_TARGET_VALUE, Type.DECIMAL).withRequired(false).withLabel("Target value").build();
        final ConfigDescriptionParameter restore = ConfigDescriptionParameterBuilder
                .create(CONFIG_RESTORE, Type.BOOLEAN).withRequired(false).withDefault("false")
                .withLabel("Restore backed up value").build();
        final ConfigDescriptionParameter skipIfCurrentTargetIsBelowTargetValue = ConfigDescriptionParameterBuilder
                .create(CONFIG_SKIP_IF_CURRENT_TARGET_IS_BELOW_TARGET_VALUE, Type.BOOLEAN).withRequired(false)
                .withDefault("false").withLabel("Skip if current target is below target value").build();

        var config = new ArrayList<ConfigDescriptionParameter>();
        Collections.addAll(config, targetItem, backupItem, targetValue, restore, skipIfCurrentTargetIsBelowTargetValue);
        return new NumericTargetSetterActionType(config, new ArrayList<>());
    }
}
