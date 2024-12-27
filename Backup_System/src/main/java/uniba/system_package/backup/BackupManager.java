package uniba.system_package.backup;

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
}
