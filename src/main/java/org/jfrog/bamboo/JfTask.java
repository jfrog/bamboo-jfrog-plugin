package org.jfrog.bamboo;


import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.*;

import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.types.Commandline;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.config.ServerConfig;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.jfrog.bamboo.utils.BambooUtils;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.bamboo.utils.ExecutableRunner;

import java.io.*;
import java.util.*;

import static java.lang.String.format;

public class JfTask extends JfContext implements TaskType  {
    protected static final Logger log = LogManager.getLogger(JfTask.class);
    protected transient ServerConfigManager serverConfigManager;
    private BuildLog buildLog;
    private ExecutableRunner commandRunner;
    protected CustomVariableContext customVariableContext;
    protected PluginAccessor pluginAccessor;

    @Override
    public @NotNull TaskResult execute(final @NotNull TaskContext taskContext) throws TaskException {
        buildLog = new BuildLog(log, taskContext.getBuildLogger());
        serverConfigManager = ServerConfigManager.getInstance();
        ConfigurationMap confMap = taskContext.getConfigurationMap();
        String serverId = confMap.get(JF_TASK_SERVER_ID);
        try {
            // Download CLI (if needed) and retrieve path
            String jfExecutablePath = JfInstaller.getJfExecutable(
                    "",
                    buildLog);

            // Create commandRunner to run JFrog CLI commands
            commandRunner = new ExecutableRunner(
                    jfExecutablePath,
                    taskContext.getWorkingDirectory(),
                    createJfrogEnvironmentVariables(taskContext.getBuildContext(), serverId),
                    buildLog
            );

            buildLog.info("myconf: " + confMap);
            // Run 'jf config add' and 'jf config use' commands.
            runJFrogConfigAddCommand(serverId);

            // Running JFrog CLI command
            String cliCommand = confMap.get(JF_TASK_COMMAND);
            String[] splitArgs =cliCommand.trim().split(" ");
            List<String> cliCommandArgs = new ArrayList<>(Arrays.asList(splitArgs));
            // Received command format is 'jf <arg1> <<arg2> ...'
            // We remove 'jf' because the executable already exists on the command runner object.
            if (cliCommandArgs.get(0).equalsIgnoreCase("jf")) {
                cliCommandArgs.remove(0);
            }
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
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(serverId);
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