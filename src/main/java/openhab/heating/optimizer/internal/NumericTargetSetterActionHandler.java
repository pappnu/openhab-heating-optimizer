package openhab.heating.optimizer.internal;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.handler.ActionHandler;
import org.openhab.core.automation.handler.BaseModuleHandler;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.types.DecimalType;

import openhab.heating.utils.Items;

@NonNullByDefault
public class NumericTargetSetterActionHandler extends BaseModuleHandler<Action> implements ActionHandler {
    private final ItemRegistry itemRegistry;
    private final EventPublisher eventPublisher;

    public NumericTargetSetterActionHandler(Action module, ItemRegistry itemRegistry, EventPublisher eventPublisher) {
        super(module);
        this.itemRegistry = itemRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public @Nullable Map<String, @Nullable Object> execute(Map<String, Object> context) {
        var config = module.getConfiguration().as(NumericTargetSetterConfig.class);
        var targetItem = Items.getItem(itemRegistry, config.targetItem);
        var backupItem = Items.getItem(itemRegistry, config.backupItem);

        if (config.restore) {
            var backupState = Items.getStateDecimal(backupItem.getState());
            eventPublisher.post(ItemEventFactory.createStateEvent(targetItem.getName(), backupState));
        } else {
            var currentTargetState = Items.getStateDecimal(targetItem.getState());
            eventPublisher.post(ItemEventFactory.createStateEvent(backupItem.getName(), currentTargetState));
            eventPublisher
                    .post(ItemEventFactory.createStateEvent(targetItem.getName(), new DecimalType(config.targetValue)));
        }

        return null;
    }
}
