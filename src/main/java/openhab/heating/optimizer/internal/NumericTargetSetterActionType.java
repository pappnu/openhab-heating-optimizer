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

    public NumericTargetSetterActionType(List<ConfigDescriptionParameter> configDescriptions, List<Input> inputs) {
        super(UID, configDescriptions, "Set numeric target", "Sets a numeric target.", null, Visibility.VISIBLE, inputs,
                null);
    }

    public static ActionType initialize() {
        var targetItem = ConfigDescriptionParameterBuilder.create(CONFIG_TARGET_ITEM, Type.TEXT).withRequired(true)
                .withContext("item").withLabel("Target item").build();
        var backupItem = ConfigDescriptionParameterBuilder.create(CONFIG_BACKUP_ITEM, Type.TEXT).withRequired(true)
                .withContext("item").withLabel("Backup item").build();
        var targetValue = ConfigDescriptionParameterBuilder.create(CONFIG_TARGET_VALUE, Type.DECIMAL)
                .withRequired(false).withLabel("Target value").build();
        var restore = ConfigDescriptionParameterBuilder.create(CONFIG_RESTORE, Type.BOOLEAN).withRequired(false)
                .withDefault("false").withLabel("Restore backed up value").build();

        var config = new ArrayList<ConfigDescriptionParameter>();
        Collections.addAll(config, targetItem, backupItem, targetValue, restore);
        return new NumericTargetSetterActionType(config, new ArrayList<>());
    }
}
