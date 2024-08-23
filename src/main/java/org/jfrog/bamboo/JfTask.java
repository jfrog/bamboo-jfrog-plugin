package org.jfrog.bamboo;

import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.config.ServerConfig;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.jfrog.bamboo.utils.BambooUtils;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.bamboo.utils.ExecutableRunner;
import org.jfrog.bamboo.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JFrog CLI Task.
 */
public class JfTask extends JfContext implements TaskType {
    private BuildLog buildLog;
    private ServerConfigManager serverConfigManager;
    private ExecutableRunner commandRunner;
    private CustomVariableContext customVariableContext;
    private PluginAccessor pluginAccessor;
    private AdministrationConfiguration administrationConfiguration;
    private AdministrationConfigurationAccessor administrationConfigurationAccessor;

    /**
     * Executes the JFrog CLI Task.
     *
     * @param taskContext The task context.
     * @return The task result.
     */
    @Override
    public @NotNull TaskResult execute(final @NotNull TaskContext taskContext) {
        buildLog = new BuildLog(taskContext.getBuildLogger());
        serverConfigManager = ServerConfigManager.getInstance();
        ConfigurationMap confMap = taskContext.getConfigurationMap();
        TaskResultBuilder resultBuilder = TaskResultBuilder.newBuilder(taskContext);
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(confMap.get(JF_TASK_SERVER_ID));
        if (selectedServerConfig == null) {
            buildLog.error("The selected Server ID doesn't exists: " + confMap.get(JF_TASK_SERVER_ID));
            return resultBuilder.failedWithError().build();
        }
        try {
            String jfrogTmpDir = BambooUtils.getJfrogTmpDir(customVariableContext);

            // Download CLI (if needed) and retrieve path
            String jfExecutablePath = JfInstaller.getJfExecutable(selectedServerConfig, jfrogTmpDir, buildLog);

            // Create commandRunner to run JFrog CLI commands
            Map<String, String> envs = createJfrogEnvironmentVariables(taskContext.getBuildContext(), selectedServerConfig);
            File workingDir = getWorkingDirectory(confMap.get(JF_TASK_WORKING_DIRECTORY), taskContext.getWorkingDirectory());
            buildLog.info("Working directory: " + workingDir);
            List<String> secrets = List.of(selectedServerConfig.getPassword(), selectedServerConfig.getAccessToken());
            commandRunner = new ExecutableRunner(jfExecutablePath, workingDir, envs, secrets, buildLog);

            // Run 'jf config add' and 'jf config use' commands.
            int exitCode = configAllJFrogServers();
            if (exitCode != 0) {
                return resultBuilder.failedWithError().build();
            }

            // Make selected Server ID as default (by 'jf c use')
            exitCode = commandRunner.run(List.of("config", "use", selectedServerConfig.getServerId()));
            if (exitCode != 0) {
                return resultBuilder.failedWithError().build();
            }

            // Running JFrog CLI command
            String cliCommand = confMap.get(JF_TASK_COMMAND);
            // We remove 'jf' because the executable already exists on the command runner object.
            cliCommand = StringUtils.removeStart(cliCommand, "jf ");

            List<String> unwrappedArgs = Utils.splitStringPreservingQuotes(cliCommand)
                    .stream()
                    .map(Utils::unQuote)
                    .collect(Collectors.toList());

            exitCode = commandRunner.run(unwrappedArgs);
            if (exitCode != 0) {
                return resultBuilder.failedWithError().build();
            }
        } catch (Exception e) {
            buildLog.error(ExceptionUtils.getRootCauseMessage(e), e);
            return resultBuilder.failedWithError().build();
        }
        return resultBuilder.success().build();
    }

    /**
     * Retrieves the working directory.
     *
     * @param customWd  The custom working directory.
     * @param defaultWd The default working directory.
     * @return The resolved working directory.
     * @throws IOException If the working directory does not exist.
     */
    public File getWorkingDirectory(String customWd, File defaultWd) throws IOException {
        if (StringUtils.isBlank(customWd)) {
            return defaultWd;
        }

        if (!Files.exists(Paths.get(customWd))) {
            throw new IOException("Working directory: '" + customWd + "' does not exist.");
        }

        return new File(customWd);
    }

