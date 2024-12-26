package uniba.system_package.backup;

/**
 * A common interface for all backup targets (e.g., servers, databases).
 */
public interface BackupTarget {
    /**
     * Perform the backup operation for the target.
     */
    void performBackup();

    /**
     * Get the name of the backup target.
     *
     * @return The name of the target.
     */
    String getName();
}