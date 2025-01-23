package uniba.system_package.backup;
import uniba.system_package.backup.Database;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import uniba.system_package.notification.NotificationManager;
import uniba.system_package.scheduler.Scheduler;
import uniba.system_package.storage.RetentionPolicy;
import uniba.system_package.storage.StorageManager;
import uniba.system_package.utils.ConfigurationManager;
import uniba.system_package.utils.LogManager;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupManager {
    private static final Logger logger = LogManager.getLogger(BackupManager.class);

    private final ConfigurationManager configurationManager;
    private final StorageManager storageManager;
    private final RetentionPolicy retentionPolicy;
    private final Scheduler scheduler;
    private final NotificationManager notificationManager;
    private final Map<String, BackupMetadata> backupTargets;

    public BackupManager(ConfigurationManager configurationManager, StorageManager storageManager, Scheduler scheduler, Map<String, BackupMetadata> backupTargets) {
        this.configurationManager = configurationManager;
        this.storageManager = new StorageManager();
        this.retentionPolicy = new RetentionPolicy();
        this.scheduler = scheduler;
        this.backupTargets = backupTargets;

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

        // Convert Config.Server to Server (implements BackupTarget)
        configurationManager.getServers().forEach(serverConfig -> {
            backupTargets.add(new Server(
                    serverConfig.getName(),
                    serverConfig.getHost(),
                    serverConfig.getUser(),
                    serverConfig.getPassword(),
                    serverConfig.getPathsToBackup(),
                    serverConfig.getPreBackupScript(),
                    serverConfig.getPostBackupScript()
            ));
        });

        // Convert Config.Database to Database (implements BackupTarget)
        configurationManager.getDatabases().forEach(databaseConfig -> {

            BackupTarget databaseTarget = new Database(
                    databaseConfig.getName(),
                    databaseConfig.getType(),
                    databaseConfig.getHost(),
                    databaseConfig.getUser(),
                    databaseConfig.getPassword(),
                    databaseConfig.getPreBackupScript(),
                    databaseConfig.getPostBackupScript()
            );
            backupTargets.add(databaseTarget);
        });

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


    public SystemStatus getSystemStatus() {
        boolean schedulerActive = scheduler.isRunning();

        Map<String, String> lastBackupTimes = new HashMap<>();
        Map<String, String> nextBackupTimes = new HashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

        for (String target : backupTargets.keySet()) {
            BackupMetadata lastBackup = backupTargets.get(target);
            String lastTime = (lastBackup != null)
                    ? formatter.format(Instant.ofEpochSecond(lastBackup.getStartTime()))
                    : "No backups yet";
            lastBackupTimes.put(target, lastTime);

            String nextTime = scheduler.getNextRunTime(target);
            nextBackupTimes.put(target, (nextTime != null) ? nextTime : "Not scheduled");
        }

        return new SystemStatus(schedulerActive, lastBackupTimes, nextBackupTimes);
    }

    /**
     * Restores a backup by its unique ID.
     *
     * @param backupId The unique ID of the backup to restore.
     * @return true if the restore is successful, false otherwise.
     */
    public boolean restoreBackupById(String backupId) {
        logger.info("Starting restore process for backup ID: {}", backupId);

        try {
            // Locate the backup file by its ID
            Path backupPath = Paths.get("/backups/", backupId + ".tar.gz");

            if (!backupPath.toFile().exists()) {
                logger.error("Backup file not found for ID: {}", backupId);
                return false;
            }

            // Restore the backup (unpack or import)
            boolean restoreSuccess = storageManager.restoreBackup(backupPath);

            if (restoreSuccess) {
                logger.info("Backup ID {} restored successfully.", backupId);
                return true;
            } else {
                logger.error("Failed to restore backup ID: {}", backupId);
                return false;
            }
        } catch (Exception e) {
            logger.error("An error occurred while restoring backup ID {}: {}", backupId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Enables a target (server or database) by name.
     *
     * @param targetName The name of the target to enable.
     * @return true if the target was found and enabled, false otherwise.
     */
    public boolean enableTarget(String targetName) {
        List<BackupTarget> targets = addBackupTargets();
        for (BackupTarget target : targets) {
            if (target.getName().equalsIgnoreCase(targetName)) {
                if (target instanceof Server) {
                    ((Server) target).setEnabled(true);
                } else if (target instanceof Database) {
                    ((Database) target).setEnabled(true);
                }
                logger.info("Target '{}' has been enabled.", targetName);
                return true;
            }
        }
        logger.warn("Target '{}' not found. Cannot enable it.", targetName);
        return false;
    }


    /**
     * Disables a target (server or database) by name.
     *
     * @param targetName The name of the target to disable.
     * @return true if the target was found and disabled, false otherwise.
     */
    public boolean disableTarget(String targetName) {
        List<BackupTarget> targets = addBackupTargets();
        for (BackupTarget target : targets) {
            if (target.getName().equalsIgnoreCase(targetName)) {
                if (target instanceof Server) {
                    ((Server) target).setEnabled(false);
                } else if (target instanceof Database) {
                    ((Database) target).setEnabled(false);
                }
                logger.info("Target '{}' has been disabled.", targetName);
                return true;
            }
        }
        logger.warn("Target '{}' not found. Cannot disable it.", targetName);
        return false;
    }


    /**
     * Retrieves all backups as a list of BackupMetadata.
     *
     * @return List of BackupMetadata objects representing all stored backups.
     */
    public List<BackupMetadata> getAllBackups() {
        logger.info("Retrieving all backups...");
        List<BackupMetadata> backups = new ArrayList<>();

        try {
            Path backupsDir = Paths.get("/backups");
            if (!Files.exists(backupsDir) || !Files.isDirectory(backupsDir)) {
                logger.warn("Backups directory does not exist or is not a directory: {}", backupsDir);
                return backups;
            }

            Files.list(backupsDir).filter(Files::isRegularFile).forEach(file -> {
                try {
                    // Parse backup metadata from file name or content
                    String fileName = file.getFileName().toString();
                    BackupMetadata metadata = parseBackupMetadata(fileName);
                    if (metadata != null) {
                        backups.add(metadata);
                    }
                } catch (Exception e) {
                    logger.error("Error parsing backup file '{}': {}", file.getFileName(), e.getMessage(), e);
                }
            });
        } catch (IOException e) {
            logger.error("Error listing backups: {}", e.getMessage(), e);
        }

        logger.info("Found {} backups.", backups.size());
        return backups;
    }

    /**
     * Parses a backup file name to create a BackupMetadata object.
     *
     * @param fileName The file name of the backup.
     * @return BackupMetadata object, or null if parsing fails.
     */

    private BackupMetadata parseBackupMetadata(String fileName) {
        try {
            // Example: backup_Server1_full_20250101.tar.gz
            String[] parts = fileName.split("_");
            if (parts.length < 4) {
                logger.warn("Invalid backup file name format: {}", fileName);
                return null;
            }

            String targetName = parts[1];
            String backupType = parts[2];
            String timestamp = parts[3].replace(".tar.gz", "");

            // Convert timestamp string to long (epoch time)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, formatter);
            long epochTime = dateTime.toEpochSecond(ZoneOffset.UTC);

            BackupMetadata metadata = new BackupMetadata();
            metadata.setTargetName(targetName);
            metadata.setBackupType(backupType);
            metadata.setStartTime(epochTime); // Set as a long
            return metadata;
        } catch (Exception e) {
            logger.error("Error parsing metadata from file name '{}': {}", fileName, e.getMessage(), e);
            return null;
        }
    }


}
