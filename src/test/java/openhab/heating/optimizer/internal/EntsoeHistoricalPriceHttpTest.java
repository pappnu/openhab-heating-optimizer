package openhab.heating.optimizer.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

public class EntsoeHistoricalPriceHttpTest {
    private static final String FINLAND_AREA = "10YFI-1--------U";
    private static final ZoneId FINLAND_ZONE = ZoneId.of("Europe/Helsinki");

    @Test
    public void parsesAndResamplesHistoricalXml() throws IOException {
        String xml = """
                <?xml version="1.0" encoding="utf-8"?>
                <Publication_MarketDocument xmlns="urn:iec62325.351:tc57wg16:451-3:publicationdocument:7:3">
                    <mRID>05e8314f237d4998b65a04c538fc8ec9</mRID>
                    <revisionNumber>1</revisionNumber>
                    <type>A44</type>
                    <sender_MarketParticipant.mRID codingScheme="A01">10X1001A1001A450</sender_MarketParticipant.mRID>
                    <sender_MarketParticipant.marketRole.type>A32</sender_MarketParticipant.marketRole.type>
                    <receiver_MarketParticipant.mRID codingScheme="A01">10X1001A1001A450</receiver_MarketParticipant.mRID>
                    <receiver_MarketParticipant.marketRole.type>A33</receiver_MarketParticipant.marketRole.type>
                    <createdDateTime>2024-10-01T15:07:18Z</createdDateTime>
                    <period.timeInterval>
                        <start>2024-07-27T22:00Z</start>
                        <end>2024-07-28T22:00Z</end>
                    </period.timeInterval>
                    <TimeSeries>
                        <mRID>1</mRID>
                        <auction.type>A01</auction.type>
                        <businessType>A62</businessType>
                        <in_Domain.mRID codingScheme="A01">10YAT-APG------L</in_Domain.mRID>
                        <out_Domain.mRID codingScheme="A01">10YAT-APG------L</out_Domain.mRID>
                        <contract_MarketAgreement.type>A01</contract_MarketAgreement.type>
                        <currency_Unit.name>EUR</currency_Unit.name>
                        <price_Measure_Unit.name>MWH</price_Measure_Unit.name>
                        <classificationSequence_AttributeInstanceComponent.position>2</classificationSequence_AttributeInstanceComponent.position>
                        <curveType>A03</curveType>
                        <Period>
                            <timeInterval>
                                <start>2024-07-27T22:00Z</start>
                                <end>2024-07-28T22:00Z</end>
                            </timeInterval>
                            <resolution>PT15M</resolution>
                            <Point>
                                <position>1</position>
                                <price.amount>104.98</price.amount>
                            </Point>
                            <Point>
                                <position>3</position>
                                <price.amount>79.98</price.amount>
                            </Point>
                            <Point>
                                <position>4</position>
                                <price.amount>71.3</price.amount>
                            </Point>
                            <Point>
                                <position>5</position>
                                <price.amount>89.19</price.amount>
                            </Point>
                            <Point>
                                <position>6</position>
                                <price.amount>82.16</price.amount>
                            </Point>
                            <Point>
                                <position>7</position>
                                <price.amount>72.42</price.amount>
                            </Point>
                            <Point>
                                <position>8</position>
                                <price.amount>54.98</price.amount>
                            </Point>
                            <Point>
                                <position>9</position>
                                <price.amount>80.99</price.amount>
                            </Point>
                            <Point>
                                <position>10</position>
                                <price.amount>79.98</price.amount>
                            </Point>
                            <Point>
                                <position>11</position>
                                <price.amount>63.6</price.amount>
                            </Point>
                            <Point>
                                <position>12</position>
                                <price.amount>54.98</price.amount>
                            </Point>
                            <Point>
                                <position>13</position>
                                <price.amount>79.98</price.amount>
                            </Point>
                            <Point>
                                <position>15</position>
                                <price.amount>55.02</price.amount>
                            </Point>
                            <Point>
                                <position>16</position>
                                <price.amount>50.45</price.amount>
                            </Point>
                            <Point>
                                <position>17</position>
                                <price.amount>74.98</price.amount>
                            </Point>
                            <Point>
                                <position>18</position>
                                <price.amount>69.97</price.amount>
                            </Point>
                            <Point>
                                <position>19</position>
                                <price.amount>54.98</price.amount>
                            </Point>
                            <Point>
                                <position>20</position>
                                <price.amount>52.6</price.amount>
                            </Point>
                            <Point>
                                <position>21</position>
                                <price.amount>65.58</price.amount>
                            </Point>
                            <Point>
                                <position>22</position>
                                <price.amount>64.56</price.amount>
                            </Point>
                            <Point>
                                <position>23</position>
                                <price.amount>51.13</price.amount>
                            </Point>
                            <Point>
                                <position>24</position>
                                <price.amount>50.04</price.amount>
                            </Point>
                            <Point>
                                <position>25</position>
                                <price.amount>60.43</price.amount>
                            </Point>
                            <Point>
                                <position>26</position>
                                <price.amount>55.02</price.amount>
                            </Point>
                            <Point>
                                <position>27</position>
                                <price.amount>54.98</price.amount>
                            </Point>
                            <Point>
                                <position>28</position>
                                <price.amount>53.89</price.amount>
                            </Point>
                            <Point>
                                <position>29</position>
                                <price.amount>74.91</price.amount>
                            </Point>
                            <Point>
                                <position>30</position>
                                <price.amount>52.58</price.amount>
                            </Point>
                            <Point>
                                <position>31</position>
                                <price.amount>50.05</price.amount>
                            </Point>
                            <Point>
                                <position>32</position>
                                <price.amount>47.51</price.amount>
                            </Point>
                            <Point>
                                <position>33</position>
                                <price.amount>75.39</price.amount>
                            </Point>
                            <Point>
                                <position>34</position>
                                <price.amount>56.39</price.amount>
                            </Point>
                            <Point>
                                <position>35</position>
                                <price.amount>36.54</price.amount>
                            </Point>
                            <Point>
                                <position>36</position>
                                <price.amount>32.57</price.amount>
                            </Point>
                            <Point>
                                <position>37</position>
                                <price.amount>64.94</price.amount>
                            </Point>
                            <Point>
                                <position>38</position>
                                <price.amount>40.05</price.amount>
                            </Point>
                            <Point>
                                <position>39</position>
                                <price.amount>12.58</price.amount>
                            </Point>
                            <Point>
                                <position>40</position>
                                <price.amount>-30.71</price.amount>
                            </Point>
                            <Point>
                                <position>41</position>
                                <price.amount>64.95</price.amount>
                            </Point>
                            <Point>
                                <position>42</position>
                                <price.amount>25.04</price.amount>
                            </Point>
                            <Point>
                                <position>43</position>
                                <price.amount>0.21</price.amount>
                            </Point>
                            <Point>
                                <position>44</position>
                                <price.amount>-39.45</price.amount>
                            </Point>
                            <Point>
                                <position>45</position>
                                <price.amount>47.42</price.amount>
                            </Point>
                            <Point>
                                <position>46</position>
                                <price.amount>6.98</price.amount>
                            </Point>
                            <Point>
                                <position>47</position>
                                <price.amount>-13.65</price.amount>
                            </Point>
                            <Point>
                                <position>48</position>
                                <price.amount>-18.24</price.amount>
                            </Point>
                            <Point>
                                <position>49</position>
                                <price.amount>22.41</price.amount>
                            </Point>
                            <Point>
                                <position>50</position>
                                <price.amount>-12.47</price.amount>
                            </Point>
                            <Point>
                                <position>51</position>
                                <price.amount>-20.16</price.amount>
                            </Point>
                            <Point>
                                <position>52</position>
                                <price.amount>-17.47</price.amount>
                            </Point>
                            <Point>
                                <position>53</position>
                                <price.amount>-37.79</price.amount>
                            </Point>
                            <Point>
                                <position>54</position>
                                <price.amount>-37.6</price.amount>
                            </Point>
                            <Point>
                                <position>55</position>
                                <price.amount>-30.82</price.amount>
                            </Point>
                            <Point>
                                <position>56</position>
                                <price.amount>-39.57</price.amount>
                            </Point>
                            <Point>
                                <position>57</position>
                                <price.amount>-58.29</price.amount>
                            </Point>
                            <Point>
                                <position>58</position>
                                <price.amount>-55.26</price.amount>
                            </Point>
                            <Point>
                                <position>59</position>
                                <price.amount>-52.64</price.amount>
                            </Point>
                            <Point>
                                <position>60</position>
                                <price.amount>-21.84</price.amount>
                            </Point>
                            <Point>
                                <position>61</position>
                                <price.amount>-58.92</price.amount>
                            </Point>
                            <Point>
                                <position>62</position>
                                <price.amount>-51.02</price.amount>
                            </Point>
                            <Point>
                                <position>63</position>
                                <price.amount>-46.66</price.amount>
                            </Point>
                            <Point>
                                <position>64</position>
                                <price.amount>32.5</price.amount>
                            </Point>
                            <Point>
                                <position>65</position>
                                <price.amount>-62.54</price.amount>
                            </Point>
                            <Point>
                                <position>66</position>
                                <price.amount>-41.35</price.amount>
                            </Point>
                            <Point>
                                <position>67</position>
                                <price.amount>12.51</price.amount>
                            </Point>
                            <Point>
                                <position>68</position>
                                <price.amount>34.48</price.amount>
                            </Point>
                            <Point>
                                <position>69</position>
                                <price.amount>-82.82</price.amount>
                            </Point>
                            <Point>
                                <position>70</position>
                                <price.amount>-5.5</price.amount>
                            </Point>
                            <Point>
                                <position>71</position>
                                <price.amount>42.48</price.amount>
                            </Point>
                            <Point>
                                <position>72</position>
                                <price.amount>79.98</price.amount>
                            </Point>
                            <Point>
                                <position>73</position>
                                <price.amount>5.03</price.amount>
                            </Point>
                            <Point>
                                <position>74</position>
                                <price.amount>28.09</price.amount>
                            </Point>
                            <Point>
                                <position>75</position>
                                <price.amount>74.91</price.amount>
                            </Point>
                            <Point>
                                <position>76</position>
                                <price.amount>79.98</price.amount>
                            </Point>
                            <Point>
                                <position>77</position>
                                <price.amount>49.47</price.amount>
                            </Point>
                            <Point>
                                <position>78</position>
                                <price.amount>62.45</price.amount>
                            </Point>
                            <Point>
                                <position>79</position>
                                <price.amount>80.99</price.amount>
                            </Point>
                            <Point>
                                <position>80</position>
                                <price.amount>85.54</price.amount>
                            </Point>
                            <Point>
                                <position>81</position>
                                <price.amount>80.99</price.amount>
                            </Point>
                            <Point>
                                <position>82</position>
                                <price.amount>87.43</price.amount>
                            </Point>
                            <Point>
                                <position>83</position>
                                <price.amount>86.93</price.amount>
                            </Point>
                            <Point>
                                <position>84</position>
                                <price.amount>90.04</price.amount>
                            </Point>
                            <Point>
                                <position>85</position>
                                <price.amount>97.42</price.amount>
                            </Point>
                            <Point>
                                <position>86</position>
                                <price.amount>80.08</price.amount>
                            </Point>
                            <Point>
                                <position>87</position>
                                <price.amount>79.98</price.amount>
                            </Point>
                            <Point>
                                <position>88</position>
                                <price.amount>79.91</price.amount>
                            </Point>
                            <Point>
                                <position>89</position>
                                <price.amount>96.22</price.amount>
                            </Point>
                            <Point>
                                <position>90</position>
                                <price.amount>89.91</price.amount>
                            </Point>
                            <Point>
                                <position>91</position>
                                <price.amount>79.92</price.amount>
                            </Point>
                            <Point>
                                <position>92</position>
                                <price.amount>69.95</price.amount>
                            </Point>
                            <Point>
                                <position>93</position>
                                <price.amount>99.94</price.amount>
                            </Point>
                            <Point>
                                <position>94</position>
                                <price.amount>80.99</price.amount>
                            </Point>
                            <Point>
                                <position>95</position>
                                <price.amount>74.91</price.amount>
                            </Point>
                            <Point>
                                <position>96</position>
                                <price.amount>51.59</price.amount>
                            </Point>
                        </Period>
                    </TimeSeries>
                </Publication_MarketDocument>
                                """;

        var quarterHourResponse = EntsoeHistoricalPriceFetcher.parseResponse(xml, Duration.ofMinutes(15));
        var quarterHours = quarterHourResponse.prices();
        var hours = EntsoeHistoricalPriceFetcher.parseResponse(xml, Duration.ofMinutes(60)).prices();

        assertEquals("EUR", quarterHourResponse.currency());
        assertEquals(96, quarterHours.size());
        assertEquals(0.10498, quarterHours.firstEntry().getValue(), 0.000001);
        assertEquals(0.10498, quarterHours.get(Instant.parse("2024-07-27T22:15:00Z")), 0.000001);
        assertEquals(0.07998, quarterHours.get(Instant.parse("2024-07-28T01:15:00Z")), 0.000001);
        assertEquals(-0.03071, quarterHours.get(Instant.parse("2024-07-28T07:45:00Z")), 0.000001);
        assertEquals(24, hours.size());
        assertEquals(0.09031, hours.firstEntry().getValue(), 0.000001);
        assertEquals(0.0663575, hours.get(Instant.parse("2024-07-28T01:00:00Z")), 0.000001);
    }

