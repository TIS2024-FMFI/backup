package uniba.system_package.storage;

import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class RetentionPolicy {
    private static final Logger logger = LogManager.getLogger(RetentionPolicy.class);

    /**
     * Deletes old backups based on the retention policy.
     *
     * @param backups   List of backup file paths.
     * @param maxToKeep Maximum number of backups to retain.
     */
    public void deleteOldBackups(List<String> backups, int maxToKeep) {
        if (backups.size() <= maxToKeep) {
            logger.info("No old backups to delete. Total backups: {}", backups.size());
            return;
        }

        // Sort backups by last modified date (oldest first)
        backups.sort((b1, b2) -> {
            File file1 = new File(b1);
            File file2 = new File(b2);
            return Long.compare(file1.lastModified(), file2.lastModified());
        });

        // Delete backups exceeding the maxToKeep limit
        for (int i = 0; i < backups.size() - maxToKeep; i++) {
            File backupToDelete = new File(backups.get(i));
            if (backupToDelete.delete()) {
                logger.info("Deleted old backup: {}", backupToDelete.getPath());
            } else {
                logger.error("Failed to delete old backup: {}", backupToDelete.getPath());
            }
        }
    }
}
