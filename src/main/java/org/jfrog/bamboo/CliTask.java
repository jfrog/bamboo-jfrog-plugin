package org.jfrog.bamboo;


import com.atlassian.bamboo.buildqueue.manager.AgentManager;
import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.*;

import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.plugin.PluginAccessor;
import com.google.inject.Inject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;
import org.jfrog.bamboo.utils.BambooUtils;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.bamboo.utils.ExecutableRunner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.String.format;

public class CliTask extends AbstractTaskConfigurator implements TaskType  {
    protected static final Logger log = LogManager.getLogger(CliTask.class);
    protected transient ServerConfigManager serverConfigManager;
    private BuildLog buildLog;
    private final String JFROG_TASK_CLI_SERVER_ID = "jfrog.task.cli.serverid";
    private final String JFROG_TASK_CLI_COMMAND = "jfrog.task.cli.command";
    private ExecutableRunner commandRunner;
    protected CustomVariableContext customVariableContext;
    protected PluginAccessor pluginAccessor;

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        serverConfigManager = ServerConfigManager.getInstance();
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", 1);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        serverConfigManager = ServerConfigManager.getInstance();
        context.put(JFROG_TASK_CLI_SERVER_ID, taskDefinition.getConfiguration().get(JFROG_TASK_CLI_SERVER_ID));
        context.put(JFROG_TASK_CLI_COMMAND, taskDefinition.getConfiguration().get(JFROG_TASK_CLI_COMMAND));
        context.put("serverConfigManager", serverConfigManager);
    }
    @Override
    @NotNull
    public Map<String, String> generateTaskConfigMap(
            @NotNull final ActionParametersMap params,
            @Nullable final TaskDefinition previousTaskDefinition
    ) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put(JFROG_TASK_CLI_SERVER_ID, params.getString(JFROG_TASK_CLI_SERVER_ID));
        config.put(JFROG_TASK_CLI_COMMAND, params.getString(JFROG_TASK_CLI_COMMAND));
        return config;
    }

    private void initTask(final TaskContext taskContext) {
        this.buildLog = new BuildLog(log, taskContext.getBuildLogger());
        this.serverConfigManager = ServerConfigManager.getInstance();

    }

    @Override
    public @NotNull TaskResult execute(final @NotNull TaskContext taskContext) throws TaskException {
        initTask(taskContext);
        ConfigurationMap confMap = taskContext.getConfigurationMap();
        String serverId = confMap.get(JFROG_TASK_CLI_SERVER_ID);
        try {
            // Download CLI (if needed) and retrieve path
            String jfExecutablePath = JFrogCliInstaller.getJfExecutable(
                    "",
                    buildLog);

            // Create commandRunner to run JFrog CLI commands
            commandRunner = new ExecutableRunner(
                    jfExecutablePath,
                    taskContext.getWorkingDirectory(),
                    createJfrogEnvironmentVariables(taskContext.getBuildContext(), serverId),
                    buildLog
            );

            // Run 'jf config add' and 'jf config use' commands.
            runJFrogConfigAddCommand(serverId);

            // Running JFrog CLI command
            String cliCommand = confMap.get(JFROG_TASK_CLI_COMMAND);
            List<String> cliCommandArgs = Arrays.asList(cliCommand.split(" "));
            commandRunner.run(cliCommandArgs);

        } catch (IOException | InterruptedException e) {
            buildLog.error("Error!!!: " + e + "\n" + ExceptionUtils.getStackTrace(e));
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private Map<String, String> createJfrogEnvironmentVariables(BuildContext buildContext, String serverId) throws IOException {
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("JFROG_CLI_SERVER_ID", serverId);
        environmentVariables.put("JFROG_CLI_BUILD_NAME", buildContext.getPlanName());
        environmentVariables.put("JFROG_CLI_BUILD_NUMBER", String.valueOf(buildContext.getBuildNumber()));
        environmentVariables.put("JFROG_CLI_BUILD_URL", "");

        environmentVariables.put("JFROG_CLI_USER_AGENT", BambooUtils.getJFrogPluginIdentifier(pluginAccessor));
        environmentVariables.put("JFROG_CLI_LOG_TIMESTAMP", "OFF");
        environmentVariables.put("JFROG_CLI_HOME_DIR", BambooUtils.getJfrogSpecificBuildTmp(customVariableContext, buildContext));

        environmentVariables.put("JFROG_CLI_LOG_LEVEL", "DEBUG");
        return environmentVariables;
    }

    private void runJFrogConfigAddCommand(String serverId) throws IOException, InterruptedException {
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(Long.parseLong(serverId));
        if (selectedServerConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        buildLog.info(format("Using ServerID: %s (%s)", serverId, selectedServerConfig.getUrl()));

        // Run 'jf config add' command to configure the server.
        List<String> configAddArgs = List.of(
                "config",
                "add",
                serverId,
                "--url=" + selectedServerConfig.getUrl(),
                "--user=" + selectedServerConfig.getUsername(),
                "--password=" + selectedServerConfig.getPassword(),
                "--interactive=false",
                "--overwrite=true"
        );
        commandRunner.run(configAddArgs);

        // Run 'jf config use' command to make the previously configured server as default.
        List<String> configUseArgs = List.of(
                "config",
                "use",
                serverId
        );
        commandRunner.run(configUseArgs);
    }

    @SuppressWarnings("unused")
    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

    @SuppressWarnings("unused")
    public void setPluginAccessor(PluginAccessor pluginAccessor) {
        this.pluginAccessor = pluginAccessor;
    }
}