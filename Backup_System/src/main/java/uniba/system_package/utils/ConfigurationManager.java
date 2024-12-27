package uniba.system_package.utils;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.util.List;


public class ConfigurationManager {
    private Config config;

    // Load the configuration file
    private void loadConfiguration(String configFilePath) {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(configFilePath)) {
            Yaml yaml = new Yaml();
            this.config = yaml.loadAs(inputStream, Config.class);
        } catch (Exception e) {
            throw new RuntimeException("Error loading configuration file: " + configFilePath, e);
        }
    }

    public Config.Server getServer(String name) {
        return config.getServers().stream()
                .filter(server -> server.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Server not found: " + name));
    }

    public List<Config.Database> getAllDatabases() {
        return config.getDatabases();
    }

    public void validateConfig() {
        // Validate schedules
        if (config.getSchedule().getFullBackup() == null || config.getSchedule().getIncrementalBackup() == null) {
            throw new IllegalArgumentException("Schedules must include both full and incremental backups.");
        }

        // Validate retention policies
        Config.RetentionPolicy retentionPolicy = config.getRetentionPolicy();
        if (retentionPolicy.getFullBackupsToKeep() <= 0) {
            throw new IllegalArgumentException("Retention policy must specify a positive number of full backups to keep.");
        }
        if (retentionPolicy.getIncrementalBackupsToKeep() <= 0) {
            throw new IllegalArgumentException("Retention policy must specify a positive number of incremental backups to keep.");
        }

        // Validate remote storage
        Config.RemoteStorage remoteStorage = config.getRemoteStorage();
        if (remoteStorage.getHost() == null || remoteStorage.getUser() == null || remoteStorage.getPassword() == null) {
            throw new IllegalArgumentException("Remote storage configuration must include host, user, and password.");
        }
    }


    // Get the list of servers
    public List<Config.Server> getServers() {
        return config.getServers();
    }

    // Get the list of databases
    public List<Config.Database> getDatabases() {
        return config.getDatabases();
    }

    // Get remote storage settings
    public Config.RemoteStorage getRemoteStorage() {
        return config.getRemoteStorage();
    }

    // Get retention policy
    public Config.RetentionPolicy getRetentionPolicy() {
        return config.getRetentionPolicy();
    }

    // Get schedule settings
    public Config.Schedule getSchedule() {
        return config.getSchedule();
    }

    public Config.Email getEmail() {
        return config.getEmail();
    }

    // Inner static classes for mapping the YAML structure
    public static class Config {
        private List<Server> servers;
        private List<Database> databases;
        private RemoteStorage remoteStorage;
        private RetentionPolicy retentionPolicy;
        private Schedule schedule;
        private Email email; // Add email configuration


        // Getters and Setters
        public List<Server> getServers() {
            return servers;
        }

        public void setServers(List<Server> servers) {
            this.servers = servers;
        }

        public List<Database> getDatabases() {
            return databases;
        }

        public void setDatabases(List<Database> databases) {
            this.databases = databases;
        }

        public RemoteStorage getRemoteStorage() {
            return remoteStorage;
        }

        public void setRemoteStorage(RemoteStorage remoteStorage) {
            this.remoteStorage = remoteStorage;
        }

        public RetentionPolicy getRetentionPolicy() {
            return retentionPolicy;
        }

        public void setRetentionPolicy(RetentionPolicy retentionPolicy) {
            this.retentionPolicy = retentionPolicy;
        }

        public Schedule getSchedule() {
            return schedule;
        }

        public void setSchedule(Schedule schedule) {
            this.schedule = schedule;
        }


        public Email getEmail() {
            return email;
        }

        public void setEmail(Email email) {
            this.email = email;
        }

        // Schedule class for cron expressions
        public static class Schedule {
            private String fullBackup;          // Cron for full backups
            private String incrementalBackup;   // Cron for incremental backups

            public String getFullBackup() {
                return fullBackup;
            }

            public void setFullBackup(String fullBackup) {
                this.fullBackup = fullBackup;
            }

            public String getIncrementalBackup() {
                return incrementalBackup;
            }

            public void setIncrementalBackup(String incrementalBackup) {
                this.incrementalBackup = incrementalBackup;
            }
        }

        public static class RemoteStorage {
            private String host;
            private String user;
            private String password;
            private String remotePath;

            // Getters and Setters
            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getRemotePath() {
                return remotePath;
            }

            public void setRemotePath(String remotePath) {
                this.remotePath = remotePath;
            }
        }

        public static class RetentionPolicy {
            private int fullBackupsToKeep;
            private int incrementalBackupsToKeep;

            // Getters and Setters
            public int getFullBackupsToKeep() {
                return fullBackupsToKeep;
            }

            public void setFullBackupsToKeep(int fullBackupsToKeep) {
                this.fullBackupsToKeep = fullBackupsToKeep;
            }

            public int getIncrementalBackupsToKeep() {
                return incrementalBackupsToKeep;
            }

            public void setIncrementalBackupsToKeep(int incrementalBackupsToKeep) {
                this.incrementalBackupsToKeep = incrementalBackupsToKeep;
            }
        }

        public static class Server {
            private String name;
            private String host;
            private String user;
            private String password;
            private List<String> pathsToBackup;
            private String preBackupScript; // Pre-backup script
            private String postBackupScript; // Post-backup script

            // Getters and Setters
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public List<String> getPathsToBackup() {
                return pathsToBackup;
            }

            public void setPathsToBackup(List<String> pathsToBackup) {
                this.pathsToBackup = pathsToBackup;
            }

            public String getPreBackupScript() {
                return preBackupScript;
            }

            public void setPreBackupScript(String preBackupScript) {
                this.preBackupScript = preBackupScript;
            }

            public String getPostBackupScript() {
                return postBackupScript;
            }

            public void setPostBackupScript(String postBackupScript) {
                this.postBackupScript = postBackupScript;
            }
        }

        public static class Database {
            private String name;
            private String type;
            private String host;
            private String user;
            private String password;
            private String preBackupScript; // Pre-backup script
            private String postBackupScript; // Post-backup script

            // Getters and Setters
            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getHost() {
                return host;
            }

            public void setHost(String host) {
                this.host = host;
            }

            public String getUser() {
                return user;
            }

            public void setUser(String user) {
                this.user = user;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getPreBackupScript() {
                return preBackupScript;
            }

            public void setPreBackupScript(String preBackupScript) {
                this.preBackupScript = preBackupScript;
            }

            public String getPostBackupScript() {
                return postBackupScript;
            }

            public void setPostBackupScript(String postBackupScript) {
                this.postBackupScript = postBackupScript;
            }
        }

        public static class Email {
            private String smtpHost;
            private int smtpPort;
            private String username;
            private String password;
            private String fromAddress;
            private List<String> toAddresses;

            // Getters and Setters
            public String getSmtpHost() {
                return smtpHost;
            }

            public void setSmtpHost(String smtpHost) {
                this.smtpHost = smtpHost;
            }

            public int getSmtpPort() {
                return smtpPort;
            }

            public void setSmtpPort(int smtpPort) {
                this.smtpPort = smtpPort;
            }

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public String getFromAddress() {
                return fromAddress;
            }

            public void setFromAddress(String fromAddress) {
                this.fromAddress = fromAddress;
            }

            public List<String> getToAddresses() {
                return toAddresses;
            }

            public void setToAddresses(List<String> toAddresses) {
                this.toAddresses = toAddresses;
            }
        }
    }
}
