package uniba.system_package.backup;

import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

public class BackupJob {
    private static final Logger logger = LogManager.getLogger(BackupJob.class);

    private BackupTarget target;
    private String backupType; // "full" or "incremental"
    private BackupMetadata metadata;

    public BackupJob(BackupTarget target, String backupType) {
        this.target = target;
        this.backupType = backupType;
        this.metadata = new BackupMetadata(target.getName(), backupType);
    }

    public void run(BackupManager backupManager) {
        logger.info("Starting {} backup job for target: {}", backupType, target.getName());
        metadata.setStartTime(System.currentTimeMillis());

        try {
            target.performBackup();
            metadata.setStatus("success");

            metadata.setLocation("/backups/" + target.getName() + "_" + backupType + ".tar.gz");
            metadata.setBackupSize(1024 * 1024 * 10); // Simulate 10MB size
        } catch (Exception e) {
            metadata.setStatus("failure");
            logger.error("Backup failed for target: {}", target.getName(), e);
        } finally {
            metadata.setEndTime(System.currentTimeMillis());
        }

        logMetadata(backupManager);
    }

    private void logMetadata(BackupManager backupManager) {
        logger.info("Backup completed. Metadata: {}", metadata);

        // Notify via BackupManager
        backupManager.notifyBackupResult(metadata);
    }

    public BackupMetadata getMetadata() {
        return metadata;
    }
}