    @Test
    public void normalizesMixedSourceResolutions() throws IOException {
        String xml = """
                <Publication_MarketDocument>
                    <period.timeInterval><start>2024-01-01T10:00Z</start><end>2024-01-01T12:00Z</end></period.timeInterval>
                    <TimeSeries>
                        <currency_Unit.name>EUR</currency_Unit.name>
                        <price_Measure_Unit.name>MWH</price_Measure_Unit.name>
                        <Period>
                            <timeInterval><start>2024-01-01T10:00Z</start><end>2024-01-01T11:00Z</end></timeInterval>
                            <resolution>PT60M</resolution>
                            <Point><position>1</position><price.amount>100</price.amount></Point>
                        </Period>
                        <Period>
                            <timeInterval><start>2024-01-01T11:00Z</start><end>2024-01-01T12:00Z</end></timeInterval>
                            <resolution>PT15M</resolution>
                            <Point><position>1</position><price.amount>200</price.amount></Point>
                            <Point><position>2</position><price.amount>400</price.amount></Point>
                            <Point><position>3</position><price.amount>600</price.amount></Point>
                            <Point><position>4</position><price.amount>800</price.amount></Point>
                        </Period>
                    </TimeSeries>
                </Publication_MarketDocument>
                """;

        var defaultPrices = EntsoeHistoricalPriceFetcher.parseResponse(xml, null).prices();
        var quarterHours = EntsoeHistoricalPriceFetcher.parseResponse(xml, Duration.ofMinutes(15)).prices();
        var hours = EntsoeHistoricalPriceFetcher.parseResponse(xml, Duration.ofMinutes(60)).prices();

        assertEquals(8, defaultPrices.size());
        assertEquals(0.1, defaultPrices.get(Instant.parse("2024-01-01T10:45:00Z")), 0.000001);
        assertEquals(8, quarterHours.size());
        assertEquals(0.1, quarterHours.get(Instant.parse("2024-01-01T10:45:00Z")), 0.000001);
        assertEquals(2, hours.size());
        assertEquals(0.1, hours.get(Instant.parse("2024-01-01T10:00:00Z")), 0.000001);
        assertEquals(0.5, hours.get(Instant.parse("2024-01-01T11:00:00Z")), 0.000001);
    }

