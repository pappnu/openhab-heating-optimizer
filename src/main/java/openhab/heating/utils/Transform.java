package openhab.heating.utils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;

public class Transform {

    public static double[] makePricesQuarterly(double[] prices, int timeStepMinutes, int stepsInFirstBatch) {
        if (prices.length == 0) {
            return new double[0];
        }
        if (timeStepMinutes <= 0) {
            return Arrays.copyOf(prices, prices.length);
        }
        if (timeStepMinutes != 15 && timeStepMinutes != 30 && timeStepMinutes != 60) {
            throw new IllegalArgumentException(
                    "Only the following step granularities are supported: 15, 30 and 60. Received: " + timeStepMinutes);
        }
        if (timeStepMinutes == 15) {
            return prices;
        }
        int factor = timeStepMinutes / 15;
        if (factor <= 1) {
            return Arrays.copyOf(prices, prices.length);
        }

        int firstBatchSize = stepsInFirstBatch % factor;
        if (firstBatchSize == 0) {
            firstBatchSize = factor;
        }
        double[] quarterHourPrices = new double[Math.max(0, (prices.length - 1) * factor + firstBatchSize)];
        if (quarterHourPrices.length > 0) {
            Arrays.fill(quarterHourPrices, 0, firstBatchSize, prices[0]);
            for (int i = 0; i < prices.length - 1; i++) {
                int startIdx = i * factor + firstBatchSize;
                Arrays.fill(quarterHourPrices, startIdx, startIdx + factor, prices[i + 1]);
            }
        }
        return quarterHourPrices;
    }

    public static double[] makePricesQuarterly(double[] prices, Duration timeStep, int stepsInFirstBatch) {
        return makePricesQuarterly(prices, Math.toIntExact(timeStep.toMinutes()), stepsInFirstBatch);
    }

    public static double[] makePricesQuarterly(double[] prices, int timeStepMinutes, ZonedDateTime start) {
        return makePricesQuarterly(prices, timeStepMinutes, 4 - start.getMinute() / 15);
    }

    public static double[] makePricesQuarterly(double[] prices, Duration timeStep, ZonedDateTime start) {
        return makePricesQuarterly(prices, Math.toIntExact(timeStep.toMinutes()), start);
    }
}
