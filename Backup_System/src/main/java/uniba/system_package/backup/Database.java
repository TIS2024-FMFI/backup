package uniba.system_package.backup;

import uniba.system_package.scripts.ScriptExecutor;
import uniba.system_package.storage.StorageManager;
import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

import java.util.List;

public class Database implements BackupTarget {
    private static final Logger logger = LogManager.getLogger(Database.class);

    private String name;
    private String type; // e.g., MySQL
    private String host;
    private String user;
    private String password;
    private String preBackupScript; // Path to pre-backup script
    private String postBackupScript; // Path to post-backup script
    private StorageManager storageManager;

    public Database(String name, String type, String host, String user, String password,
                    String preBackupScript, String postBackupScript) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.user = user;
        this.password = password;
        this.preBackupScript = preBackupScript;
        this.postBackupScript = postBackupScript;
        this.storageManager = new StorageManager();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean performBackup() {
        logger.info("Starting database backup for: {}", name);

        ScriptExecutor scriptExecutor = new ScriptExecutor();

        // Execute pre-backup script
        if (preBackupScript != null && !preBackupScript.isEmpty()) {
            logger.info("Executing pre-backup script for database: {}", name);
            if (!scriptExecutor.executeScript(preBackupScript)) {
                logger.error("Pre-backup script failed for database: {}", name);
                return false;
            }
        }

        try {
            String dumpFilePath = "/dumps/" + name + ".sql";
            if (storageManager.createDatabaseDump(type, host, user, password, dumpFilePath)) {
                logger.info("Database dump created successfully at: {}", dumpFilePath);
            } else {
                logger.error("Failed to create database dump for: {}", name);
                return false;
            }

            String backupArchivePath = "/backups/" + name + ".tar.gz";
            if (storageManager.compressFiles(List.of(dumpFilePath), backupArchivePath)) {
                logger.info("Database backup successfully stored at: {}", backupArchivePath);
            } else {
                logger.error("Failed to compress database dump for: {}", name);
                return false;
            }
        } catch (Exception e) {
            logger.error("Backup failed for database: {}", name, e);
            return false;
        }

        // Execute post-backup script
        if (postBackupScript != null && !postBackupScript.isEmpty()) {
            logger.info("Executing post-backup script for database: {}", name);
            if (!scriptExecutor.executeScript(postBackupScript)) {
                logger.error("Post-backup script failed for database: {}", name);
                return false;
            }
        }

        return true;
    }
}
