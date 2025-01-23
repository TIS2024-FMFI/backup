package uniba.system_package.backup;

import org.slf4j.Logger;
import uniba.system_package.scripts.ScriptExecutor;
import uniba.system_package.storage.StorageManager;
import uniba.system_package.utils.LogManager;

import java.util.List;

public class Server implements BackupTarget {
    private static final Logger logger = LogManager.getLogger(Server.class);

    private final String name;
    private final String host;
    private final String user;
    private final String password;
    private final List<String> pathsToBackup;
    private final String preBackupScript; // Path to pre-backup script
    private final String postBackupScript; // Path to post-backup script
    private final StorageManager storageManager;

    public Server(String name, String host, String user, String password, List<String> pathsToBackup,
                  String preBackupScript, String postBackupScript) {
        this.name = name;
        this.host = host;
        this.user = user;
        this.password = password;
        this.pathsToBackup = pathsToBackup;
        this.preBackupScript = preBackupScript;
        this.postBackupScript = postBackupScript;
        this.storageManager = new StorageManager();
    }

    public String getName() {
        return name;
    }

    public List<String> getPathsToBackup() {
        return pathsToBackup;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private boolean enabled = true; // Default to enabled

    public boolean isEnabled() {
        return enabled;
    }
    @Override
    public boolean performBackup() {
        logger.info("Starting backup for server: {}", name);
        if (!enabled) {
            logger.info("Backup skipped for server '{}' because it is disabled.", name);
            return false;
        }

        logger.info("Starting backup for server: {}", name);

        ScriptExecutor scriptExecutor = new ScriptExecutor();


        // Execute pre-backup script
        if (preBackupScript != null && !preBackupScript.isEmpty()) {
            logger.info("Executing pre-backup script for server: {}", name);
            if (!scriptExecutor.executeScript(preBackupScript)) {
                logger.error("Pre-backup script failed for server: {}", name);
                return false;
            }
        }

        try {
            String backupArchivePath = "/backups/" + name + ".tar.gz";
            if (storageManager.compressFiles(pathsToBackup, backupArchivePath)) {
                logger.info("Backup successfully stored at: {}", backupArchivePath);
            } else {
                logger.error("Failed to create backup archive for server: {}", name);
                return false;
            }
        } catch (Exception e) {
            logger.error("Backup failed for server: {}", name, e);
            return false;
        }

        // Execute post-backup script
        if (postBackupScript != null && !postBackupScript.isEmpty()) {
            logger.info("Executing post-backup script for server: {}", name);
            if (!scriptExecutor.executeScript(postBackupScript)) {
                logger.error("Post-backup script failed for server: {}", name);
                return false;
            }
        }

        return true;
    }

}