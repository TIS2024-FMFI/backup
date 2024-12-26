package uniba.system_package.scheduler;

import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Scheduler {
    private static final Logger logger = LogManager.getLogger(Scheduler.class);

    private final Timer timer;
    private final ExecutorService executorService;

    public Scheduler() {
        this.timer = new Timer(true); // Daemon thread to ensure JVM can exit
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Parses a schedule string and returns a simulated delay (in milliseconds).
     * Replace with a real cron parser for production systems.
     *
     * @param cronExpression The cron-like expression to parse.
     * @return Simulated delay in milliseconds.
     */
    public long parseSchedule(String cronExpression) {
        logger.info("Parsing cron expression: {}", cronExpression);

        // Simulated delay (e.g., 1-minute delay for simplicity)
        return System.currentTimeMillis() + 60 * 1000; // 1-minute delay
    }

    /**
     * Schedules a backup task to run periodically.
     *
     * @param backupType    The type of backup (e.g., "full" or "incremental").
     * @param backupTask    The backup task to execute.
     * @param delay         Initial delay before the task starts (in milliseconds).
     * @param interval      Interval between subsequent executions (in milliseconds).
     */
    public void scheduleBackup(String backupType, Runnable backupTask, long delay, long interval) {
        logger.info("Scheduling {} backup with an initial delay of {} ms and interval of {} ms.", backupType, delay, interval);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                executorService.submit(() -> {
                    logger.info("Executing {} backup task...", backupType);
                    try {
                        backupTask.run();
                    } catch (Exception e) {
                        logger.error("Error executing {} backup task: {}", backupType, e.getMessage(), e);
                    }
                });
            }
        }, delay, interval);
    }

    /**
     * Starts the scheduler.
     */
    public void start() {
        logger.info("Scheduler started.");
    }

    /**
     * Stops the scheduler and shuts down resources.
     */
    public void stop() {
        logger.info("Stopping scheduler...");
        timer.cancel();
        executorService.shutdown();
        logger.info("Scheduler stopped.");
    }
}
