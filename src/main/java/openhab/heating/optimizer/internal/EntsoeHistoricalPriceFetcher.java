package openhab.heating.optimizer.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import javax.measure.Unit;
import javax.measure.quantity.Energy;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.unit.Units;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;

import openhab.heating.utils.TimeUtils;

@NonNullByDefault
public class EntsoeHistoricalPriceFetcher {
    private static final String API_URL = "https://web-api.tp.entsoe.eu/api?";
    private static final int MAX_TIME_SERIES_PER_RESPONSE = 100;
    private static final Map<String, Unit<Energy>> ENERGY_UNIT_MAP = Map.of("mwh", Units.MEGAWATT_HOUR, "kwh",
            Units.KILOWATT_HOUR, "wh", Units.WATT_HOUR);
    private static final Duration DEFAULT_TARGET_RESOLUTION = Objects.requireNonNull(Duration.ofMinutes(15));
    private static final DateTimeFormatter REQUEST_TIME_FORMAT = Objects
            .requireNonNull(DateTimeFormatter.ofPattern("yyyyMMddHHmm").withZone(ZoneOffset.UTC));
    private static final HttpClient HTTP_CLIENT = Objects.requireNonNull(HttpClient.newHttpClient());
    /**
     * Parser for ISO-8601 strings without seconds part, e.g. 2024-07-27T22:00Z
     */
    public static final DateTimeFormatter ISO_OPTIONAL_SECONDS = Objects
            .requireNonNull(new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0).appendOffsetId().toFormatter());
    private static final XmlMapper XML_MAPPER = createXmlMapper();

    public static PriceSeries fetch(String securityToken, String area, Instant start, Instant end,
            @Nullable Duration targetResolution, int timeoutSeconds) throws IOException, InterruptedException {
        if (securityToken.isBlank() || area.isBlank()) {
            throw new IllegalArgumentException("Entso-E security token and area must be configured");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Start time must be before end time");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Request timeout must be positive");
        }

        // Fetch prices in batches of at max 1 year
        var result = new TreeMap<Instant, Double>();
        String responseCurrency = "";
        boolean hasResponseCurrency = false;
        var cursor = start;
        while (cursor.isBefore(end)) {
            var requestEnd = nextRequestEnd(cursor, end);
            int offset = 0;
            while (true) {
                var response = request(securityToken, area, cursor, requestEnd, offset, timeoutSeconds);
                var page = parseResponse(response, targetResolution);
                if (page.timeSeriesCount == 0) {
                    if (offset == 0) {
                        throw new IOException("Entso-E response contains no price points");
                    }
                    break;
                }
                if (hasResponseCurrency && !responseCurrency.equals(page.currency)) {
                    throw new IOException("Entso-E responses contain prices in different currencies");
                }
                responseCurrency = page.currency;
                hasResponseCurrency = true;
                result.putAll(page.prices);
                if (!hasMoreTimeSeries(page.timeSeriesCount)) {
                    break;
                }
                offset += MAX_TIME_SERIES_PER_RESPONSE;
            }
            cursor = requestEnd;
        }
        return new PriceSeries(result, responseCurrency);
    }

    static boolean hasMoreTimeSeries(int timeSeriesCount) {
        return timeSeriesCount >= MAX_TIME_SERIES_PER_RESPONSE;
    }

    static Instant nextRequestEnd(Instant cursor, Instant end) {
        var nextYear = cursor.atZone(ZoneId.systemDefault()).plusYears(1).toInstant();
        return nextYear.isBefore(end) ? nextYear : end;
    }

    private static String request(String securityToken, String area, Instant start, Instant end, int offset,
            int timeoutSeconds) throws IOException, InterruptedException {
        var url = API_URL + "securityToken=" + encode(securityToken) + "&documentType=A44&in_Domain=" + encode(area)
                + "&out_Domain=" + encode(area) + "&periodStart=" + REQUEST_TIME_FORMAT.format(start) + "&periodEnd="
                + REQUEST_TIME_FORMAT.format(end) + "&offset=" + offset;
        var request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(timeoutSeconds)).GET().build();
        var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Entso-E request failed with HTTP status " + response.statusCode());
        }
        var body = response.body();
        if (body == null) {
            throw new IOException("Entso-E response's body is null");
        }
        return body;
    }

    static ParsedResponse parseResponse(String xml, @Nullable Duration targetResolution) throws IOException {
        final PublicationMarketDocument document;
        try {
            document = Objects.requireNonNull(XML_MAPPER.readValue(xml, PublicationMarketDocument.class));
        } catch (NullPointerException | JsonProcessingException e) {
            throw new IOException("Entso-E returned invalid XML", e);
        }

        validateTimeInterval(document.timeInterval);

        int timeSeriesCount = document.timeSeries.size();
        if (timeSeriesCount == 0) {
            return new ParsedResponse(new TreeMap<>(), "", timeSeriesCount);
        }

        String responseCurrency = document.timeSeries.isEmpty() ? "" : document.timeSeries.get(0).currency;
        var weightedPrices = new TreeMap<Instant, WeightedPrice>();
        for (var timeSeries : document.timeSeries) {
            if (!responseCurrency.equals(timeSeries.currency)) {
                throw new IOException("Entso-E response contains prices in different currencies");
            }
            double unitMultiplier = getPerKilowattHourMultiplier(timeSeries.priceMeasureUnit);
            for (var period : timeSeries.periods) {
                parsePeriod(period, unitMultiplier, targetResolution, weightedPrices);
            }
        }
        if (weightedPrices.isEmpty()) {
            throw new IOException("Entso-E response contains no price points");
        }

        var prices = new TreeMap<Instant, Double>();
        weightedPrices.forEach((time, weightedPrice) -> prices.put(time, weightedPrice.average));
        return new ParsedResponse(prices, responseCurrency, timeSeriesCount);
    }

    record ParsedResponse(TreeMap<Instant, Double> prices, String currency, int timeSeriesCount) {
    }

    public record PriceSeries(TreeMap<Instant, Double> prices, String currency) {
    }

    private static void validateTimeInterval(EntsoeTimeInterval interval) throws IOException {
        if (!TimeUtils.isAtTopOfHour(interval.start) || !TimeUtils.isAtTopOfHour(interval.end)) {
            throw new IOException("Entso-E interval start and end must be at the top of the hour");
        }
    }

    private static void parsePeriod(EntsoePeriod period, double unitMultiplier, @Nullable Duration targetResolution,
            TreeMap<Instant, WeightedPrice> weightedPrices) throws IOException {
        var interval = period.timeInterval;
        var periodStart = interval.start;
        var periodEnd = interval.end;
        var sourceResolution = period.resolution;
        var outputResolution = targetResolution == null ? DEFAULT_TARGET_RESOLUTION : targetResolution;
        if (!periodStart.isBefore(periodEnd)) {
            throw new IOException("Entso-E period start must be before its end");
        }
        if (!areEvenlyConvertible(sourceResolution, outputResolution)) {
            throw new IOException("Entso-E source and target resolutions must be evenly convertible");
        }

        int position = 1;
        Instant pointStart = periodStart;
        Double previousPrice = null;
        for (var point : period.points) {
            while (position < point.position) {
                Instant pointEnd = Objects.requireNonNull(pointStart.plus(sourceResolution));
                if (previousPrice != null) {
                    addWeightedPrice(pointStart, pointEnd, previousPrice, outputResolution, weightedPrices);
                }
                position++;
                pointStart = pointEnd;
            }

            double adjustedPrice = point.priceAmount * unitMultiplier;
            Instant pointEnd = Objects.requireNonNull(pointStart.plus(sourceResolution));
            addWeightedPrice(pointStart, pointEnd, adjustedPrice, outputResolution, weightedPrices);
            position++;
            pointStart = pointEnd;
            previousPrice = adjustedPrice;
        }
    }

    private static boolean areEvenlyConvertible(Duration sourceResolution, Duration targetResolution) {
        if (sourceResolution.isNegative() || sourceResolution.isZero() || targetResolution.isNegative()
                || targetResolution.isZero()) {
            return false;
        }
        long sourceSeconds = sourceResolution.getSeconds();
        long targetSeconds = targetResolution.getSeconds();
        return sourceSeconds % targetSeconds == 0 || targetSeconds % sourceSeconds == 0;
    }

    /** Handles conversions between resolutions, e.g. 15m -> 60m or vice versa. */
    private static void addWeightedPrice(Instant start, Instant end, double price, Duration resolution,
            TreeMap<Instant, WeightedPrice> weightedPrices) {
        long resolutionSeconds = resolution.getSeconds();
        var cursor = start;
        while (cursor.isBefore(end)) {
            // When decreasing the number of points bucketEpoch is the same for multiple different start times, e.g.
            // 10:00, 10:15, 10:30 and 10:45 when using 60 minute resolution
            long bucketEpoch = Math.floorDiv(cursor.getEpochSecond(), resolutionSeconds) * resolutionSeconds;
            var bucketStart = Instant.ofEpochSecond(bucketEpoch);
            var bucketEnd = bucketStart.plusSeconds(resolutionSeconds);
            var overlapEnd = end.isBefore(bucketEnd) ? end : bucketEnd;
            double overlapSeconds = Duration.between(cursor, overlapEnd).toNanos() / 1_000_000_000.0;
            weightedPrices.computeIfAbsent(bucketStart, key -> new WeightedPrice()).add(price, overlapSeconds);
            cursor = Objects.requireNonNull(overlapEnd);
        }
    }

    private static final class WeightedPrice {
        private double average;
        private double totalSeconds;

        private void add(double price, double seconds) {
            double newTotalSeconds = totalSeconds + seconds;
            average = (average * totalSeconds + price * seconds) / newTotalSeconds;
            totalSeconds = newTotalSeconds;
        }
    }

    private static double getPerKilowattHourMultiplier(String unit) throws IOException {
        @Nullable
        Unit<Energy> entsoeUnit = ENERGY_UNIT_MAP.get(unit.toLowerCase());
        if (entsoeUnit == null) {
            throw new IOException("Entso-E returned an unsupported energy unit: " + unit);
        }
        return Units.KILOWATT_HOUR.getConverterTo(entsoeUnit).convert(1.0);
    }

    private static String encode(String value) {
        return Objects.requireNonNull(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }

    private static XmlMapper createXmlMapper() {
        var mapper = new XmlMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        var javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(Instant.class, InstantDeserializer.INSTANT.withDateFormat(ISO_OPTIONAL_SECONDS));
        mapper.registerModule(javaTimeModule);

        return mapper;
    }

    @JacksonXmlRootElement(localName = "Publication_MarketDocument")
    public record PublicationMarketDocument(
            @JacksonXmlProperty(localName = "period.timeInterval") EntsoeTimeInterval timeInterval,
            @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "TimeSeries") List<EntsoeTimeSeries> timeSeries) {
    }

    public record EntsoeTimeSeries(@JacksonXmlProperty(localName = "currency_Unit.name") String currency,
            @JacksonXmlProperty(localName = "price_Measure_Unit.name") String priceMeasureUnit,
            @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "Period") List<EntsoePeriod> periods) {
    }

    public record EntsoePeriod(EntsoeTimeInterval timeInterval, Duration resolution,
            @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "Point") List<EntsoePoint> points) {
    }

    public record EntsoeTimeInterval(Instant start, Instant end) {
    }

    public record EntsoePoint(int position, @JacksonXmlProperty(localName = "price.amount") Double priceAmount) {
    }
}
