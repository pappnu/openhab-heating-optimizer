package openhab.heating.utils;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class MathUtils {
    public static float lerp(float start, float end, float proportion) {
        return start + proportion * (end - start);
    }

    public static float remap(float value, float startOriginal, float endOriginal, float startTarget, float endTarget) {
        return startTarget + (value - startOriginal) * (endTarget - startTarget) / (endOriginal - startOriginal);
    }

    public static double interpolate(double[] original, double[] target, double value) {
        if (value <= original[0]) {
            return target[0];
        }
        if (value >= original[original.length - 1]) {
            return target[target.length - 1];
        }

        for (int i = 0; i < original.length - 1; i++) {
            double lower = original[i];
            double upper = original[i + 1];
            if (value >= lower && value <= upper) {
                double span = upper - lower;
                if (span == 0) {
                    return target[i];
                }
                double fraction = (value - lower) / span;
                return target[i] + (target[i + 1] - target[i]) * fraction;
            }
        }

        return target[target.length - 1];
    }
}