    @Test
    public void convertsCaseInsensitiveEnergyUnits() throws IOException {
        String xml = """
                <Publication_MarketDocument>
                    <period.timeInterval><start>2024-01-01T10:00Z</start><end>2024-01-01T11:00Z</end></period.timeInterval>
                    <TimeSeries>
                        <currency_Unit.name>EUR</currency_Unit.name>
                        <price_Measure_Unit.name>mWh</price_Measure_Unit.name>
                        <Period>
                            <timeInterval><start>2024-01-01T10:00Z</start><end>2024-01-01T11:00Z</end></timeInterval>
                            <resolution>PT60M</resolution>
                            <Point><position>1</position><price.amount>100</price.amount></Point>
                        </Period>
                    </TimeSeries>
                </Publication_MarketDocument>
                """;

        var prices = EntsoeHistoricalPriceFetcher.parseResponse(xml, Duration.ofMinutes(15)).prices();

        assertEquals(0.1, prices.firstEntry().getValue(), 0.000001);
    }

    @Test
    public void rejectsIntervalsNotAlignedToTheHour() {
        String xml = """
                <Publication_MarketDocument>
                    <period.timeInterval><start>%s</start><end>%s</end></period.timeInterval>
                    <TimeSeries>
                        <currency_Unit.name>EUR</currency_Unit.name>
                        <price_Measure_Unit.name>KWH</price_Measure_Unit.name>
                        <Period>
                            <timeInterval><start>2024-01-01T10:00Z</start><end>2024-01-01T11:00Z</end></timeInterval>
                            <resolution>PT60M</resolution>
                            <Point><position>1</position><price.amount>1</price.amount></Point>
                        </Period>
                    </TimeSeries>
                </Publication_MarketDocument>
                """;

        assertThrows(IOException.class, () -> EntsoeHistoricalPriceFetcher
                .parseResponse(Objects.requireNonNull(xml.formatted("2024-01-01T10:15Z", "2024-01-01T11:00Z")), null));
        assertThrows(IOException.class, () -> EntsoeHistoricalPriceFetcher
                .parseResponse(Objects.requireNonNull(xml.formatted("2024-01-01T10:00Z", "2024-01-01T11:15Z")), null));
    }

