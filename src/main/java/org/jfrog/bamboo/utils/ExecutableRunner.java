package org.jfrog.bamboo.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecutableRunner {
    private final File workingDir;
    private final String executable;
    private final Map<String, String> envs;
    private final BuildLog buildLog;

    public ExecutableRunner(String executable, File workingDir, Map<String, String> envs, BuildLog buildLog) {
        this.executable = executable;
        this.workingDir = workingDir;
        this.envs = envs;
        this.buildLog = buildLog;
    }

    public void run(List<String> commandArgs) throws IOException, InterruptedException {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(executable);
        fullCommand.addAll(commandArgs);
        try {

            // Create the process builder
            ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);

            // Set the additional environment variables from the envs map
            processBuilder.environment().putAll(envs);
            processBuilder.directory(workingDir);
            // Redirect the process output to the console
            processBuilder.redirectErrorStream(true);

            // Start the process
            buildLog.info("Running command: " + maskSecrets(String.join(" ", processBuilder.command())));
            Process process = processBuilder.start();

            // Read the output of the process
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buildLog.info(line);
            }

            // Wait for the process to complete and return exit code
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Command failed with exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            buildLog.error("Failed running command: " + e);
            throw e;
        }
    }

    private String maskSecrets(String arg) {
        return arg.replaceAll("--password=\\S+", "--password=***").
                replaceAll("--access-token=\\S+", "--access-token=***");
    }


}
