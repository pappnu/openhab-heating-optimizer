package openhab.heating.optimizer.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.automation.type.ModuleType;
import org.openhab.core.automation.type.ModuleTypeProvider;
import org.openhab.core.common.registry.ProviderChangeListener;
import org.osgi.service.component.annotations.Component;

@NonNullByDefault
@Component(immediate = true, service = { ModuleTypeProvider.class })
public class HeatingOptimizerModuleTypeProvider implements ModuleTypeProvider {

    protected Map<String, ModuleType> providedModuleTypes;

    public HeatingOptimizerModuleTypeProvider() {
        providedModuleTypes = new HashMap<>();
        providedModuleTypes.put(HeatingOptimizerActionType.UID, HeatingOptimizerActionType.initialize());
        providedModuleTypes.put(ContinuousPeriodOptimizerActionType.UID,
                ContinuousPeriodOptimizerActionType.initialize());
    }

    @Override
    public void addProviderChangeListener(ProviderChangeListener<ModuleType> listener) {
        // this provider does not change
    }

    @Override
    public Collection<ModuleType> getAll() {
        return Collections.unmodifiableCollection(providedModuleTypes.values());
    }

    @Override
    public void removeProviderChangeListener(ProviderChangeListener<ModuleType> arg0) {
        // this provider does not change
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> @Nullable T getModuleType(String uid, @Nullable Locale locale) {
        return (T) providedModuleTypes.get(uid);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ModuleType> Collection<T> getModuleTypes(@Nullable Locale locale) {
        return (Collection<T>) providedModuleTypes.values();
    }
}
