package openhab.heating.utils;

import java.util.ArrayList;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class Cache {
    public static <R> Supplier<R> memoize(Supplier<R> function) {
        ArrayList<R> cache = new ArrayList<>(1);
        return () -> cache.size() > 0 ? cache.get(0) : function.get();
    }
}
