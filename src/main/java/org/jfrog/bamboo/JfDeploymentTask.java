package org.jfrog.bamboo;

import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.config.ServerConfig;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.jfrog.bamboo.utils.BambooUtils;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.bamboo.utils.ExecutableRunner;
import org.jfrog.bamboo.utils.Utils;

import javax.inject.Inject;
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
 * JFrog CLI Task for Bamboo Deployment Projects.
 *
 * <p>Implements {@link DeploymentTaskType} so it appears in the Deployment Project task picker,
 * where the plain {@link JfTask} (which only implements {@link com.atlassian.bamboo.task.TaskType})
 * is invisible.
 *
 * <p>Shares its configurator ({@link JfContext}) and FTL template with {@link JfTask}, so the
 * user-facing configuration UI is identical in both build plans and deployment projects.
 */
public class JfDeploymentTask extends JfContext implements DeploymentTaskType {

    private BuildLog buildLog;

    @Inject
    private ServerConfigManager serverConfigManager;

    @Inject
    @ComponentImport
    private CustomVariableContext customVariableContext;

    @Inject
    @ComponentImport
    private PluginAccessor pluginAccessor;

    @Inject
    @ComponentImport
    private AdministrationConfigurationAccessor administrationConfigurationAccessor;

    /**
     * Executes the JFrog CLI command in a Deployment Project environment.
     */
    @Override
    public @NotNull TaskResult execute(@NotNull DeploymentTaskContext taskContext) {
        buildLog = new BuildLog(taskContext.getBuildLogger());
        TaskResultBuilder resultBuilder = TaskResultBuilder.newBuilder(taskContext);

        String selectedServerId = taskContext.getConfigurationMap().get(JF_TASK_SERVER_ID);
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(selectedServerId);
        if (selectedServerConfig == null) {
            buildLog.error("The selected Server ID doesn't exist: " + selectedServerId);
            return resultBuilder.failedWithError().build();
        }

        try {
            String jfrogTmpDir = BambooUtils.getJfrogTmpDir(customVariableContext);

            // Download CLI (if needed) and retrieve path
            String jfExecutablePath = JfInstaller.getJfExecutable(selectedServerConfig, jfrogTmpDir, buildLog);

            Map<String, String> envs = createDeploymentEnvironmentVariables(taskContext, selectedServerConfig);
            String customWd = taskContext.getConfigurationMap().get(JF_TASK_WORKING_DIRECTORY);
            File workingDir = getWorkingDirectory(customWd, taskContext.getWorkingDirectory());
            buildLog.info("Working directory: " + workingDir);

            List<String> secrets = List.of(selectedServerConfig.getPassword(), selectedServerConfig.getAccessToken());
            ExecutableRunner commandRunner = new ExecutableRunner(jfExecutablePath, workingDir, envs, secrets, buildLog);

            // Run 'jf config add' for every configured JFrog server
            for (ServerConfig serverConfig : serverConfigManager.getAllServerConfigs()) {
                int exitCode = runConfigAdd(commandRunner, serverConfig);
                if (exitCode != 0) {
                    return resultBuilder.failedWithError().build();
                }
            }

            // Make the selected server the default
            int exitCode = commandRunner.run(List.of("config", "use", selectedServerConfig.getServerId()));
            if (exitCode != 0) {
                return resultBuilder.failedWithError().build();
            }

            // Run the user-supplied CLI command
            String cliCommand = taskContext.getConfigurationMap().get(JF_TASK_COMMAND);
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
     * Builds the JFrog CLI environment variables for a deployment-project context.
     *
     * <p>Unlike the build-plan variant, there is no {@link com.atlassian.bamboo.v2.build.BuildContext}
     * available here. Build-specific variables ({@code JFROG_CLI_BUILD_NAME},
     * {@code JFROG_CLI_BUILD_NUMBER}, {@code JFROG_CLI_BUILD_URL}) are therefore omitted; users who
     * need them can pass {@code --build-name} / {@code --build-number} flags in their command.
     */
    private Map<String, String> createDeploymentEnvironmentVariables(
            DeploymentTaskContext taskContext, ServerConfig serverConfig) throws IOException {

        Map<String, String> envs = new HashMap<>();
        envs.put("JFROG_CLI_SERVER_ID", serverConfig.getServerId());

        // Isolate the CLI home dir per deployment run using the unique deployment result ID
        String deploymentKey = String.valueOf(taskContext.getDeploymentContext().getDeploymentResultId());
        envs.put("JFROG_CLI_HOME_DIR", BambooUtils.getJfrogTmpSubdir(customVariableContext, deploymentKey));

        // Shared cache for CLI binary and build-info extractors across all deployment runs on this agent
        envs.put("JFROG_CLI_DEPENDENCIES_DIR", BambooUtils.getJfrogTmpSubdir(customVariableContext, "dependencies"));

        if (StringUtils.isNotBlank(serverConfig.getCliRepository())) {
            envs.put("JFROG_CLI_RELEASES_REPO", serverConfig.getServerId() + "/" + serverConfig.getCliRepository());
        }

        envs.put("JFROG_CLI_USER_AGENT", BambooUtils.getJFrogPluginIdentifier(pluginAccessor));
        envs.put("JFROG_CLI_LOG_TIMESTAMP", "OFF");

        buildLog.info("The following JFrog CLI environment variables will be used: " + envs);
        return envs;
    }

    /**
     * Runs {@code jf config add} to register a server in the CLI's temp home directory.
     */
    private int runConfigAdd(ExecutableRunner commandRunner, ServerConfig serverConfig)
            throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of(
                "config", "add", serverConfig.getServerId(),
                "--url=" + serverConfig.getUrl(),
                "--interactive=false",
                "--overwrite=true"
        ));
        if (StringUtils.isNotBlank(serverConfig.getAccessToken())) {
            args.add("--access-token=" + serverConfig.getAccessToken());
        } else if (StringUtils.isNotBlank(serverConfig.getUsername()) && StringUtils.isNotBlank(serverConfig.getPassword())) {
            args.add("--user=" + serverConfig.getUsername());
            args.add("--password=" + serverConfig.getPassword());
        }
        return commandRunner.run(args);
    }

    /**
     * Resolves the working directory, falling back to the task's default if no custom path is set.
     */
    private File getWorkingDirectory(String customWd, File defaultWd) throws IOException {
        if (StringUtils.isBlank(customWd)) {
            return defaultWd;
        }
        if (!Files.exists(Paths.get(customWd))) {
            throw new IOException("Working directory: '" + customWd + "' does not exist.");
        }
        return new File(customWd);
    }

    // Setters for Spring injection and unit-test overrides

    @SuppressWarnings("unused")
    public void setServerConfigManager(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
    }

    @SuppressWarnings("unused")
    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }

    @SuppressWarnings("unused")
    public void setAdministrationConfigurationAccessor(
            AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        this.administrationConfigurationAccessor = administrationConfigurationAccessor;
    }
}