    @Test
    public void rejectsDifferentCurrenciesAcrossTimeSeries() {
        String xml = """
                <Publication_MarketDocument>
                    <period.timeInterval><start>2024-01-01T10:00Z</start><end>2024-01-01T11:00Z</end></period.timeInterval>
                    <TimeSeries>
                        <currency_Unit.name>EUR</currency_Unit.name>
                        <price_Measure_Unit.name>KWH</price_Measure_Unit.name>
                        <Period>
                            <timeInterval><start>2024-01-01T10:00:00Z</start><end>2024-01-01T11:00:00Z</end></timeInterval>
                            <resolution>PT15M</resolution>
                            <Point><position>1</position><price.amount>1</price.amount></Point>
                        </Period>
                    </TimeSeries>
                    <TimeSeries>
                        <currency_Unit.name>USD</currency_Unit.name>
                        <price_Measure_Unit.name>KWH</price_Measure_Unit.name>
                        <Period>
                            <timeInterval><start>2024-01-01T10:00:00Z</start><end>2024-01-01T11:00:00Z</end></timeInterval>
                            <resolution>PT15M</resolution>
                            <Point><position>1</position><price.amount>1</price.amount></Point>
                        </Period>
                    </TimeSeries>
                </Publication_MarketDocument>
                """;

        assertThrows(IOException.class, () -> EntsoeHistoricalPriceFetcher.parseResponse(xml, Duration.ofMinutes(15)));
    }

