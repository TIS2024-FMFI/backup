package uniba.system_package.backup;

import uniba.system_package.scripts.ScriptExecutor;
import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

public class Database implements BackupTarget {
    private static final Logger logger = LogManager.getLogger(Database.class);

    private String name;
    private String type; // e.g., MySQL
    private String host;
    private String user;
    private String password;
    private String preBackupScript; // Path to pre-backup script
    private String postBackupScript; // Path to post-backup script

    // Constructor
    public Database(String name, String type, String host, String user, String password,
                    String preBackupScript, String postBackupScript) {
        this.name = name;
        this.type = type;
        this.host = host;
        this.user = user;
        this.password = password;
        this.preBackupScript = preBackupScript;
        this.postBackupScript = postBackupScript;
    }

    // Getters
    @Override
    public String getName() {
        return name;
    }

    public String getType() {
        return type;
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

    // Implementing BackupTarget methods
    @Override
    public void performBackup() {
        logger.info("Starting database backup for: {}", name);

        ScriptExecutor scriptExecutor = new ScriptExecutor();

        // Execute pre-backup script if specified
        if (preBackupScript != null && !preBackupScript.isEmpty()) {
            logger.info("Executing pre-backup script for database: {}", name);
            if (!scriptExecutor.executeScript(preBackupScript)) {
                logger.error("Pre-backup script failed for database: {}", name);
                return; // Abort backup if pre-backup script fails
            }
        }

        try {
            // Simulated database dump logic (replace with actual database dump logic)
            Thread.sleep(1000); // Simulating time taken for dump
            logger.info("Database backup completed successfully for: {}", name);
        } catch (InterruptedException e) {
            logger.error("Database backup interrupted for: {}", name, e);
        } catch (Exception e) {
            logger.error("Database backup failed for: {}", name, e);
        }

        // Execute post-backup script if specified
        if (postBackupScript != null && !postBackupScript.isEmpty()) {
            logger.info("Executing post-backup script for database: {}", name);
            if (!scriptExecutor.executeScript(postBackupScript)) {
                logger.error("Post-backup script failed for database: {}", name);
            }
        }
    }
}
