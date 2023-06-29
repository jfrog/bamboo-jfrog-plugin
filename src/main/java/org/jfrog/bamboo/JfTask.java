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
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.config.ServerConfig;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.jfrog.bamboo.utils.BambooUtils;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.bamboo.utils.ExecutableRunner;

import java.io.IOException;
import java.util.*;

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
    public @NotNull TaskResult execute(final @NotNull TaskContext taskContext) {
        buildLog = new BuildLog(log, taskContext.getBuildLogger());
        serverConfigManager = ServerConfigManager.getInstance();
        ConfigurationMap confMap = taskContext.getConfigurationMap();

        try {
            // Download CLI (if needed) and retrieve path
            String jfExecutablePath = JfInstaller.getJfExecutable("", buildLog);

            // Create commandRunner to run JFrog CLI commands
            String serverId = confMap.get(JF_TASK_SERVER_ID);
            Map<String, String> envs = createJfrogEnvironmentVariables(taskContext.getBuildContext(), serverId);
            commandRunner = new ExecutableRunner(jfExecutablePath, taskContext.getWorkingDirectory(), envs, buildLog);

            // Run 'jf config add' and 'jf config use' commands.
            configAllJFrogServers();

            // Make selected Server ID as default (by 'jf c use')
            commandRunner.run(List.of("config", "use", serverId));

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
        Map<String, String> jfEnvs = new HashMap<>() {
            public String put(String key, String value) {
                if (StringUtils.isBlank(System.getProperty(key))) {
                    return super.put(key, value);
                }
                return "";
            }
        };

        jfEnvs.put("JFROG_CLI_SERVER_ID", serverId);
        jfEnvs.put("JFROG_CLI_BUILD_NAME", buildContext.getPlanName());
        jfEnvs.put("JFROG_CLI_BUILD_NUMBER", String.valueOf(buildContext.getBuildNumber()));

        String fullBuildKey = buildContext.getResultKey().getKey();
        jfEnvs.put("JFROG_CLI_HOME_DIR", BambooUtils.getJfrogSpecificBuildTmp(customVariableContext, fullBuildKey));

        String buildUrl = BambooUtils.createBambooBuildUrl(fullBuildKey, administrationConfiguration, administrationConfigurationAccessor);
        jfEnvs.put("JFROG_CLI_BUILD_URL", buildUrl);

        jfEnvs.put("JFROG_CLI_USER_AGENT", BambooUtils.getJFrogPluginIdentifier(pluginAccessor));
        jfEnvs.put("JFROG_CLI_LOG_TIMESTAMP", "OFF");

        buildLog.info("The following environment variables will be used: " + jfEnvs);
        return jfEnvs;
    }

    private void configAllJFrogServers() throws IOException, InterruptedException {
        for (ServerConfig serverConfig : serverConfigManager.getAllServerConfigs()) {
            runJFrogCliConfigAddCommand(serverConfig);
        }
    }

    private void runJFrogCliConfigAddCommand(ServerConfig serverConfig) throws IOException, InterruptedException {
        // Run 'jf config add' command to configure the server.
        List<String> configAddArgs = new ArrayList<>(List.of(
                "config",
                "add",
                serverConfig.getServerId(),
                "--url=" + serverConfig.getUrl(),
                "--interactive=false",
                "--overwrite=true"
        ));
        if (StringUtils.isNotEmpty(serverConfig.getAccessToken())) {
            configAddArgs.add("--access-token=" + serverConfig.getAccessToken());
        } else if (StringUtils.isNotEmpty(serverConfig.getUsername()) && StringUtils.isNotEmpty(serverConfig.getPassword())) {
            configAddArgs.add("--user=" + serverConfig.getUsername());
            configAddArgs.add("--password=" + serverConfig.getPassword());
        }
        commandRunner.run(configAddArgs);
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