package main.java.uniba.system_package.backup;

import java.util.Map;

public class SystemStatus {
    private boolean schedulerActive;
    private Map<String, String> lastBackupTimes;
    private Map<String, String> nextBackupTimes;

    public SystemStatus(boolean schedulerActive, Map<String, String> lastBackupTimes, Map<String, String> nextBackupTimes) {
        this.schedulerActive = schedulerActive;
        this.lastBackupTimes = lastBackupTimes;
        this.nextBackupTimes = nextBackupTimes;
    }

    public boolean isSchedulerActive() {
        return schedulerActive;
    }

    public Map<String, String> getLastBackupTimes() {
        return lastBackupTimes;
    }

    public Map<String, String> getNextBackupTimes() {
        return nextBackupTimes;
    }
}
