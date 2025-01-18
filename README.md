# backup
Automatic configurable backup of internet sites

# Backup System

A backup management tool that automates and manages backups for servers and databases on a configurable schedule. This system supports full and incremental backups, script execution, remote storage (SFTP), and more.

---

## Table of Contents
1. [Features](#features)  
2. [Requirements](#requirements)  
3. [Installation & Setup](#installation--setup)  
4. [Running the CLI](#running-the-cli)  
5. [Available Commands](#available-commands)
6. [Detailed Use & Configuration](#detailed-use--configuration)

---

## Features
- **Full & Incremental Backups**: Supports different backup types based on your schedule or manual triggers.
- **Script Execution**: Runs custom scripts before/after backups.
- **Remote Storage**: Optionally stores backup archives on an SFTP server.
- **Retention Policy**: Automatically cleans up old backups to save space.
- **Email Notifications**: Sends email alerts on backup success or failure.
- **CLI Interface**: Command-line interface to schedule, run, enable/disable, restore, and validate backups/scripts.

---

## Requirements
- **Java 17+**  
- **Maven 3+**  
- **MySQL** (for database backups, if needed)  
- **Quartz** (scheduling, included via Maven dependencies)  
- **SFTP Server** (if using remote backups)  
- **Internet Access** (for email notifications, if configured)

---

## Installation & Setup

1. **Clone the Repository**
    ```bash
    git clone https://github.com/your-username/backup-system.git
    ```

2. **Navigate to the Project Folder**
    ```bash
    cd backup-system
    ```

3. **Build with Maven**
    ```bash
    mvn clean package
    ```

4. **Edit the Configuration File**
    - Open `src/main/resources/config.yaml` and adjust servers, databases, schedules, retention policies, remote storage, and email settings to match your environment.

5. **Check Scripts**
    - Ensure your pre- and post-backup scripts (if any) are executable (`.sh` on Linux/macOS, `.bat` on Windows).

---

## Running the CLI

1. **Package and Run**
    - After building, run the application:
      ```bash
      java -jar target/Backup_System-1.0-SNAPSHOT.jar
      ```
    - You should see:
      ```
      Welcome to the Backup System CLI. Type 'help' for a list of commands.
      cli>
      ```

2. **Interactive Mode**
    - Type commands directly in the CLI prompt, for example:
      ```
      run_backup --target Server1
      ```

3. **Scheduling**
    - If you want automated backups, ensure your cron expressions are set properly in `config.yaml`. The scheduler will start automatically when the application runs.

---

## Available Commands

| Command            | Syntax / Example                                                     | Description                                                                        |
|--------------------|----------------------------------------------------------------------|------------------------------------------------------------------------------------|
| **help**           | `help` or `help --cmd <COMMAND_NAME>`                                | Shows all commands or detailed help for a specific command.                        |
| **schedule_backup**| `schedule_backup --target <TARGET_NAME> --type <TYPE> --schedule "<CRON_EXPRESSION>"` | Schedules a new backup with a cron expression.                    |
| **run_backup**     | `run_backup --target <TARGET_NAME> [--type full\|incremental]`       | Manually triggers a backup for a specified target.                                 |
| **restore_backup** | `restore_backup --id <BACKUP_ID>`                                    | Restores data from a backup identified by its ID.                                  |
| **status**         | `status`                                                             | Displays overall system status, e.g., scheduler state, last backups, next backups. |
| **list_backups**   | `list_backups`                                                       | Lists stored backups (requires a method in `BackupManager` to retrieve them).      |
| **enable_server**  | `enable_server --target <TARGET_NAME>`                               | Re-enables backups for a server (if previously disabled).                          |
| **disable_server** | `disable_server --target <TARGET_NAME>`                              | Disables backups for a server so they are not included in backup jobs.             |
| **validate_script**| `validate_script --path <SCRIPT_PATH>`                               | Validates a script file to ensure it’s executable.                                 |
| **exit**           | `exit`                                                               | Exits the CLI.                                                                     |

---

## 6. Detailed Use & Configuration

This section explains how the system uses your `config.yaml` file and what is expected from the administrator versus what the system handles automatically.

### 6.1 Configuration File: `config.yaml`
- **Servers**: Define each server's name, host, user, password, paths to backup, and optional pre-/post-backup scripts.  
- **Databases**: Similar to servers, but also specify the database type (`mysql`) for creating dumps.  
- **Schedule**: Set cron expressions for full and incremental backups (e.g., `"0 2 * * 0"` for every Sunday at 2 AM).  
- **RetentionPolicy**: Configure how many full or incremental backups to keep (`fullBackupsToKeep`, `incrementalBackupsToKeep`). Old backups beyond this limit are automatically deleted.  
- **RemoteStorage**: Optionally provide SFTP settings (host, user, password, remotePath) if you want backups uploaded off-site.  
- **Email**: Provide SMTP details if you want notifications on backup results.

### 6.2 What the Administrator Does
1. **Edit `config.yaml`**: Make sure each server or database is listed, including any needed scripts or paths.  
2. **Start the Application**: Run `java -jar target/Backup_System-1.0-SNAPSHOT.jar`, which initializes and reads `config.yaml`.  
3. **Use the CLI Commands**: From the CLI, schedule additional backups, run manual backups, restore data, enable/disable servers, etc.  
4. **Monitor Logs & Emails**: Check the console or log files for backup outcomes, or watch your inbox if email notifications are configured.

### 6.3 What the System Handles Automatically
1. **Scheduled Backups**: Once the application is running, Quartz handles recurring cron jobs for full and incremental backups based on the schedule in `config.yaml`.  
2. **Pre-/Post-Scripts**: Automatically runs any scripts specified for a server or database before and after each backup.  
3. **Retention Policies**: After each backup, the system checks and deletes older backups exceeding the keep limits.  
4. **Remote Upload**: If `RemoteStorage` is configured, the system uploads the newly created backup archives to the specified SFTP path.  
5. **Email Notifications**: For each backup (or error), the system sends an email with relevant information, if configured.

By **updating `config.yaml`** and **running the CLI**, you can manage your backups with minimal effort. 
The system automates recurring backups and related tasks, so you only need to monitor and occasionally run manual commands.
