package uniba.system_package.scripts;

import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

import java.io.File;

public class ScriptExecutor {
    private static final Logger logger = LogManager.getLogger(ScriptExecutor.class);

    /**
     * Validates the script at the given path.
     *
     * @param scriptPath The path to the script.
     * @return True if the script is valid, otherwise false.
     */
    public boolean validateScript(String scriptPath) {
        File scriptFile = new File(scriptPath);

        // Validate the script file existence
        if (!scriptFile.exists()) {
            logger.error("Script file not found: {}", scriptPath);
            return false;
        }

        // Validate if the file is a regular file
        if (!scriptFile.isFile()) {
            logger.error("Invalid script file (not a regular file): {}", scriptPath);
            return false;
        }

        // Validate execute permissions
        if (!scriptFile.canExecute()) {
            logger.error("Script file is not executable: {}", scriptPath);
            return false;
        }

        logger.info("Script at {} is valid.", scriptPath);
        return true;
    }

    /**
     * Executes a script at the given path.
     *
     * @param scriptPath The path to the script.
     * @return True if the script executes successfully, otherwise false.
     */
    public boolean executeScript(String scriptPath) {
        File scriptFile = new File(scriptPath);

        // Validate the script file
        if (!validateScript(scriptPath)) {
            logger.error("Script validation failed: {}", scriptPath);
            return false;
        }

        // Execute the script
        try {
            Process process = new ProcessBuilder(scriptPath).start();

            // Wait for the process to complete
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("Script executed successfully: {}", scriptPath);
                return true;
            } else {
                logger.error("Script execution failed with exit code {}: {}", exitCode, scriptPath);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error executing script: {}", scriptPath, e);
            return false;
        }
    }
}
