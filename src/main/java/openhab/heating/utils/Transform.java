package openhab.heating.utils;

import java.time.Duration;
import java.util.Arrays;

public class Transform {

    public static double[] makePricesQuarterly(double[] prices, int timeStepMinutes) {
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

        double[] quarterHourPrices = new double[prices.length * factor];
        for (int i = 0; i < prices.length; i++) {
            int startIdx = i * factor;
            Arrays.fill(quarterHourPrices, startIdx, startIdx + factor, prices[i]);
        }
        return quarterHourPrices;
    }

    public static double[] makePricesQuarterly(double[] prices, Duration timeStep) {
        return makePricesQuarterly(prices, Math.toIntExact(timeStep.toMinutes()));
    }
}
