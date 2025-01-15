package uniba.system_package.backup;

import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import uniba.system_package.scheduler.Scheduler;
import uniba.system_package.storage.RetentionPolicy;
import uniba.system_package.storage.StorageManager;
import uniba.system_package.utils.ConfigurationManager;
import uniba.system_package.utils.LogManager;
import uniba.system_package.notification.NotificationManager;
import org.slf4j.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BackupManager {
    private static final Logger logger = LogManager.getLogger(BackupManager.class);

    private final ConfigurationManager configurationManager;
    private final StorageManager storageManager;
    private final RetentionPolicy retentionPolicy;
    private final Scheduler scheduler;
    private final NotificationManager notificationManager;

    public BackupManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
        this.storageManager = new StorageManager();
        this.retentionPolicy = new RetentionPolicy();
        this.scheduler = new Scheduler();

        // Initialize NotificationManager
        ConfigurationManager.Config.Email emailConfig = configurationManager.getEmail();
        this.notificationManager = new NotificationManager(
                emailConfig.getSmtpHost(),
                emailConfig.getSmtpPort(),
                emailConfig.getUsername(),
                emailConfig.getPassword(),
                emailConfig.getFromAddress(),
                emailConfig.getToAddresses()
        );

        // Inject BackupManager into Quartz Scheduler context
        try {
            this.scheduler.getQuartzScheduler().getContext().put("backupManager", this);
        } catch (SchedulerException e) {
            logger.error("Failed to inject BackupManager into Quartz Scheduler context: {}", e.getMessage(), e);
        }
    }

    /**
     * Configures and starts scheduled backups.
     */
    public void startScheduledBackups() {
        logger.info("Configuring scheduled backups...");

        String fullBackupCron = configurationManager.getSchedule().getFullBackup();
        String incrementalBackupCron = configurationManager.getSchedule().getIncrementalBackup();

        try {
            // Schedule full backups
            scheduler.scheduleCronBackup("full",
                    () -> startBackup("full"), fullBackupCron);

            // Schedule incremental backups
            scheduler.scheduleCronBackup("incremental",
                    () -> startBackup("incremental"), incrementalBackupCron);

            logger.info("Scheduled full and incremental backups successfully.");
        } catch (Exception e) {
            logger.error("Error configuring scheduled backups: {}", e.getMessage(), e);
        }
    }

    /**
     * Starts a backup process based on the provided type (e.g., full or incremental).
     *
     * @param backupType The type of backup to run ("full" or "incremental").
     */
    public void startBackup(String backupType) {
        logger.info("Starting {} backup process...", backupType);

        List<BackupTarget> backupTargets = addBackupTargets();

        for (BackupTarget target : backupTargets) {
            BackupJob job = new BackupJob(target, backupType);
            job.run(this); // Pass BackupManager instance to run method

            // Handle remote storage integration
            BackupMetadata metadata = job.getMetadata();
            if ("success".equals(metadata.getStatus())) {
                ConfigurationManager.Config.RemoteStorage remoteConfig = configurationManager.getRemoteStorage();
                String remotePath = remoteConfig.getRemotePath() + "/" + metadata.getTargetName() + "_" + backupType + ".tar.gz";

                if (!storageManager.uploadToSFTP(metadata.getLocation(), remotePath,
                        remoteConfig.getHost(), remoteConfig.getUser(), remoteConfig.getPassword())) {
                    logger.error("Failed to store backup remotely for target: {}", metadata.getTargetName());
                } else {
                    logger.info("Backup for target {} successfully uploaded to remote location: {}", metadata.getTargetName(), remotePath);
                }
            }
        }

        applyRetentionPolicies();
        logger.info("{} backup process completed.", backupType);
    }



    /**
     * Adds servers and databases to the backup target list.
     *
     * @return List of BackupTarget objects.
     */
    private List<BackupTarget> addBackupTargets() {
        List<BackupTarget> backupTargets = new ArrayList<>();

        // Load servers
        for (ConfigurationManager.Config.Server serverConfig : configurationManager.getServers()) {
            backupTargets.add(new Server(
                    serverConfig.getName(),
                    serverConfig.getHost(),
                    serverConfig.getUser(),
                    serverConfig.getPassword(),
                    serverConfig.getPathsToBackup(),
                    serverConfig.getPreBackupScript(),
                    serverConfig.getPostBackupScript()
            ));
        }

        // Load databases
        for (ConfigurationManager.Config.Database databaseConfig : configurationManager.getDatabases()) {
            backupTargets.add(new Database(
                    databaseConfig.getName(),
                    databaseConfig.getType(),
                    databaseConfig.getHost(),
                    databaseConfig.getUser(),
                    databaseConfig.getPassword(),
                    databaseConfig.getPreBackupScript(),
                    databaseConfig.getPostBackupScript()
            ));
        }

        return backupTargets;
    }


    /**
     * Applies retention policies to manage old backups.
     */
    private void applyRetentionPolicies() {
        logger.info("Applying retention policies...");
        try {
            List<String> allBackups = storageManager.listBackupFiles("/backups");
            int fullBackupsToKeep = configurationManager.getRetentionPolicy().getFullBackupsToKeep();

            storageManager.deleteOldBackups(allBackups, fullBackupsToKeep);
            logger.info("Retention policies applied successfully.");
        } catch (Exception e) {
            logger.error("Error applying retention policies: {}", e.getMessage(), e);
        }
    }

    /**
     * Stops the scheduler safely.
     */
    public void stopScheduler() {
        scheduler.stop();
    }

    // Send email after each backup
    public void notifyBackupResult(BackupMetadata metadata) {
        String subject = "Backup Result: " + metadata.getTargetName();
        String message = notificationManager.formatBackupResultMessage(
                metadata.getBackupType(),
                metadata.getTargetName(),
                metadata.getStatus(),
                metadata.getLocation()
        );

        notificationManager.sendEmail(subject, message);
    }

    /**
     * Overloaded method to manually start a backup for ONE specific target (Server or Database).
     * @param backupType  "full" or "incremental"
     * @param targetName  The name of the server or database as defined in config
     * @return true if the backup was performed, false if no matching target or invalid backupType
     */
    public boolean startBackupForTarget(String backupType, String targetName) {
        // 1) Log the request
        logger.info("Request received to start a manual '{}' backup for target '{}'.", backupType, targetName);

        // 2) Validate the backup type if you're strictly allowing only "full" or "incremental"
        if (!"full".equalsIgnoreCase(backupType) && !"incremental".equalsIgnoreCase(backupType)) {
            logger.warn("Invalid backup type '{}' provided. Only 'full' or 'incremental' are allowed.", backupType);
            return false;
        }

        // 3) Load all possible targets (Servers + Databases) from config
        List<BackupTarget> backupTargets = addBackupTargets();
        if (backupTargets.isEmpty()) {
            logger.warn("No backup targets found in the configuration. Unable to proceed with backup.");
            return false;
        }

        boolean foundTarget = false;

        // 4) Search for the matching target by name
        for (BackupTarget target : backupTargets) {
            // Compare case-insensitive to match your config entries
            if (target.getName().equalsIgnoreCase(targetName)) {
                foundTarget = true;
                logger.info("Found target '{}'. Starting '{}' backup...", target.getName(), backupType);

                // 5) Create and run the BackupJob
                BackupJob job = new BackupJob(target, backupType);
                try {
                    job.run(this);
                } catch (Exception e) {
                    logger.error("Error while running backup job for target '{}': {}", target.getName(), e.getMessage(), e);
                    return false;
                }

                // 6) Once the job completes, apply retention policies to remove old backups if needed
                applyRetentionPolicies();
                logger.info("Retention policies applied after manual backup for '{}'.", target.getName());

                // We only wanted to back up one target, so we can break out of the loop
                break;
            }
        }

        // 7) If we never found a target with the given name, log and return false
        if (!foundTarget) {
            logger.warn("No target named '{}' found. Backup not performed.", targetName);
            return false;
        }
        logger.info("Manual '{}' backup for target '{}' completed successfully.", backupType, targetName);
        return true;
    }

    /**
     * Schedules a backup for a specific target with a given type and cron expression.
     *
     * @param targetName      The name of the backup target (Server or Database).
     * @param backupType      The type of backup ("full" or "incremental").
     * @param cronExpression  The cron expression defining the backup schedule.
     * @return true if scheduling was successful, false otherwise.
     */
    public boolean scheduleBackupForTarget(String targetName, String backupType, String cronExpression) {
        logger.info("Scheduling a '{}' backup for target '{}' with cron '{}'", backupType, targetName, cronExpression);

        // 1. Validate backup type
        if (!"full".equalsIgnoreCase(backupType) && !"incremental".equalsIgnoreCase(backupType)) {
            logger.warn("Invalid backup type '{}'. Only 'full' or 'incremental' are allowed.", backupType);
            return false;
        }

        // 2. Adjust cron expression to include seconds for Quartz
        String quartzCronExpression;
        String[] cronParts = cronExpression.trim().split("\\s+");
        if (cronParts.length == 5) {
            quartzCronExpression = "0 " + cronExpression;
        } else if (cronParts.length == 6) {
            quartzCronExpression = cronExpression;
        } else {
            logger.warn("Invalid cron expression '{}'. Expected 5 or 6 fields.", cronExpression);
            return false;
        }

        // 3. If 6 fields, ensure day-of-month or day-of-week is '?'
        if (quartzCronExpression.split("\\s+").length == 6) {
            String[] parts = quartzCronExpression.split("\\s+");
            // Check if both day-of-month and day-of-week are '*'
            if ("*".equals(parts[3]) && "*".equals(parts[5])) {
                // Replace day-of-month with '?'
                parts[3] = "?";
                quartzCronExpression = String.join(" ", parts);
                logger.info("Adjusted cron expression to '{}'", quartzCronExpression);
            }
        }

        // 4. Validate the adjusted cron expression
        if (!CronExpression.isValidExpression(quartzCronExpression)) {
            logger.warn("Invalid cron expression '{}'.", quartzCronExpression);
            return false;
        }

        // 5. Find the target
        List<BackupTarget> backupTargets = addBackupTargets();
        BackupTarget matchedTarget = null;
        for (BackupTarget target : backupTargets) {
            if (target.getName().equalsIgnoreCase(targetName)) {
                matchedTarget = target;
                break;
            }
        }

        if (matchedTarget == null) {
            logger.warn("No target named '{}' found. Cannot schedule backup.", targetName);
            return false;
        }

        // 6. Schedule the backup using Scheduler
        try {
            scheduler.scheduleCronBackupForTarget(matchedTarget, backupType, quartzCronExpression);
            logger.info("Successfully scheduled '{}' backup for target '{}' with cron '{}'", backupType, targetName, quartzCronExpression);
            return true;
        } catch (Exception e) {
            logger.error("Failed to schedule backup for target '{}': {}", targetName, e.getMessage(), e);
            return false;
        }
    }
}