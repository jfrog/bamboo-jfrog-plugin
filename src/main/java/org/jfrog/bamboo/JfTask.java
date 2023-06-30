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
import com.atlassian.crowd.exception.DirectoryNotFoundException;
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        TaskResultBuilder resultBuilder = TaskResultBuilder.newBuilder(taskContext);
        try {
            // Download CLI (if needed) and retrieve path
            String jfExecutablePath = JfInstaller.getJfExecutable("", buildLog);

            // Create commandRunner to run JFrog CLI commands
            String serverId = confMap.get(JF_TASK_SERVER_ID);
            Map<String, String> envs = createJfrogEnvironmentVariables(taskContext.getBuildContext(), serverId);
            File workingDir = getWorkingDirectory(confMap.get(JF_TASK_WORKING_DIRECTORY), taskContext.getWorkingDirectory());
            commandRunner = new ExecutableRunner(jfExecutablePath, workingDir, envs, buildLog);

            // Run 'jf config add' and 'jf config use' commands.
            int exitCode = configAllJFrogServers();
            if (exitCode != 0) {
                resultBuilder.failedWithError().build();
            }

            // Make selected Server ID as default (by 'jf c use')
            exitCode = commandRunner.run(List.of("config", "use", serverId));
            if (exitCode != 0) {
                resultBuilder.failedWithError().build();
            }
            // Running JFrog CLI command
            String cliCommand = confMap.get(JF_TASK_COMMAND);
            String[] splitArgs = cliCommand.trim().split(" ");
            List<String> cliCommandArgs = new ArrayList<>(Arrays.asList(splitArgs));
            // Received command format is 'jf <arg1> <<arg2> ...'
            // We remove 'jf' because the executable already exists on the command runner object.
            if (cliCommandArgs.get(0).equals("jf")) {
                cliCommandArgs.remove(0);
            }
            exitCode = commandRunner.run(cliCommandArgs);
            if (exitCode != 0) {
                resultBuilder.failedWithError().build();
            }
        } catch (DirectoryNotFoundException e) {
            buildLog.error(e.getMessage());
            return resultBuilder.failedWithError().build();
        } catch (IOException | InterruptedException e) {
            buildLog.error(e + "\n" + ExceptionUtils.getStackTrace(e));
            return resultBuilder.failedWithError().build();
        }
        return resultBuilder.success().build();
    }

    private File getWorkingDirectory(String customWd, File defaultWd) throws DirectoryNotFoundException {
        if (StringUtils.isBlank(customWd)) {
            return defaultWd;
        }

        if (!Files.exists(Paths.get(customWd))) {
            throw new DirectoryNotFoundException("Working directory: '" + customWd + "' does not exist.");
        }

        return new File(customWd);
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

    private int runJFrogCliConfigAddCommand(ServerConfig serverConfig) throws IOException, InterruptedException {
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