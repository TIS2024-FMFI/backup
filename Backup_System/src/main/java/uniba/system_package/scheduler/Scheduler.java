package uniba.system_package.scheduler;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import uniba.system_package.backup.BackupManager;
import uniba.system_package.backup.BackupTarget;
import uniba.system_package.utils.LogManager;

import java.util.List;

public class Scheduler {
    private static final Logger logger = LogManager.getLogger(Scheduler.class);

    private final org.quartz.Scheduler quartzScheduler;

    public Scheduler() {
        try {
            this.quartzScheduler = StdSchedulerFactory.getDefaultScheduler();
            this.quartzScheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to initialize Quartz Scheduler", e);
        }
    }

    /**
     * Schedules a backup task for a specific target using a cron expression.
     *
     * @param target          The BackupTarget to schedule backups for.
     * @param backupType      The type of backup ("full" or "incremental").
     * @param cronExpression  The cron expression defining the schedule.
     */
    public void scheduleCronBackupForTarget(BackupTarget target, String backupType, String cronExpression) throws SchedulerException {
        String jobName = target.getName() + "-" + backupType + "-backup";
        String jobGroup = "backup-jobs";

        JobDetail jobDetail = JobBuilder.newJob(BackupJobWrapper.class)
                .withIdentity(jobName, jobGroup)
                .usingJobData("backupType", backupType)
                .usingJobData("targetName", target.getName())
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "-trigger", jobGroup)
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .forJob(jobDetail)
                .build();

        quartzScheduler.scheduleJob(jobDetail, trigger);
        logger.info("Scheduled '{}' backup for target '{}' with cron '{}'", backupType, target.getName(), cronExpression);
    }


    /**
     * Existing method to schedule global backups.
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

            quartzScheduler.scheduleJob(jobDetail, trigger);

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
            quartzScheduler.shutdown();
            logger.info("Scheduler stopped.");
        } catch (SchedulerException e) {
            logger.error("Failed to stop the scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Expose Quartz Scheduler for BackupManager injection.
     */
    public org.quartz.Scheduler getQuartzScheduler() {
        return this.quartzScheduler;
    }



    /**
     * A Quartz job wrapper for executing tasks.
     */
    public static class BackupJobWrapper implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            Logger logger = LogManager.getLogger(BackupJobWrapper.class);
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();

            String backupType = dataMap.getString("backupType");
            String targetName = dataMap.getString("targetName");

            if (backupType != null && targetName != null) {
                BackupManager backupManager;
                try {
                    backupManager = (BackupManager) context.getScheduler().getContext().get("backupManager");
                } catch (SchedulerException e) {
                    logger.error("Failed to retrieve BackupManager from scheduler context: {}", e.getMessage(), e);
                    return;
                }

                try {
                    backupManager.startBackupForTarget(backupType, targetName);
                } catch (Exception e) {
                    logger.error("Error executing scheduled backup for target '{}': {}", targetName, e.getMessage(), e);
                }
            } else {
                logger.warn("Missing backupType or targetName in job data.");
            }
        }
    }

    // Add this method to Scheduler.java
    public boolean isRunning() {
        try {
            // Return true if the scheduler is not in standby or shutdown mode
            return !quartzScheduler.isInStandbyMode() && !quartzScheduler.isShutdown();
        } catch (SchedulerException e) {
            // Log the error and return false if something went wrong
            logger.error("Failed to determine scheduler status: {}", e.getMessage(), e);
            return false;
        }
    }

    public String getNextRunTime(String jobName) {
        try {

            for (String groupName : quartzScheduler.getJobGroupNames()) {
                for (JobKey jobKey : quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    if (jobKey.getName().equals(jobName)) {
                        // Get all triggers associated with this job
                        List<? extends Trigger> triggers = quartzScheduler.getTriggersOfJob(jobKey);
                        if (!triggers.isEmpty()) {
                            // Return the next fire time of the first trigger
                            Trigger trigger = triggers.get(0);
                            return trigger.getNextFireTime().toString();
                        }
                    }
                }
            }
        } catch (SchedulerException e) {
            logger.error("Failed to retrieve next run time for job '{}': {}", jobName, e.getMessage(), e);
        }
        // If no job or trigger was found, return a default message
        return "Not scheduled";
    }

}
