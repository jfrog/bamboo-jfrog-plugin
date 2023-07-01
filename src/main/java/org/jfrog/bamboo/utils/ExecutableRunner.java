package org.jfrog.bamboo.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for running executable commands.
 */
public class ExecutableRunner {
    private final File workingDir;
    private final String executable;
    private final Map<String, String> envs;
    private final BuildLog buildLog;

    /**
     * Constructs an ExecutableRunner object.
     *
     * @param executable The name or path of the executable command to run.
     * @param workingDir The working directory in which the command should be executed.
     * @param envs       Additional environment variables to set for the command execution.
     * @param buildLog   The logger for capturing command output and logs.
     */
    public ExecutableRunner(String executable, File workingDir, Map<String, String> envs, BuildLog buildLog) {
        this.executable = executable;
        this.workingDir = workingDir;
        this.envs = envs;
        this.buildLog = buildLog;
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

        buildLog.info("Running command: " + maskSecrets(String.join(" ", processBuilder.command())));

        Process process = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buildLog.info(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            buildLog.error("Command failed with exit code: " + exitCode);
        }
        return exitCode;
    }

    /**
     * Masks secret values in the given argument string.
     *
     * @param arg The argument string to mask secrets in.
     * @return The argument string with masked secrets.
     */
    private String maskSecrets(String arg) {
        return arg.replaceAll("--password=\\S+", "--password=***")
                .replaceAll("--access-token=\\S+", "--access-token=***");
    }
}