    @Test
    public void rejectsNonIntegralResolutionConversion() {
        String xml = """
                <Publication_MarketDocument>
                    <period.timeInterval><start>2024-01-01T10:00Z</start><end>2024-01-01T11:00Z</end></period.timeInterval>
                    <TimeSeries>
                        <currency_Unit.name>EUR</currency_Unit.name>
                        <price_Measure_Unit.name>KWH</price_Measure_Unit.name>
                        <Period>
                            <timeInterval><start>2024-01-01T10:00:00Z</start><end>2024-01-01T11:00:00Z</end></timeInterval>
                            <resolution>PT17M</resolution>
                            <Point><position>1</position><price.amount>1</price.amount></Point>
                        </Period>
                    </TimeSeries>
                </Publication_MarketDocument>
                """;

        assertThrows(IOException.class, () -> EntsoeHistoricalPriceFetcher.parseResponse(xml, Duration.ofMinutes(60)));
    }

    @Test
    public void rejectsMalformedXml() {
        assertThrows(IOException.class,
                () -> EntsoeHistoricalPriceFetcher.parseResponse("<Publication_MarketDocument>", Duration.ofHours(1)));
    }

    @Test
    public void splitsLongRangesIntoAtMostOneYearRequests() {
        var start = Objects.requireNonNull(Instant.parse("2020-01-01T00:00:00Z"));
        var end = Objects.requireNonNull(Instant.parse("2023-01-01T00:00:00Z"));
        var cursor = start;
        int requestCount = 0;

        while (cursor.isBefore(end)) {
            var next = EntsoeHistoricalPriceFetcher.nextRequestEnd(cursor, end);
            assertFalse(next.isAfter(cursor.atZone(ZoneId.systemDefault()).plusYears(1).toInstant()));
            assertFalse(next.isAfter(end));
            cursor = next;
            requestCount++;
        }

        assertEquals(3, requestCount);
    }

