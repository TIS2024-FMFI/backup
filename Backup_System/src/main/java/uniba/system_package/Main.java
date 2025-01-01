package uniba.system_package;

import uniba.system_package.backup.BackupManager;
import uniba.system_package.cli.CLI;
import uniba.system_package.utils.ConfigurationManager;

public class Main {
        public static void main(String[] args) {
            // 1. Load the configuration
            String configFilePath = "config.yaml";

            // Create and initialize the ConfigurationManager
            ConfigurationManager configurationManager = new ConfigurationManager();
            // Load the configuration (ensure you have a method to do so)
            configurationManager.loadConfiguration(configFilePath);

            // Validate the config to ensure required fields are present
            try {
                configurationManager.validateConfig();
            } catch (IllegalArgumentException e) {
                System.err.println("Configuration validation failed: " + e.getMessage());
                System.exit(1);
            }

            // 2. Create the BackupManager using the loaded configuration
            BackupManager backupManager = new BackupManager(configurationManager);

            // 3. Create the CLI and start it
            CLI cli = new CLI(backupManager, configurationManager);

            // 4. Start the CLI
            cli.start();
        }
}