    /**
     * Creates the JFrog CLI environment variables.
     *
     * @param buildContext The build context.
     * @param serverConfig The selected server config.
     * @return The JFrog CLI environment variables.
     * @throws IOException If an I/O error occurs.
     */
    public Map<String, String> createJfrogEnvironmentVariables(BuildContext buildContext, ServerConfig serverConfig) throws IOException {
        Map<String, String> jfEnvs = new HashMap<>() {
            @Override
            public String put(String key, String value) {
                if (StringUtils.isBlank(System.getProperty(key))) {
                    return super.put(key, value);
                }
                return "";
            }
        };

        for (VariableDefinitionContext varContext : buildContext.getVariableContext().getEffectiveVariables().values()) {
            jfEnvs.put(varContext.getKey(), varContext.getValue());
        }
        jfEnvs.put("JFROG_CLI_SERVER_ID", serverConfig.getServerId());
        jfEnvs.put("JFROG_CLI_BUILD_NAME", buildContext.getPlanName());
        jfEnvs.put("JFROG_CLI_BUILD_NUMBER", String.valueOf(buildContext.getBuildNumber()));

        // Build temporary directory to store JFrog config, will be deleted after the build.
        String fullBuildKey = buildContext.getResultKey().getKey();
        jfEnvs.put("JFROG_CLI_HOME_DIR", BambooUtils.getJfrogTmpSubdir(customVariableContext, fullBuildKey));

        // Agent persistent directory to store the JFrog CLI executable and build-info extractors.
        jfEnvs.put("JFROG_CLI_DEPENDENCIES_DIR", BambooUtils.getJfrogTmpSubdir(customVariableContext, "dependencies"));

        if (StringUtils.isNotBlank(serverConfig.getCliRepository())) {
            // Configured Artifactory repository name from which to download the jar needed by the mvn/gradle command.
            jfEnvs.put("JFROG_CLI_RELEASES_REPO", serverConfig.getServerId() + "/" + serverConfig.getCliRepository());
        }

        String buildUrl = BambooUtils.createBambooBuildUrl(fullBuildKey, administrationConfiguration, administrationConfigurationAccessor);
        jfEnvs.put("JFROG_CLI_BUILD_URL", buildUrl);

        jfEnvs.put("JFROG_CLI_USER_AGENT", BambooUtils.getJFrogPluginIdentifier(pluginAccessor));
        jfEnvs.put("JFROG_CLI_LOG_TIMESTAMP", "OFF");

        buildLog.info("The following JFrog CLI environment variables will be used: " + jfEnvs);
        return jfEnvs;
    }

    /**
     * Configures all JFrog servers using 'jf config add' command.
     *
     * @return The exit code of the command.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the execution is interrupted.
     */
    private int configAllJFrogServers() throws IOException, InterruptedException {
        int exitCode = 0;
        for (ServerConfig serverConfig : serverConfigManager.getAllServerConfigs()) {
            exitCode = runJFrogCliConfigAddCommand(serverConfig);
            if (exitCode != 0) {
                break;
            }
        }
        return exitCode;
    }

    /**
     * Runs the 'jf config add' command to configure the server.
     *
     * @param serverConfig The server configuration.
     * @return The exit code of the command.
     * @throws IOException          If an I/O error occurs.
     * @throws InterruptedException If the execution is interrupted.
     */
    public int runJFrogCliConfigAddCommand(ServerConfig serverConfig) throws IOException, InterruptedException {
        // Run 'jf config add' command to configure the server.
        List<String> configAddArgs = new ArrayList<>(List.of(
                "config",
                "add",
                serverConfig.getServerId(),
                "--url=" + serverConfig.getUrl(),
                "--interactive=false",
                "--overwrite=true"
        ));
        if (StringUtils.isNotBlank(serverConfig.getAccessToken())) {
            configAddArgs.add("--access-token=" + serverConfig.getAccessToken());
        } else if (StringUtils.isNotBlank(serverConfig.getUsername()) && StringUtils.isNotBlank(serverConfig.getPassword())) {
            configAddArgs.add("--user=" + serverConfig.getUsername());
            configAddArgs.add("--password=" + serverConfig.getPassword());
        }
        return commandRunner.run(configAddArgs);
    }

    /**
     * Sets the custom variable context.
     *
     * @param customVariableContext The custom variable context.
     */
    @SuppressWarnings("unused")
    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    /**
     * Sets the plugin accessor.
     *
     * @param pluginAccessor The plugin accessor.
     */
    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }

    /**
     * Sets the administration configuration.
     *
     * @param administrationConfiguration The administration configuration.
     */
    @SuppressWarnings("unused")
    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        this.administrationConfiguration = administrationConfiguration;
    }

    /**
     * Sets the administration configuration accessor.
     *
     * @param administrationConfigurationAccessor The administration configuration accessor.
     */
    @SuppressWarnings("unused")
    public void setAdministrationConfigurationAccessor(
            AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        this.administrationConfigurationAccessor = administrationConfigurationAccessor;
    }
}
