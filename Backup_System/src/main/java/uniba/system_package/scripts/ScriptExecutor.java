package uniba.system_package.scripts;

import uniba.system_package.utils.LogManager;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class ScriptExecutor {
    private static final Logger logger = LogManager.getLogger(ScriptExecutor.class);

    /**
     * Executes a script at the given path.
     *
     * @param scriptPath The path to the script.
     * @return True if the script executes successfully, otherwise false.
     */
    public boolean executeScript(String scriptPath) {
        File scriptFile = new File(scriptPath);

        // Validate the script file
        if (!scriptFile.exists()) {
            logger.error("Script file not found: {}", scriptPath);
            return false;
        }
        if (!scriptFile.canExecute()) {
            logger.error("Script file is not executable: {}", scriptPath);
            return false;
        }

        // Execute the script
        try {
            Process process = new ProcessBuilder(scriptPath).start();

            // Capture the output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("Script output: {}", line);
                }
            }

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