    @Test
    public void requestsAnotherPageOnlyWhenResponseHas100TimeSeries() {
        assertFalse(EntsoeHistoricalPriceFetcher.hasMoreTimeSeries(99));
        assertFalse(EntsoeHistoricalPriceFetcher.hasMoreTimeSeries(0));
        assertTrue(EntsoeHistoricalPriceFetcher.hasMoreTimeSeries(100));
        assertTrue(EntsoeHistoricalPriceFetcher.hasMoreTimeSeries(101));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENTSOE_API_TOKEN", matches = ".+")
    public void fetchesFinlandQuarterHourPrices() throws Exception {
        assertFetchesPrices(Objects.requireNonNull(Duration.ofMinutes(15)));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "ENTSOE_API_TOKEN", matches = ".+")
    public void fetchesFinlandHourlyPrices() throws Exception {
        assertFetchesPrices(Objects.requireNonNull(Duration.ofMinutes(60)));
    }

    private static void assertFetchesPrices(Duration resolution) throws IOException, InterruptedException {
        int days = 1;
        var end = Objects
                .requireNonNull(LocalDate.now(FINLAND_ZONE).minusDays(2).atStartOfDay(FINLAND_ZONE).toInstant());
        var start = Objects.requireNonNull(end.minus(Duration.ofDays(days)));
        var token = Objects.requireNonNull(System.getenv("ENTSOE_API_TOKEN"));
        var prices = EntsoeHistoricalPriceFetcher.fetch(token, FINLAND_AREA, start, end, resolution, 30);

        assertFalse(prices.prices().isEmpty(), "Expected historical spot-price data for Finland");
        assertEquals(60 / resolution.toMinutes() * 24 * days, prices.prices().size());
    }
}
