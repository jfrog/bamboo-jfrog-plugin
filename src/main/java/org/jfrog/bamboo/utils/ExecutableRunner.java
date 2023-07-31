package org.jfrog.bamboo.utils;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.jfrog.build.extractor.UrlUtils.removeCredentialsFromUrl;

/**
 * Utility class for running executable commands.
 */
public class ExecutableRunner {
    private final File workingDir;
    private final String executable;
    private final Map<String, String> envs;
    private final BuildLog buildLog;
    private final List<String> secrets;
    private final int COMMAND_TIMEOUT = 45; // minutes


    /**
     * Constructs an ExecutableRunner object.
     *
     * @param executable The name or path of the executable command to run.
     * @param workingDir The working directory in which the command should be executed.
     * @param envs       Additional environment variables to set for the command execution.
     * @param buildLog   The logger for capturing command output and logs.
     */
    public ExecutableRunner(String executable, File workingDir, Map<String, String> envs, List<String> secrets, BuildLog buildLog) {
        this.executable = executable;
        this.workingDir = workingDir;
        this.envs = envs;
        this.buildLog = buildLog;
        this.secrets = secrets;
    }

    /**
     * Runs the executable command with the given arguments.
     *
     * @param commandArgs The arguments to pass to the executable command.
     * @return The exit code of the command.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the execution is interrupted.
     */
    public int run(List<String> commandArgs) throws IOException, InterruptedException {
        List<String> fullCommand = new ArrayList<>(commandArgs);
        fullCommand.add(0, executable);

        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand)
                .directory(workingDir)
                .redirectErrorStream(true);
        processBuilder.environment().putAll(envs);

        buildLog.info("Working Directory: " + workingDir);
        buildLog.info("Running command: " + maskSecrets(String.join(" ", processBuilder.command())));

        Process process = processBuilder.start();

        Thread outputReader = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    buildLog.info(maskSecrets(line));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Start the output reader thread
        outputReader.start();

        // Wait for the process to complete with a timeout of 30 minutes
        if (process.waitFor(COMMAND_TIMEOUT, TimeUnit.MINUTES)) {
            // Process completed within the timeout
            outputReader.join(); // Wait for the output reader thread to finish
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                buildLog.error("Command failed with exit code: " + exitCode);
            }
            return exitCode;
        } else {
            // Timeout occurred, terminate the process and the output reader thread
            process.destroy();
            outputReader.interrupt();
            buildLog.error("Command timed out after " + COMMAND_TIMEOUT + " minutes");
            return -1; // Set a custom exit code to indicate timeout
        }
    }

    /**
     * Masks secret values in the given argument string.
     *
     * @param line The argument string to mask secrets in.
     * @return The argument string with masked secrets.
     */
    private String maskSecrets(String line) {
        if (secrets != null && !secrets.isEmpty()) {
            for (String secret : secrets) {
                if (StringUtils.isNotBlank(secret)) {
                    line = line.replaceAll(secret, "***");
                }
            }
        }
        String regex = "--(password|access-token)=\\S+";
        line = line.replaceAll(regex, "--$1=***");
        return removeCredentialsFromUrl(line);
    }
}
