# backup
Automatic configurable backup of internet sites

# Backup System

This project is a **command-line Backup System** designed to help administrators **automate** and **manage** backups for servers and databases. The system supports **full** and **incremental** backups, uses **cron scheduling** (via Quartz), and can **upload** backups to a remote server using **SFTP**.

## Table of Contents

1. [Features](#features)  
2. [Requirements](#requirements)  
3. [Installation & Setup](#installation--setup)  
4. [Running the CLI](#running-the-cli)  
5. [Available Commands](#available-commands)  

---

## Features

- **Automated Backups**: Schedule backups using cron-like expressions.  
- **Manual Backups**: Run on-demand backups for individual targets.  
- **Retention Policy**: Automatically remove old backups to save space.  
- **Script Execution**: Run pre- and post-backup scripts.  
- **Remote Storage**: Upload backups to a remote server via SFTP.  
- **Logging & Notifications**: Log events and send email alerts.

---

## Requirements

1. **Java 17** or higher.  
2. **Maven** for building the project.  
3. A **config.yaml** file (see [Configuration File](#configuration-file)) placed in the `resources` folder or alongside the JAR.

---

## Installation & Setup

1. **Clone the Repository**  
   ```bash
   git clone https://github.com/YourUsername/YourBackupSystem.git
   cd YourBackupSystem
2. **Build the Project**
  mvn clean package

This creates a runnable JAR in the target/ folder, for example:
  Backup_System-1.0-SNAPSHOT-shaded.jar

3. **Prepare Configuration**
  Make sure you have a valid config.yaml (see Configuration File) in the same directory as your JAR, or inside src/main/resources.

## Running the CLI

1. **Navigate to the Target Folder**
  cd target

2. **Run the JAR**
  java -jar Backup_System-1.0-SNAPSHOT-shaded.jar

  You should see:
  Welcome to the Backup System CLI. Type 'help' for a list of commands.
  cli>
  
3. **Type Commands**
  help
  run_backup --target Server1 --type full
  etc.

## Available Commands

  help
  Shows all available commands or detailed help for a specific command.
