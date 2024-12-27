package uniba.system_package.scheduler;

import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class Scheduler {
    private static final Logger logger = LogManager.getLogger(Scheduler.class);

    private final org.quartz.Scheduler scheduler;
    private final ExecutorService executorService;

    public Scheduler() {
        try {
            this.scheduler = StdSchedulerFactory.getDefaultScheduler();
            this.scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to initialize Quartz Scheduler", e);
        }
        this.executorService = Executors.newCachedThreadPool();
    }

    /**
     * Schedules a backup task using a cron expression.
     *
     * @param backupType  The type of backup (e.g., "full" or "incremental").
     * @param backupTask  The backup task to execute.
     * @param cronExpression The cron expression to schedule the task.
     */
    public void scheduleCronBackup(String backupType, Runnable backupTask, String cronExpression) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(BackupJobWrapper.class)
                    .withIdentity(backupType + "-backup", "backup-jobs")
                    .build();

            jobDetail.getJobDataMap().put("task", backupTask);

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(backupType + "-trigger", "backup-triggers")
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);

            logger.info("{} backup scheduled with cron expression: {}", backupType, cronExpression);
        } catch (SchedulerException e) {
            logger.error("Failed to schedule {} backup: {}", backupType, e.getMessage(), e);
        }
    }

    /**
     * Stops the scheduler and shuts down resources.
     */
    public void stop() {
        try {
            scheduler.shutdown();
            executorService.shutdown();
            logger.info("Scheduler stopped.");
        } catch (SchedulerException e) {
            logger.error("Failed to stop the scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * A Quartz job wrapper for executing tasks.
     */
    public static class BackupJobWrapper implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            Runnable task = (Runnable) context.getJobDetail().getJobDataMap().get("task");
            try {
                task.run();
            } catch (Exception e) {
                Logger logger = LogManager.getLogger(BackupJobWrapper.class);
                logger.error("Error executing backup task: {}", e.getMessage(), e);
            }
        }
    }
}
