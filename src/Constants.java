import java.util.Arrays;
import java.util.List;

public final class Constants {
    public static final long MARKET_DAILY_PERIOD = 1000; // 1 second
    public static final String MARKET_AGENT_NAME = "market-agent";
    public static final List<String> BROKER_AGENT_NAMES = Arrays.asList("no-sell-broker-agent", "no-margin-broker-agent", "no-buy-broker-agent", "all-broker-agent");
    public static final String EXCHANGE_AGENT_NAME = "exchange-agent";
    public static final String MARKET_NO_MORE_DAYS_MSG = "NO_MORE_DAYS";
    public static final String UNSUPPORTED_ORDER_TYPE = "UNSUPPORTED_ORDER";
    public static final String COMPLETED_ORDER = "COMPLETE_ORDER";
    public static final String DATA_FILENAME = "C:\\Users\\ASUS\\Desktop\\MIEIC\\1anom\\s2\\asma\\data.json";
    public enum ORDER_TYPES {
        BUY("BUY"),
        SELL("SELL"),
        SHORT("SHORT");

        ORDER_TYPES(String buy) {
        }
    }

    private Constants() {
        // private constructor to prevent instantiation
    }
}
