package uniba.system_package.cli;

import uniba.system_package.backup.BackupManager;
import uniba.system_package.utils.ConfigurationManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

public class CLI {
    private BackupManager backupManager;
    private ConfigurationManager configurationManager;
    private boolean isRunning;

    private static class ParsedCommand {
        private String command;
        private Map<String, String> args;

        public ParsedCommand(String command, Map<String, String> args) {
            this.command = command;
            this.args = args;
        }

        public String getCommand() {
            return command;
        }

        public Map<String, String> getArgs() {
            return args;
        }
    }

    public CLI(BackupManager backupManager, ConfigurationManager configurationManager) {
        this.backupManager = backupManager;
        this.configurationManager = configurationManager;
        this.isRunning = true;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("Welcome to the Backup System CLI. Type 'help' for a list of commands.");

        while (isRunning) {
            System.out.print("cli> ");
            String input = scanner.nextLine();

            ParsedCommand parsed = parseInput(input);
            handleCommand(parsed);
        }
    }

    private ParsedCommand parseInput(String input) {
        List<String> tokens = new ArrayList<>();
        Matcher m = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(input);
        while (m.find()) {
            if (m.group(1) != null) {
                tokens.add(m.group(1));
            } else {
                tokens.add(m.group(2));
            }
        }

        if (tokens.isEmpty()) {
            return new ParsedCommand("", new HashMap<>());
        }

        int index = 0;
        if (tokens.get(0).equalsIgnoreCase("backup-system")) {
            index++;
        }

        if (index >= tokens.size()) {
            return new ParsedCommand("", new HashMap<>());
        }

        String command = tokens.get(index);
        index++;

        Map<String, String> argsMap = new HashMap<>();
        while (index < tokens.size()) {
            String token = tokens.get(index);
            if (token.startsWith("--")) {
                String key = token.substring(2);
                String value = "";
                if (index + 1 < tokens.size() && !tokens.get(index + 1).startsWith("--")) {
                    value = tokens.get(index + 1);
                    index++;
                }
                argsMap.put(key, value);
            }
            index++;
        }

        return new ParsedCommand(command, argsMap);
    }

    private void handleCommand(ParsedCommand parsedCommand) {
        String command = parsedCommand.getCommand();
        Map<String, String> args = parsedCommand.getArgs();

        if (command.isEmpty()) {
            System.out.println("No command detected. Type 'help' for a list of commands.");
            return;
        }

        if (command.equalsIgnoreCase("help")) {
            String subCommand = args.get("cmd");
            displayHelp(subCommand);
            return;
        }

        switch (command) {
            case "schedule_backup":
                scheduleBackup(args);
                System.out.println();
                break;
            case "run_backup":
                runBackup(args);
                System.out.println();
                break;
            case "restore_backup":
                restoreBackup(args);
                System.out.println();
                break;
            case "status":
                displayStatus();
                System.out.println();
                break;
            case "list_backups":
                listBackups();
                System.out.println();
                break;
            case "enable_server":
                enableServer(args);
                System.out.println();
                break;
            case "disable_server":
                disableServer(args);
                System.out.println();
                break;
            case "validate_script":
                validateScript(args);
                System.out.println();
                break;
            case "exit":
                exit();
                System.out.println();
                break;
            default:
                System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
        }
    }

