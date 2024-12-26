package uniba.system_package.backup;

import uniba.system_package.scripts.ScriptExecutor;
import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

import java.util.List;

public class Server implements BackupTarget {
    private static final Logger logger = LogManager.getLogger(Server.class);

    private String name;
    private String host;
    private String user;
    private String password;
    private List<String> pathsToBackup;
    private String preBackupScript; // Path to pre-backup script
    private String postBackupScript; // Path to post-backup script

    // Constructor
    public Server(String name, String host, String user, String password, List<String> pathsToBackup,
                  String preBackupScript, String postBackupScript) {
        this.name = name;
        this.host = host;
        this.user = user;
        this.password = password;
        this.pathsToBackup = pathsToBackup;
        this.preBackupScript = preBackupScript;
        this.postBackupScript = postBackupScript;
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getHost() {
        return host;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getPathsToBackup() {
        return pathsToBackup;
    }

    // Perform the backup
    @Override
    public void performBackup() {
        logger.info("Starting backup for server: {}", name);

        ScriptExecutor scriptExecutor = new ScriptExecutor();

        // Execute pre-backup script if specified
        if (preBackupScript != null && !preBackupScript.isEmpty()) {
            logger.info("Executing pre-backup script for server: {}", name);
            if (!scriptExecutor.executeScript(preBackupScript)) {
                logger.error("Pre-backup script failed for server: {}", name);
                return; // Abort backup if pre-backup script fails
            }
        }

        try {
            for (String path : pathsToBackup) {
                logger.info("Backing up path: {}", path);
                // Simulate backup logic
                Thread.sleep(500); // Simulating time taken for backup
            }
            logger.info("Backup completed successfully for server: {}", name);
        } catch (InterruptedException e) {
            logger.error("Backup interrupted for server: {}", name, e);
        } catch (Exception e) {
            logger.error("Backup failed for server: {}", name, e);
        }

        // Execute post-backup script if specified
        if (postBackupScript != null && !postBackupScript.isEmpty()) {
            logger.info("Executing post-backup script for server: {}", name);
            if (!scriptExecutor.executeScript(postBackupScript)) {
                logger.error("Post-backup script failed for server: {}", name);
            }
        }
    }
}
