package openhab.heating.utils;

import java.time.Duration;
import java.util.Arrays;

public class Transform {

    public static double[] makePricesQuarterly(double[] prices, int timeStepMinutes, int stepsInFirstBatch) {
        if (prices.length == 0) {
            return new double[0];
        }
        if (timeStepMinutes <= 0) {
            return Arrays.copyOf(prices, prices.length);
        }
        int factor = timeStepMinutes / 15;
        if (factor <= 1) {
            return Arrays.copyOf(prices, prices.length);
        }
        if (timeStepMinutes % 15 != 0) {
            throw new IllegalArgumentException(
                    "Only quarterly time steps are supported: 15, 30, 45 and so on. Received: " + timeStepMinutes);
        }

        double[] quarterHourPrices = new double[Math.max(0, (prices.length - 1) * factor + stepsInFirstBatch)];
        if (quarterHourPrices.length > 0) {
            Arrays.fill(quarterHourPrices, 0, stepsInFirstBatch, prices[0]);
            for (int i = 1; i < prices.length; i++) {
                int startIdx = i * factor;
                Arrays.fill(quarterHourPrices, startIdx, startIdx + factor, prices[i]);
            }
        }
        return quarterHourPrices;
    }

    public static double[] makePricesQuarterly(double[] prices, Duration timeStep, int stepsInFirstBatch) {
        return makePricesQuarterly(prices, Math.toIntExact(timeStep.toMinutes()), stepsInFirstBatch);
    }
}