    private void displayHelp(String subCommand) {
        if (subCommand == null || subCommand.isBlank()) {
            System.out.println();
            System.out.println("Available Commands:");
            System.out.println();
            System.out.println("  help                Shows this help list.");
            System.out.println("    Usage: help --cmd <COMMAND_NAME>");
            System.out.println("  schedule_backup     Schedule a new backup.");
            System.out.println("  run_backup          Manually run a backup for a target.");
            System.out.println("  restore_backup      Restore data from a specific backup.");
            System.out.println("  status              Shows the current system status.");
            System.out.println("  list_backups        Lists all backups in storage.");
            System.out.println("  enable_server       Turn on backups for a server.");
            System.out.println("  disable_server      Turn off backups for a server.");
            System.out.println("  validate_script     Check if a script is valid.");
            System.out.println("  exit                Exit the program.");
            System.out.println("\nTo see more about one command, use: help --cmd <COMMAND_NAME>\n");
            System.out.println();
        } else {
            switch (subCommand) {
                case "schedule_backup":
                    System.out.println();
                    System.out.println("Usage: schedule_backup --target <TARGET_NAME> --type <TYPE> --schedule <CRON_EXPRESSION>");
                    System.out.println("Example: schedule_backup --target Server1 --type full --schedule \"0 2 * * 0\"");
                    System.out.println("Sets up a backup schedule using a cron expression.");
                    System.out.println();
                    break;
                case "run_backup":
                    System.out.println();
                    System.out.println("Usage: run_backup --target <TARGET_NAME>");
                    System.out.println("Example: run_backup --target Server1");
                    System.out.println("Manually triggers a backup for the given target right away.");
                    System.out.println();
                    break;
                case "restore_backup":
                    System.out.println();
                    System.out.println("Usage: restore_backup --id <BACKUP_ID>");
                    System.out.println("Example: restore_backup --id 12345");
                    System.out.println("Restores the backup with that ID.");
                    System.out.println();
                    break;
                case "status":
                    System.out.println();
                    System.out.println("Usage: status");
                    System.out.println("Example: status");
                    System.out.println("Shows info about the system, like next backups or last backup time.");
                    System.out.println();
                    break;
                case "list_backups":
                    System.out.println();
                    System.out.println("Usage: list_backups");
                    System.out.println("Example: list_backups");
                    System.out.println("Lists all known backups and their details.");
                    System.out.println();
                    break;
                case "enable_server":
                    System.out.println();
                    System.out.println("Usage: enable_server --target <TARGET_NAME>");
                    System.out.println("Example: enable_server --target Server1");
                    System.out.println("Turns backups on for the chosen server.");
                    System.out.println();
                    break;
                case "disable_server":
                    System.out.println();
                    System.out.println("Usage: disable_server --target <TARGET_NAME>");
                    System.out.println("Example: disable_server --target Server1");
                    System.out.println("Stops backing up the chosen server.");
                    System.out.println();
                    break;
                case "validate_script":
                    System.out.println();
                    System.out.println("Usage: validate_script --path <SCRIPT_PATH>");
                    System.out.println("Example: validate_script --path /scripts/pre_backup.sh");
                    System.out.println("Checks if the given script can run properly.");
                    System.out.println();
                    break;
                case "exit":
                    System.out.println();
                    System.out.println("Usage: exit");
                    System.out.println("Quits the CLI.");
                    System.out.println();
                    break;
                default:
                    System.out.println();
                    System.out.println("Unknown command: " + subCommand);
                    System.out.println("Type 'help' to see a list of all available commands.");
                    System.out.println();
            }
        }
    }

    private void scheduleBackup(Map<String, String> args) {
        if (!args.containsKey("target") || !args.containsKey("type") || !args.containsKey("schedule")) {
            System.out.println("Error: Missing arguments.");
            System.out.println("Syntax: schedule_backup --target <TARGET_NAME> --type <TYPE> --schedule \"<CRON_EXPRESSION>\"");
            return;
        }

        String targetName = args.get("target");
        String backupType = args.get("type");
        String cronExpression = args.get("schedule");

        if (!"full".equalsIgnoreCase(backupType) && !"incremental".equalsIgnoreCase(backupType)) {
            System.out.println("Error: Invalid backup type. Must be 'full' or 'incremental'.");
            return;
        }

        System.out.println("Scheduling a " + backupType + " backup for target '" + targetName + "' with cron '" + cronExpression + "'.");

        boolean success = backupManager.scheduleBackupForTarget(targetName, backupType, cronExpression);

        if (success) {
            System.out.println("Backup scheduled successfully.");
        } else {
            System.out.println("Failed to schedule backup. Check logs for details.");
        }
    }

    private void runBackup(Map<String, String> args) {
        if (!args.containsKey("target") || args.get("target") == null || args.get("target").isBlank()) {
            System.out.println("Error: Missing required argument --target <TARGET_NAME>.");
            return;
        }

        String backupType = "full";
        if (args.containsKey("type") && !args.get("type").isBlank()) {
            backupType = args.get("type");
        }

        String targetName = args.get("target");
        System.out.println("Running a manual backup for: " + targetName + " with type: " + backupType);

        boolean success = backupManager.startBackupForTarget(backupType, targetName);

        if (success) {
            System.out.println("Manual backup completed successfully for: " + targetName);
        } else {
            System.out.println("Manual backup failed or target not found: " + targetName);
        }
    }

