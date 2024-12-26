package uniba.system_package;

import uniba.system_package.backup.BackupManager;
import uniba.system_package.utils.ConfigurationManager;

/*public class Main {
    public static void main(String[] args) {
        // Load the configuration
        ConfigurationManager configManager = new ConfigurationManager();
        configManager.loadConfig("config.yaml");

        // Initialize BackupManager
        BackupManager backupManager = new BackupManager(configManager);

        // Manual Backup Test
        System.out.println("Starting manual full backup...");
        backupManager.startBackup("full");

        // Scheduled Backup Test

        System.out.println("Starting scheduled backups...");
        backupManager.startScheduledBackups();

        // Keep the application running for testing
        try {
            Thread.sleep(60 * 1000); // Run for 1 minute
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop the scheduler after testing
        backupManager.stopScheduler();
    }
}*/
import uniba.system_package.cli.CLI;

public class Main {
    public static void main(String[] args) {
        // Load the configuration
        ConfigurationManager configurationManager = new ConfigurationManager();
        configurationManager.loadConfig("config.yaml");

        // Initialize BackupManager
        BackupManager backupManager = new BackupManager(configurationManager);

        // Start the CLI
        CLI cli = new CLI(backupManager, configurationManager);
        cli.start();
    }
}

