package openhab.heating.utils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.Test;

@NonNullByDefault
public class MathUtilsTest {
    @Test
    public void testInterpolateValueBetweenReferencePoints() {
        double[] reference = { 1.0, 2.0, 3.0 };
        double[] values = { 10.0, 20.0, 30.0 };

        double interpolated = MathUtils.interpolate(reference, values, 2.5);

        assertTrue(Double.compare(interpolated, 25.0) == 0);
    }
}
