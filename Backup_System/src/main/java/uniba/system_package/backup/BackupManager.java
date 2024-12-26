package uniba.system_package.backup;

import uniba.system_package.scheduler.Scheduler;
import uniba.system_package.storage.RetentionPolicy;
import uniba.system_package.storage.StorageManager;
import uniba.system_package.utils.ConfigurationManager;
import uniba.system_package.utils.LogManager;
import uniba.system_package.notification.NotificationManager;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.List;

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
            long fullBackupDelay = scheduler.parseSchedule(fullBackupCron);
            long incrementalBackupDelay = scheduler.parseSchedule(incrementalBackupCron);

            // Schedule full backups
            scheduler.scheduleBackup("full", () -> startBackup("full"),
                    fullBackupDelay - System.currentTimeMillis(),
                    24 * 60 * 60 * 1000L); // Every 24 hours

            // Schedule incremental backups
            scheduler.scheduleBackup("incremental", () -> startBackup("incremental"),
                    incrementalBackupDelay - System.currentTimeMillis(),
                    6 * 60 * 60 * 1000L); // Every 6 hours

            scheduler.start();
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
     * Compresses files and stores the backup for a specific target.
     *
     * @param target     The backup target.
     * @param backupType The type of backup ("full" or "incremental").
     */
    private void handleBackupStorage(BackupTarget target, String backupType) {
        String backupLocation = "/backups/" + target.getName() + "_" + backupType + ".tar.gz";
        List<String> pathsToBackup = target instanceof Server
                ? ((Server) target).getPathsToBackup()
                : List.of(); // Replace with actual paths for databases

        if (!storageManager.compressFiles(pathsToBackup, backupLocation)) {
            logger.error("Failed to compress files for target: {}", target.getName());
            return;
        }

        String remotePath = "/remote/backups/" + target.getName() + "_" + backupType + ".tar.gz";
        storageManager.storeBackup(backupLocation, remotePath);
    }

    /**
     * Applies retention policies to manage old backups.
     */
    private void applyRetentionPolicies() {
        List<String> allBackups = List.of("/backups/Server1_backup.tar.gz", "/backups/Database1_backup.tar.gz");
        retentionPolicy.deleteOldBackups(allBackups, configurationManager.getRetentionPolicy().getFullBackupsToKeep());
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
