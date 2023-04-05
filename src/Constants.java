import java.util.Arrays;
import java.util.List;

public final class Constants {
    public static final long MARKET_DAILY_PERIOD = 10000; // 10 seconds
    public static final String MARKET_AGENT_NAME = "market-agent";
    public static final List<String> BROKER_AGENT_NAMES = Arrays.asList("broker-agent-1", "broker-agent-2", "broker-agent-4");
    public static final String EXCHANGE_AGENT_NAME = "exchange-agent";


    private Constants() {
        // private constructor to prevent instantiation
    }
}
