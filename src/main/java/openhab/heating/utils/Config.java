package openhab.heating.utils;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class Config {
    public static double[] toDoubles(String value) {
        if (value.isBlank()) {
            return new double[0];
        }
        return Arrays.stream(value.split(",")).mapToDouble(Double::parseDouble).toArray();
    }
}
