package uniba.system_package.storage;

import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class StorageManager {
    private static final Logger logger = LogManager.getLogger(StorageManager.class);

    /**
     * Compresses a list of files into a single zip archive.
     *
     * @param paths      List of file paths to compress.
     * @param outputPath Path to save the compressed archive.
     * @return True if compression is successful, false otherwise.
     */
    public boolean compressFiles(List<String> paths, String outputPath) {
        try {
            // Ensure backup directory exists
            File outputFile = new File(outputPath);
            outputFile.getParentFile().mkdirs();

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputPath))) {
                for (String path : paths) {
                    File file = new File(path);
                    if (!file.exists()) {
                        logger.error("File not found: {}", path);
                        continue;
                    }
                    logger.info("Adding file to archive: {}", path);
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
            logger.info("Compression completed. Archive saved to: {}", outputPath);
            return true;
        } catch (Exception e) {
            logger.error("Error during compression: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Saves a backup to remote storage.
     *
     * @param localPath  Path of the local backup file.
     * @param remotePath Path where the backup should be stored remotely.
     * @return True if storage is successful, false otherwise.
     */
    public boolean storeBackup(String localPath, String remotePath) {
        // Simulate remote storage logic (e.g., SFTP)
        logger.info("Storing backup from {} to remote path: {}", localPath, remotePath);
        try {
            // Simulated storage logic (replace with actual SFTP or cloud storage logic)
            Thread.sleep(500);
            logger.info("Backup successfully stored at remote location: {}", remotePath);
            return true;
        } catch (Exception e) {
            logger.error("Error storing backup remotely: {}", e.getMessage(), e);
            return false;
        }
    }
}
