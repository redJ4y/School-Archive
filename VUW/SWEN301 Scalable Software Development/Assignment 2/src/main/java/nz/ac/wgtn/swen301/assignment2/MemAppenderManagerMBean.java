package nz.ac.wgtn.swen301.assignment2;

public interface MemAppenderManagerMBean {

    String[] getLogs();

    long getLogCount();

    long getDiscardedLogCount();

    void exportToJSON(String fileName);
}
