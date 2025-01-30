package uniba.system_package;

import uniba.system_package.backup.BackupManager;
import uniba.system_package.backup.BackupMetadata;
import uniba.system_package.cli.CLI;
import uniba.system_package.scheduler.Scheduler;
import uniba.system_package.storage.StorageManager;
import uniba.system_package.utils.ConfigurationManager;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;



public class Main {
    public static void main(String[] args) {
        // Locate the configuration file from resources
        String configFileName = "config.yaml";

        // Load the config file from the classpath
        InputStream configInputStream = Main.class.getClassLoader().getResourceAsStream(configFileName);

        if (configInputStream == null) {
            System.err.println("Configuration file not found in the resources: " + configFileName);
            System.exit(1);
        }

        ConfigurationManager configurationManager = null;
        try {
            // Copy the InputStream to a temporary file (if needed, for libraries expecting file paths)
            Path tempConfigFile = Files.createTempFile("config", ".yaml");
            Files.copy(configInputStream, tempConfigFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            configInputStream.close();

            // Print the path for debugging
            System.out.println("Using temporary config file: " + tempConfigFile.toAbsolutePath());

            // Load the configuration
            configurationManager = new ConfigurationManager();
            configurationManager.loadConfiguration(tempConfigFile.toAbsolutePath().toString());
            System.out.println("Configuration loaded successfully.");
        } catch (Exception e) {
            System.err.println("Failed to load configuration: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }


        // Step 2: Initialize Components
        StorageManager storageManager = new StorageManager();
        Scheduler scheduler = new Scheduler();

        // Step 3: Initialize Backup Metadata Map
        Map<String, BackupMetadata> backupMetadataMap = new HashMap<>();

        // Step 4: Initialize BackupManager
        BackupManager backupManager = new BackupManager(configurationManager, storageManager, scheduler, backupMetadataMap);

        // Start scheduled backups
        try {
            backupManager.startScheduledBackups();
            System.out.println("Scheduled backups started successfully.");
        } catch (Exception e) {
            System.err.println("Error starting scheduled backups: " + e.getMessage());
            e.printStackTrace();
        }

        // Step 5: Initialize and Start the CLI
        CLI cli = new CLI(backupManager, configurationManager);
        try {
            System.out.println("Starting CLI...");
            cli.start(); // Start the CLI to allow user interaction
        } catch (Exception e) {
            System.err.println("Error running CLI: " + e.getMessage());
            e.printStackTrace();
        }

        // Step 6: Graceful Shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down backup system...");
            try {
                backupManager.stopScheduler(); // Stop the scheduler gracefully
                System.out.println("Backup system shut down gracefully.");
            } catch (Exception e) {
                System.err.println("Error during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }
}
