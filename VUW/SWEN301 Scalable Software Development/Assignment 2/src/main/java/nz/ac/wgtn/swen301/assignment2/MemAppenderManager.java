package nz.ac.wgtn.swen301.assignment2;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;
import java.util.List;

public class MemAppenderManager implements MemAppenderManagerMBean {

    private final MemAppender memAppender;
    private final PatternLayout patternLayout;

    public MemAppenderManager(MemAppender memAppender) {
        this.memAppender = memAppender;
        this.patternLayout = new PatternLayout();
    }

    @Override
    public String[] getLogs() {
        List<LoggingEvent> logs = memAppender.getCurrentLogs();
        String[] logStrings = new String[logs.size()];
        for (int i = 0; i < logs.size(); i++) {
            logStrings[i] = patternLayout.format(logs.get(i));
        }
        return logStrings;
    }

    @Override
    public long getLogCount() {
        return memAppender.getCurrentLogs().size();
    }

    @Override
    public long getDiscardedLogCount() {
        return memAppender.getDiscardedLogCount();
    }

    @Override
    public void exportToJSON(String fileName) {
        try {
            memAppender.exportToJSON(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
