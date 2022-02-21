package ru.task.throttler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class ThrottlerUtils {

    public static final int NOTIFIERS_PER_PROCESSOR_COUNT;

    public static final int DISTRIBUTORS_COUNT;

    public static final Set<String> POSSIBLE_CCY_PAIRS;

    static {
        NOTIFIERS_PER_PROCESSOR_COUNT = getFromEnvOrDefault("NOTIFIERS_PER_PROCESSOR_COUNT", 10);
        DISTRIBUTORS_COUNT = getFromEnvOrDefault("DISTRIBUTORS_COUNT", 5);

        POSSIBLE_CCY_PAIRS = new HashSet<>();
        //all possible ccypairs should be declared here
        //it's better to get it from file or any smarter way, but for demonstration only few ccyPairs are hardcoded here
        POSSIBLE_CCY_PAIRS.add("EURUSD");
        POSSIBLE_CCY_PAIRS.add("EURRUB");
        POSSIBLE_CCY_PAIRS.add("USDRUB");
    }

    private static int getFromEnvOrDefault(String envName, int defaultCount) {
        var env = System.getenv(envName);
        if (env != null) {
            return Integer.parseInt(env);
        }
        return defaultCount;
    }

    public static <T> Map<String, T> createAllCcyPairsMap(Function<String, T> valueMapper) {
        return POSSIBLE_CCY_PAIRS
                .stream()
                .collect(toMap(Function.identity(), valueMapper));
    }
}
