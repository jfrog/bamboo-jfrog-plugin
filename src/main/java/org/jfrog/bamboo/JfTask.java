package org.jfrog.bamboo;


import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.configuration.AdministrationConfigurationAccessor;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.agent.capability.AgentContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.plugin.PluginAccessor;
import org.jfrog.bamboo.config.ServerConfig;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.jfrog.bamboo.utils.BambooUtils;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.bamboo.utils.ExecutableRunner;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;

import static java.lang.String.format;

public class JfTask extends JfContext implements TaskType {
    protected static final Logger log = LogManager.getLogger(JfTask.class);
    protected transient ServerConfigManager serverConfigManager;
    private BuildLog buildLog;
    private ExecutableRunner commandRunner;
    protected CustomVariableContext customVariableContext;
    protected PluginAccessor pluginAccessor;
    protected AdministrationConfiguration administrationConfiguration;
    protected AdministrationConfigurationAccessor administrationConfigurationAccessor;

    @Override
    public @NotNull TaskResult execute(final @NotNull TaskContext taskContext) throws TaskException {
        buildLog = new BuildLog(log, taskContext.getBuildLogger());
        serverConfigManager = ServerConfigManager.getInstance();
        ConfigurationMap confMap = taskContext.getConfigurationMap();
        String serverId = confMap.get(JF_TASK_SERVER_ID);

        try {
            // Download CLI (if needed) and retrieve path
            String jfExecutablePath = JfInstaller.getJfExecutable("", buildLog);

            // Create commandRunner to run JFrog CLI commands
            commandRunner = new ExecutableRunner(jfExecutablePath, taskContext.getWorkingDirectory(), createJfrogEnvironmentVariables(taskContext.getBuildContext(), serverId), buildLog);

            // Run 'jf config add' and 'jf config use' commands.
            runJFrogCliConfigAddCommand(serverId);
            // Running JFrog CLI command
            String cliCommand = confMap.get(JF_TASK_COMMAND);
            String[] splitArgs = cliCommand.trim().split(" ");
            List<String> cliCommandArgs = new ArrayList<>(Arrays.asList(splitArgs));
            // Received command format is 'jf <arg1> <<arg2> ...'
            // We remove 'jf' because the executable already exists on the command runner object.
            if (cliCommandArgs.get(0).equalsIgnoreCase("jf")) {
                cliCommandArgs.remove(0);
            }
            commandRunner.run(cliCommandArgs);

        } catch (IOException | InterruptedException e) {
            buildLog.error(e + "\n" + ExceptionUtils.getStackTrace(e));
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private Map<String, String> createJfrogEnvironmentVariables(BuildContext buildContext, String serverId) throws IOException {
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("JFROG_CLI_SERVER_ID", serverId);
        environmentVariables.put("JFROG_CLI_BUILD_NAME", buildContext.getPlanName());
        environmentVariables.put("JFROG_CLI_BUILD_NUMBER", String.valueOf(buildContext.getBuildNumber()));

        String fullBuildKey = buildContext.getResultKey().getKey();
        environmentVariables.put("JFROG_CLI_HOME_DIR", BambooUtils.getJfrogSpecificBuildTmp(customVariableContext, fullBuildKey));

        String buildUrl = BambooUtils.createBambooBuildUrl(fullBuildKey, administrationConfiguration, administrationConfigurationAccessor);
        environmentVariables.put("JFROG_CLI_BUILD_URL", buildUrl);

        environmentVariables.put("JFROG_CLI_USER_AGENT", BambooUtils.getJFrogPluginIdentifier(pluginAccessor));
        environmentVariables.put("JFROG_CLI_LOG_TIMESTAMP", "OFF");

        // todo: remove this:
        environmentVariables.put("JFROG_CLI_LOG_LEVEL", "DEBUG");

        buildLog.info("The following environment variables will be used: " + environmentVariables);
        return environmentVariables;
    }

    private void runJFrogCliConfigAddCommand(String serverId) throws IOException, InterruptedException {
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(serverId);
        if (selectedServerConfig == null) {
            throw new IllegalArgumentException("Could not find JFrog server. Please check the JFrog server in the task configuration.");
        }
        buildLog.info(format("Using ServerID: %s (%s)", serverId, selectedServerConfig.getUrl()));

        // Run 'jf config add' command to configure the server.
        List<String> configAddArgs = new ArrayList<>(List.of(
                "config",
                "add",
                serverId,
                "--url=" + selectedServerConfig.getUrl(),
                "--interactive=false",
                "--overwrite=true"
        ));
        if (StringUtils.isNotEmpty(selectedServerConfig.getAccessToken())) {
            configAddArgs.add("--access-token=" + selectedServerConfig.getAccessToken());
        } else if (StringUtils.isNotEmpty(selectedServerConfig.getUsername()) && StringUtils.isNotEmpty(selectedServerConfig.getPassword())) {
            configAddArgs.add("--user=" + selectedServerConfig.getUsername());
            configAddArgs.add("--password=" + selectedServerConfig.getPassword());
        }
        commandRunner.run(configAddArgs);

        // Run 'jf config use' command to make the previously configured server as default.
        List<String> configUseArgs = List.of("config", "use", serverId);
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

    @SuppressWarnings("unused")
    public void setAdministrationConfiguration(AdministrationConfiguration administrationConfiguration) {
        this.administrationConfiguration = administrationConfiguration;
    }

    @SuppressWarnings("unused")
    public void setAdministrationConfigurationAccessor(
            AdministrationConfigurationAccessor administrationConfigurationAccessor) {
        this.administrationConfigurationAccessor = administrationConfigurationAccessor;
    }
}