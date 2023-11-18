package nz.ac.wgtn.swen301.assignment2;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import javax.management.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;

public class MemAppender extends AppenderSkeleton {

    private long maxSize;
    private long discardedLogCount;
    private final ArrayDeque<LoggingEvent> logList;
    private ObjectName mBeanName;

    public MemAppender() {
        maxSize = 1000;
        discardedLogCount = 0;
        logList = new ArrayDeque<>((int) maxSize + 1);
        mBeanName = null;
    }

    @Override
    public synchronized void setName(String newName) {
        super.setName(newName);

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        if (mBeanName != null && mbs.isRegistered(mBeanName)) {
            // Unregister previous MBean to rename
            try {
                mbs.unregisterMBean(mBeanName);
            } catch (MBeanRegistrationException | InstanceNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {
            mBeanName = new ObjectName("nz.ac.wgtn.swen301.assignment2:type=MemAppender,name=" + newName);
            MemAppenderManagerMBean mbean = new MemAppenderManager(this);
            mbs.registerMBean(mbean, mBeanName);
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException |
                 MBeanRegistrationException e) {
            e.printStackTrace();
        }
    }

    public long getMaxSize() {
        return maxSize;
    }

    public synchronized void setMaxSize(long maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize must be non-negative");
        }
        this.maxSize = maxSize;
        while (logList.size() > maxSize) {
            logList.pollFirst();
            discardedLogCount++;
        }
    }

    public long getDiscardedLogCount() {
        return discardedLogCount;
    }

    public synchronized List<LoggingEvent> getCurrentLogs() {
        return List.copyOf(logList);
    }

    public synchronized void exportToJSON(String fileName) throws IOException {
        JsonLayout jsonLayout = new JsonLayout();
        try (BufferedWriter file = new BufferedWriter(new FileWriter(fileName))) {
            file.write("[");
            Iterator<LoggingEvent> iterator = logList.iterator();
            while (iterator.hasNext()) {
                file.write(jsonLayout.format(iterator.next()));
                if (iterator.hasNext()) {
                    file.write(",");
                }
            }
            file.write("]");
        } // Automatically flushed and closed
    }

    @Override
    protected synchronized void append(LoggingEvent event) {
        logList.addLast(event);
        if (logList.size() > maxSize) {
            logList.pollFirst();
            discardedLogCount++;
        }
    }

    @Override
    public synchronized void close() {
        discardedLogCount = 0;
        logList.clear();
    }

    @Override
    public boolean requiresLayout() {
        // Layout not required for appending
        return false;
    }
}
