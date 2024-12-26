package uniba.system_package.backup;

/**
 * Represents metadata for a backup operation.
 */

public class BackupMetadata {
    private String targetName;
    private String backupType; // New attribute
    private String status;
    private long startTime;
    private long endTime;
    private long backupSize;
    private String location;

    // Constructor
    public BackupMetadata(String targetName, String backupType) {
        this.targetName = targetName;
        this.backupType = backupType;
    }

    // Getters and Setters
    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getBackupType() {
        return backupType; // Getter for backupType
    }

    public void setBackupType(String backupType) {
        this.backupType = backupType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getBackupSize() {
        return backupSize;
    }

    public void setBackupSize(long backupSize) {
        this.backupSize = backupSize;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // Calculate backup duration
    public long getDuration() {
        return endTime - startTime;
    }

    @Override
    public String toString() {
        return String.format(
                "BackupMetadata[targetName=%s, backupType=%s, status=%s, duration=%d ms, location=%s, size=%d bytes]",
                targetName, backupType, status, getDuration(), location, backupSize
        );
    }
}

