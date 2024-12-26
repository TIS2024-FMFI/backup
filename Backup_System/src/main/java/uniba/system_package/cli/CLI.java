package uniba.system_package.cli;

import uniba.system_package.backup.BackupManager;
import uniba.system_package.utils.ConfigurationManager;

import java.util.Scanner;

public class CLI {
    private BackupManager backupManager;
    private ConfigurationManager configurationManager;
    private boolean isRunning;

    public CLI(BackupManager backupManager, ConfigurationManager configurationManager) {
        this.backupManager = backupManager;
        this.configurationManager = configurationManager;
        this.isRunning = true;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to the Backup System CLI. Type 'help' for a list of commands.");

        while (isRunning) {
            System.out.print("cli> ");
            String input = scanner.nextLine();
            handleCommand(input);
        }
    }

    private void handleCommand(String input) {
        String[] parts = input.split(" ");
        String command = parts[0];

        switch (command) {
            case "help":
                displayHelp();
                break;
            case "schedule_backup":
                scheduleBackup(parts);
                break;
            case "run_backup":
                runBackup(parts);
                break;
            case "restore_backup":
                restoreBackup(parts);
                break;
            case "status":
                displayStatus();
                break;
            case "list_backups":
                listBackups();
                break;
            case "configure_settings":
                configureSettings(parts);
                break;
            case "enable_server":
                enableServer(parts);
                break;
            case "disable_server":
                disableServer(parts);
                break;
            case "validate_script":
                validateScript(parts);
                break;
            case "exit":
                exit();
                break;
            default:
                System.out.println("Unknown command. Type 'help' for a list of available commands.");
        }
    }

    private void displayHelp() {
        System.out.println("Available Commands:");
        System.out.println("help - Displays the list of available commands.");
        System.out.println("schedule_backup --target <TARGET_NAME> --type <TYPE> --schedule <CRON_EXPRESSION>");
        System.out.println("run_backup --target <TARGET_NAME>");
        System.out.println("restore_backup --id <BACKUP_ID>");
        System.out.println("status - Displays the current status of the backup system.");
        System.out.println("list_backups - Lists all stored backups with metadata.");
        System.out.println("configure_settings --setting <SETTING_NAME> --value <NEW_VALUE>");
        System.out.println("enable_server --target <TARGET_NAME>");
        System.out.println("disable_server --target <TARGET_NAME>");
        System.out.println("validate_script --path <SCRIPT_PATH>");
        System.out.println("exit - Exits the program.");
    }

    private void scheduleBackup(String[] parts) {
        System.out.println("Command to schedule a backup received. Implement logic here.");
        // Parse and implement the logic to schedule a backup
    }

    private void runBackup(String[] parts) {
        System.out.println("Command to run a manual backup received. Implement logic here.");
        // Parse and implement the logic to run a backup
    }

    private void restoreBackup(String[] parts) {
        System.out.println("Command to restore a backup received. Implement logic here.");
        // Parse and implement the logic to restore a backup
    }

    private void displayStatus() {
        System.out.println("Displaying system status. Implement logic here.");
        // Implement logic to display the status of the system
    }

    private void listBackups() {
        System.out.println("Listing all backups. Implement logic here.");
        // Implement logic to list all backups
    }

    private void configureSettings(String[] parts) {
        System.out.println("Command to configure settings received. Implement logic here.");
        // Parse and implement logic to update settings
    }

    private void enableServer(String[] parts) {
        System.out.println("Command to enable a server received. Implement logic here.");
        // Parse and implement logic to enable a server
    }

    private void disableServer(String[] parts) {
        System.out.println("Command to disable a server received. Implement logic here.");
        // Parse and implement logic to disable a server
    }

    private void validateScript(String[] parts) {
        System.out.println("Command to validate a script received. Implement logic here.");
        // Parse and implement logic to validate a script
    }

    private void exit() {
        System.out.println("Exiting the CLI. Goodbye!");
        isRunning = false;
    }
}