    private void restoreBackup(Map<String, String> args) {
        // Step 1: Validate arguments
        if (!args.containsKey("id") || args.get("id").isBlank()) {
            System.out.println("Error: Missing required argument --id <BACKUP_ID>.");
            return;
        }

        String backupId = args.get("id");
        System.out.println("Restoring backup with ID: " + backupId);

        // Step 2: Call BackupManager to handle restoration
        boolean success;
        try {
            success = backupManager.restoreBackupById(backupId);
        } catch (Exception e) {
            System.out.println("Error: An exception occurred while restoring the backup: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Step 3: Provide feedback to the user
        if (success) {
            System.out.println("Backup restored successfully for ID: " + backupId);
        } else {
            System.out.println("Error: Backup restoration failed for ID: " + backupId);
        }
    }

    private void displayStatus() {
        System.out.println("Showing system status...");
        try {
            // Fetch system status from BackupManager
            SystemStatus status = backupManager.getSystemStatus();
    
            // Display scheduler status
            System.out.println("Scheduler Status: " + (status.isSchedulerActive() ? "Active" : "Stopped"));
    
            // Display last successful backups
            System.out.println("\nLast Successful Backups:");
            status.getLastBackupTimes().forEach((target, time) -> {
                System.out.println("  " + target + ": " + (time != null ? time : "No backups yet"));
            });
    
            // Display next scheduled backups
            System.out.println("\nNext Scheduled Backups:");
            status.getNextBackupTimes().forEach((target, time) -> {
                System.out.println("  " + target + ": " + (time != null ? time : "Not scheduled"));
            });
        } catch (Exception e) {
            System.out.println("Error: Unable to fetch system status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void listBackups() {
        System.out.println("Showing all backups we have...");
        try {
            // Fetch list of backups from BackupManager
            List<BackupMetadata> backups = backupManager.getAllBackups();
    
            // Display backups in tabular format
            System.out.printf("%-10s %-20s %-10s %-20s\n", "BackupID", "TargetName", "Type", "Timestamp");
            System.out.println("--------------------------------------------------------------");
            for (BackupMetadata backup : backups) {
                System.out.printf("%-10s %-20s %-10s %-20s\n",
                        backup.getBackupId(),
                        backup.getTargetName(),
                        backup.getType(),
                        backup.getTimestamp());
            }
        } catch (Exception e) {
            System.out.println("Error: Unable to fetch backups: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void enableServer(Map<String, String> args) {
        if (!args.containsKey("target") || args.get("target").isBlank()) {
            System.out.println("Error: Missing required argument --target <TARGET_NAME>.");
            return;
        }

        String targetName = args.get("target");
        System.out.println("Enabling server: " + targetName);

        try {
            // Call BackupManager to enable the server
            boolean success = backupManager.enableTarget(targetName);
    
            if (success) {
                System.out.println("Server " + targetName + " has been successfully enabled.");
            } else {
                System.out.println("Error: Could not enable server " + targetName + ". It may not exist.");
            }
        } catch (Exception e) {
            System.out.println("Error: An exception occurred while enabling the server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void disableServer(Map<String, String> args) {
        if (!args.containsKey("target") || args.get("target").isBlank()) {
            System.out.println("Error: Missing required argument --target <TARGET_NAME>.");
            return;
        }

        String targetName = args.get("target");
        System.out.println("Disabling server: " + targetName);

        try {
            // Call BackupManager to disable the server
            boolean success = backupManager.disableTarget(targetName);
    
            if (success) {
                System.out.println("Server " + targetName + " has been successfully disabled.");
            } else {
                System.out.println("Error: Could not disable server " + targetName + ". It may not exist.");
            }
        } catch (Exception e) {
            System.out.println("Error: An exception occurred while disabling the server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateScript(Map<String, String> args) {
        if (!args.containsKey("path") || args.get("path").isBlank()) {
            System.out.println("Error: Missing required argument --path <SCRIPT_PATH>.");
            return;
        }

        String scriptPath = args.get("path");
        System.out.println("Checking script at: " + scriptPath);
        
        try {
            // Call ScriptExecutor to validate the script
            boolean isValid = scriptExecutor.validateScript(scriptPath);
    
            if (isValid) {
                System.out.println("Script validated successfully.");
            } else {
                System.out.println("Error: Script validation failed. Check the script for issues.");
            }
        } catch (Exception e) {
            System.out.println("Error: An exception occurred while validating the script: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void exit() {
        System.out.println("Exiting the CLI. Goodbye!");
        isRunning = false;
    }
}
