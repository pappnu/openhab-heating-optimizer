package openhab.heating.utils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class TimeUtils {
    public static ZonedDateTime truncateToNextQuarterHour(ZonedDateTime dateTime) {
        // Clear seconds and nanoseconds
        ZonedDateTime base = dateTime.truncatedTo(ChronoUnit.MINUTES);

        // Find minutes past the last quarter hour (0-14)
        int minutesPastQuarter = base.getMinute() % 15;

        // If exactly on a quarter hour, optionally skip adding minutes (or add 15 if you always want the next)
        int minutesToAdd = (minutesPastQuarter == 0) ? 0 : (15 - minutesPastQuarter);

        return base.plusMinutes(minutesToAdd);
    }

    public static int convertHoursToTimeSteps(float hours, Duration stepLength) {
        float step = stepLength.toSeconds() / 3600f;
        return Math.round(hours / step);
    }

    public static int convertHoursToTimeSteps(double hours, Duration stepLength) {
        return convertHoursToTimeSteps((float) hours, stepLength);
    }

    public static int convertToTimeSteps(Duration duration, Duration stepLenght) {
        return Math.toIntExact(duration.dividedBy(stepLenght));
    }
}
