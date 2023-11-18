package nz.ac.wgtn.swen301.assignment2.example;

import nz.ac.wgtn.swen301.assignment2.MemAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogRunner {

    private static final String[] MESSAGES = {"Sample log message 1", "Sample log message 2", "This is a warning", "System error occurred", "User login successful", "Some operation failed"};
    private static final Level[] LEVELS = {Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR, Level.FATAL};

    public static void main(String[] args) {
        Random random = new Random();

        Logger logger = Logger.getLogger(LogRunner.class);
        MemAppender memAppender = new MemAppender();
        memAppender.setName("LogRunnerMemAppender");
        logger.addAppender(memAppender);
        logger.setAdditivity(false);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        Runnable logTask = () -> {
            String randomMessage = MESSAGES[random.nextInt(MESSAGES.length)];
            Level randomLevel = LEVELS[random.nextInt(LEVELS.length)];
            logger.log(randomLevel, randomMessage);
        };
        // Schedule the task to run every 1 second
        scheduler.scheduleAtFixedRate(logTask, 0, 1, TimeUnit.SECONDS);
        // Stop the scheduler after 2 minutes
        scheduler.schedule(scheduler::shutdown, 2, TimeUnit.MINUTES);
    }
}
