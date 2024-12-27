package uniba.system_package.storage;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import uniba.system_package.utils.LogManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;

public class StorageManager {
    private static final Logger logger = LogManager.getLogger(StorageManager.class);

    /**
     * Creates a database dump and stores it at the specified path.
     */
    public boolean createDatabaseDump(String type, String host, String user, String password, String dumpFilePath) {
        logger.info("Initiating database dump: Type={}, Host={}, User={}", type, host, user);

        try {
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
     */
    public boolean compressFiles(List<String> filePaths, String archivePath) {
        logger.info("Starting file compression into: {}", archivePath);

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(archivePath))) {
            for (String filePath : filePaths) {
                Path path = Paths.get(filePath);

                // Handle symbolic links
                if (Files.isSymbolicLink(path)) {
                    logger.info("Storing symbolic link: {}", filePath);
                    ZipEntry linkEntry = new ZipEntry(filePath + "/");
                    zipOut.putNextEntry(linkEntry);
                    zipOut.write(Files.readSymbolicLink(path).toString().getBytes());
                    zipOut.closeEntry();
                    continue;
                }

                // Handle regular files
                File fileToCompress = path.toFile();
                if (!fileToCompress.exists()) {
                    logger.warn("File not found: {}. Skipping.", filePath);
                    continue;
                }

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
     * Uploads a backup file to a remote server via SFTP.
     */
    public boolean uploadToSFTP(String localFilePath, String remoteDir, String sftpHost, String sftpUser, String sftpPassword) {
        JSch jsch = new JSch();
        try {
            Session session = jsch.getSession(sftpUser, sftpHost, 22);
            session.setPassword(sftpPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            channelSftp.put(localFilePath, remoteDir);
            channelSftp.disconnect();
            session.disconnect();

            logger.info("File uploaded to SFTP server: {}", remoteDir);
            return true;
        } catch (Exception e) {
            logger.error("Error uploading file to SFTP server: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Deletes old backup files based on retention policies.
     */
    public void deleteOldBackups(List<String> backupFiles, int backupsToKeep) {
        if (backupFiles.size() <= backupsToKeep) {
            return; // No cleanup needed
        }

        backupFiles.sort(Comparator.naturalOrder());
        List<String> filesToDelete = backupFiles.subList(0, backupFiles.size() - backupsToKeep);

        for (String file : filesToDelete) {
            try {
                Files.delete(Paths.get(file));
                logger.info("Deleted old backup file: {}", file);
            } catch (IOException e) {
                logger.error("Failed to delete old backup file: {}", file, e);
            }
        }
    }

    /**
     * Lists all backup files in the specified directory.
     */
    public List<String> listBackupFiles(String backupDirectory) {
        logger.info("Fetching list of backup files in directory: {}", backupDirectory);
        try {
            return Files.walk(Paths.get(backupDirectory))
                    .filter(Files::isRegularFile)
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error while listing backup files in directory {}: {}", backupDirectory, e.getMessage());
            return List.of();
        }
    }
}
