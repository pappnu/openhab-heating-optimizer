package openhab.heating.optimizer.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.Action;
import org.openhab.core.automation.Module;
import org.openhab.core.automation.handler.BaseModuleHandlerFactory;
import org.openhab.core.automation.handler.ModuleHandler;
import org.openhab.core.automation.handler.ModuleHandlerFactory;
import org.openhab.core.common.ThreadPoolManager;
import org.openhab.core.items.ItemRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
@Component(service = { ModuleHandlerFactory.class })
public class HeatingOptimizerModuleHandlerFactory extends BaseModuleHandlerFactory {
    public static final String MODULE_HANDLER_FACTORY_NAME = "[HeatingOptimizerModuleHandlerFactory]";
    protected static final Collection<String> TYPES;

    protected final Logger logger = LoggerFactory.getLogger(HeatingOptimizerModuleHandlerFactory.class);

    @Reference
    private @NonNullByDefault({}) ItemRegistry itemRegistry;

    private final ScheduledExecutorService scheduler = ThreadPoolManager
            .getScheduledPool(HeatingOptimizerModuleHandlerFactory.class.getName());

    static {
        List<String> temp = new ArrayList<String>();
        temp.add(HeatingOptimizerActionType.UID);
        temp.add(ContinuousPeriodOptimizerActionType.UID);
        TYPES = Collections.unmodifiableCollection(temp);
    }

    @Override
    public Collection<String> getTypes() {
        return TYPES;
    }

    @Override
    protected @Nullable ModuleHandler internalCreate(Module module, String ruleUID) {
        ModuleHandler moduleHandler = null;
        String moduleTypeUID = module.getTypeUID();
        switch (moduleTypeUID) {
            case HeatingOptimizerActionType.UID:
                moduleHandler = new HeatingOptimizerActionHandler((Action) module, itemRegistry, scheduler);
                break;
            case ContinuousPeriodOptimizerActionType.UID:
                moduleHandler = new ContinuousPeriodOptimizerActionHandler((Action) module, itemRegistry, scheduler);
                break;

            default:
                logger.warn(MODULE_HANDLER_FACTORY_NAME + " Unsupported moduleHandler: {}", moduleTypeUID);
                break;
        }
        return moduleHandler;
    }
}
