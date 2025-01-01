package uniba.system_package.cli;

import uniba.system_package.backup.BackupManager;
import uniba.system_package.utils.ConfigurationManager;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

import java.util.*;

public class CLI {
    private BackupManager backupManager;
    private ConfigurationManager configurationManager;
    private boolean isRunning;

    // This class holds the command and any arguments parsed from user input.
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

    // Starts the CLI loop.
    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println();
        System.out.println("Welcome to the Backup System CLI. Type 'help' for a list of commands.");

        while (isRunning) {
            System.out.print("cli> ");
            String input = scanner.nextLine();

            // Parse user input, then handle it.
            ParsedCommand parsed = parseInput(input);
            handleCommand(parsed);
        }
    }

    private ParsedCommand parseInput(String input) {
        List<String> tokens = new ArrayList<>();
        // Regular expression to match quoted strings or non-space sequences
        Matcher m = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(input);
        while (m.find()) {
            if (m.group(1) != null) {
                // Quoted string without the quotes
                tokens.add(m.group(1));
            } else {
                // Unquoted word
                tokens.add(m.group(2));
            }
        }

        if (tokens.isEmpty()) {
            return new ParsedCommand("", new HashMap<>());
        }

        int index = 0;
        // If the first token is "backup-system", skip it
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

    // Handles commands and calls the right methods.
    private void handleCommand(ParsedCommand parsedCommand) {
        String command = parsedCommand.getCommand();
        Map<String, String> args = parsedCommand.getArgs();

        // If there's no command, ask the user to type 'help'.
        if (command.isEmpty()) {
            System.out.println("No command detected. Type 'help' for a list of commands.");
            return;
        }

        // Special check for "help", since we also support "help --cmd <someCommand>" for details.
        if (command.equalsIgnoreCase("help")) {
            String subCommand = args.get("cmd");
            displayHelp(subCommand);
            return;
        }

        // All other commands go here.
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
            case "configure_settings":
                configureSettings(args);
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

    // Shows help info. If 'subCommand' is empty, show everything.
    // Otherwise, show detailed help for that subCommand.
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
            System.out.println("  configure_settings  Change settings (like retention policy).");
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
                case "configure_settings":
                    System.out.println();
                    System.out.println("Usage: configure_settings --setting <SETTING_NAME> --value <NEW_VALUE> [--target <TARGET_NAME>]");
                    System.out.println("Example: configure_settings --setting retention_policy --value 5");
                    System.out.println("Changes certain settings on the fly.");
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
        // Validate required arguments: --target, --type, --schedule
        if (!args.containsKey("target") || !args.containsKey("type") || !args.containsKey("schedule")) {
            System.out.println("Error: Missing arguments.");
            System.out.println("Syntax: schedule_backup --target <TARGET_NAME> --type <TYPE> --schedule \"<CRON_EXPRESSION>\"");
            return;
        }

        String targetName = args.get("target");
        String backupType = args.get("type");
        String cronExpression = args.get("schedule");

        // Basic validation for backupType
        if (!"full".equalsIgnoreCase(backupType) && !"incremental".equalsIgnoreCase(backupType)) {
            System.out.println("Error: Invalid backup type. Must be 'full' or 'incremental'.");
            return;
        }

        // Log the command execution
        System.out.println("Scheduling a " + backupType + " backup for target '" + targetName + "' with cron '" + cronExpression + "'.");

        // Call the BackupManager's scheduling method
        boolean success = backupManager.scheduleBackupForTarget(targetName, backupType, cronExpression);

        // Notify the user
        if (success) {
            System.out.println("Backup scheduled successfully.");
        } else {
            System.out.println("Failed to schedule backup. Check logs for details.");
        }
    }



    private void runBackup(Map<String, String> args) {
        // 1) Validate required arguments
        if (!args.containsKey("target") || args.get("target") == null || args.get("target").isBlank()) {
            System.out.println("Error: Missing required argument --target <TARGET_NAME>.");
            return;
        }

        // Optionally check for --type as well. If not provided, default to 'full' or ask the user.
        String backupType = "full"; // default
        if (args.containsKey("type") && !args.get("type").isBlank()) {
            backupType = args.get("type");
        }

        String targetName = args.get("target");
        System.out.println("Running a manual backup for: " + targetName + " with type: " + backupType);

        // 2) Call the new method in BackupManager
        boolean success = backupManager.startBackupForTarget(backupType, targetName);

        // 3) Notify user of the result
        if (success) {
            System.out.println("Manual backup completed successfully for: " + targetName);
        } else {
            System.out.println("Manual backup failed or target not found: " + targetName);
        }
    }


    private void restoreBackup(Map<String, String> args) {
        if (!args.containsKey("id") || args.get("id").isBlank()) {
            System.out.println("Error: Missing required argument --id <BACKUP_ID>.");
            return;
        }

        String backupId = args.get("id");
        System.out.println("Restoring backup with ID: " + backupId);
        // TODO: Implement restore logic here
    }

    private void displayStatus() {
        System.out.println("Showing system status...");
        // TODO: Show next scheduled backups, last successful backup, etc.
    }

    private void listBackups() {
        System.out.println("Showing all backups we have...");
        // TODO: Query and print backup metadata
    }

    private void configureSettings(Map<String, String> args) {
        if (!args.containsKey("setting") || !args.containsKey("value")) {
            System.out.println("Error: Missing arguments.");
            System.out.println("Syntax: configure_settings --setting <SETTING_NAME> --value <NEW_VALUE> [--target <TARGET_NAME>]");
            return;
        }

        String setting = args.get("setting");
        String value = args.get("value");
        String targetName = args.getOrDefault("target", "");

        System.out.println("Changing setting '" + setting + "' to '" + value + "' for target: " + targetName);
        // TODO: Actually update config or objects in memory
    }

    private void enableServer(Map<String, String> args) {
        if (!args.containsKey("target") || args.get("target").isBlank()) {
            System.out.println("Error: Missing required argument --target <TARGET_NAME>.");
            return;
        }

        String targetName = args.get("target");
        System.out.println("Enabling server: " + targetName);
        // TODO: Mark this server as enabled
    }

    private void disableServer(Map<String, String> args) {
        if (!args.containsKey("target") || args.get("target").isBlank()) {
            System.out.println("Error: Missing required argument --target <TARGET_NAME>.");
            return;
        }

        String targetName = args.get("target");
        System.out.println("Disabling server: " + targetName);
        // TODO: Mark this server as disabled
    }

    private void validateScript(Map<String, String> args) {
        if (!args.containsKey("path") || args.get("path").isBlank()) {
            System.out.println("Error: Missing required argument --path <SCRIPT_PATH>.");
            return;
        }

        String scriptPath = args.get("path");
        System.out.println("Checking script at: " + scriptPath);
        // TODO: Test-run the script or do some checks
    }

    private void exit() {
        System.out.println("Exiting the CLI. Goodbye!");
        isRunning = false;
    }
}
