package uniba.system_package.backup;

/**
 * Represents metadata for a backup operation.
 */
import java.util.UUID;

public class BackupMetadata {
    private String backupId;
    private String targetName;
    private String backupType; // "full" or "incremental"
    private String status; // "success" or "failure"
    private String location;
    private long backupSize;
    private long startTime;
    private long endTime;

    public BackupMetadata(String targetName, String backupType) {
        this.backupId = UUID.randomUUID().toString(); // Generate unique ID
        this.targetName = targetName;
        this.backupType = backupType;
    }

    // Getters and setters

    public String getBackupId() {
        return backupId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getBackupType() {
        return backupType;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getBackupSize() {
        return backupSize;
    }

    public void setBackupSize(long backupSize) {
        this.backupSize = backupSize;
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

    @Override
    public String toString() {
        return "BackupMetadata{" +
                "backupId='" + backupId + '\'' +
                ", targetName='" + targetName + '\'' +
                ", backupType='" + backupType + '\'' +
                ", status='" + status + '\'' +
                ", location='" + location + '\'' +
                ", backupSize=" + backupSize +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
