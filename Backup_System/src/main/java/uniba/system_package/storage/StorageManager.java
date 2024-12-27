package uniba.system_package.storage;

import org.slf4j.Logger;
import uniba.system_package.utils.LogManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class StorageManager {
    private static final Logger logger = LogManager.getLogger(StorageManager.class);

    /**
     * Creates a database dump and stores it at the specified path.
     *
     * @param type         The type of database (e.g., MySQL, PostgreSQL).
     * @param host         The database host.
     * @param user         The database username.
     * @param password     The database password.
     * @param dumpFilePath The file path where the database dump will be stored.
     * @return True if the dump is successful, false otherwise.
     */
    public boolean createDatabaseDump(String type, String host, String user, String password, String dumpFilePath) {
        logger.info("Initiating database dump: Type={}, Host={}, User={}", type, host, user);

        try {
            // Example logic for MySQL database
            if (type.equalsIgnoreCase("MySQL")) {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "mysqldump",
                        "--host=" + host,
                        "--user=" + user,
                        "--password=" + password,
                        "--all-databases"
                );
                processBuilder.redirectOutput(new File(dumpFilePath));
                Process process = processBuilder.start();
                int exitCode = process.waitFor();

                if (exitCode != 0) {
                    logger.error("Database dump failed with exit code: {}", exitCode);
                    return false;
                }
            } else {
                logger.error("Unsupported database type: {}", type);
                return false;
            }

            logger.info("Database dump successfully created at: {}", dumpFilePath);
            return true;
        } catch (IOException | InterruptedException e) {
            logger.error("Error creating database dump: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Compresses a list of files into a single archive, handling symbolic links.
     *
     * @param filePaths   List of file paths to include in the archive.
     * @param archivePath The path for the compressed archive file.
     * @return True if compression is successful, false otherwise.
     */
    public boolean compressFiles(List<String> filePaths, String archivePath) {
        logger.info("Starting file compression into: {}", archivePath);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(archivePath))) {
            for (String filePath : filePaths) {
                Path path = Paths.get(filePath);

                if (Files.isSymbolicLink(path)) { // Lines 5-12
                    logger.info("Storing symbolic link: {}", filePath);
                    ZipEntry linkEntry = new ZipEntry(path.toString() + "/");
                    zipOut.putNextEntry(linkEntry);
                    zipOut.write(Files.readSymbolicLink(path).toString().getBytes());
                    zipOut.closeEntry();
                    continue;
                }

                File fileToCompress = path.toFile();
                if (!fileToCompress.exists()) {
                    logger.warn("File not found: {}. Skipping.", filePath);
                    continue;
                }
                // Regular file compression logic
                try (var fileInputStream = Files.newInputStream(path)) {
                    ZipEntry zipEntry = new ZipEntry(fileToCompress.getName());
                    zipOut.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fileInputStream.read(buffer)) >= 0) {
                        zipOut.write(buffer, 0, length);
                    }
                }
            }
            logger.info("Compression completed successfully.");
            return true;
        } catch (IOException e) {
            logger.error("Error during file compression: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Stores the backup at a remote location.
     *
     * @param localPath  The local path of the backup file.
     * @param remotePath The remote destination path for the backup.
     * @return True if the backup is successfully stored remotely, false otherwise.
     */
    public boolean storeBackup(String localPath, String remotePath) {
        logger.info("Storing backup from {} to remote location {}", localPath, remotePath);

        try {
            // Simulate SFTP or other remote storage logic here.
            // Replace the below simulation with actual SFTP or cloud storage SDK logic.

            File localFile = new File(localPath);
            if (!localFile.exists()) {
                logger.error("Local backup file not found: {}", localPath);
                return false;
            }

            // Simulating remote storage success
            logger.info("Backup successfully stored at remote location: {}", remotePath);
            return true;
        } catch (Exception e) {
            logger.error("Error storing backup remotely: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Lists all backup files in the specified directory.
     *
     * @param backupDirectory The directory to search for backup files.
     * @return A list of file paths as strings.
     */
    public List<String> listBackupFiles(String backupDirectory) {
        logger.info("Fetching list of backup files in directory: {}", backupDirectory);
        try {
            return Files.walk(Paths.get(backupDirectory))
                    .filter(Files::isRegularFile) // Only include files, no directories
                    .map(Path::toString) // Convert Path objects to String
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error while listing backup files in directory {}: {}", backupDirectory, e.getMessage());
            return List.of(); // Return an empty list on error
        }
    }
}
