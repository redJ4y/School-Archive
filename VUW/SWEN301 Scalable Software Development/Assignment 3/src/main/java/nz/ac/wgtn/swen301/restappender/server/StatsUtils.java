package nz.ac.wgtn.swen301.restappender.server;

import java.util.HashMap;
import java.util.Map;

public class StatsUtils {
    public static Map<String, int[]> calculateLogStats() {
        Map<String, int[]> stats = new HashMap<>();
        for (LogEvent event : Persistency.DB) {
            String logger = event.getLogger();
            LogEvent.LogLevel level = event.getLevel();
            if (!stats.containsKey(logger)) {
                stats.put(logger, new int[LogEvent.LogLevel.values().length]);
            }
            int[] loggerStats = stats.get(logger);
            loggerStats[level.ordinal()]++;
        }
        return stats;
    }
}
