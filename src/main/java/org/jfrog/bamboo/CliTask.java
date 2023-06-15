package org.jfrog.bamboo;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.process.EnvironmentVariableAccessor;
import com.atlassian.bamboo.task.*;

import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.agent.capability.CapabilityContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.admin.ServerConfig;
import org.jfrog.bamboo.admin.ServerConfigManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

import static java.lang.String.format;

public class CliTask extends AbstractTaskConfigurator implements TaskType  {

    protected static final Logger log = LogManager.getLogger(CliTask.class);
    protected transient ServerConfigManager serverConfigManager;
    private String jfExecutablePath = "";
    private CustomVariableContext customVariableContext;
    private MyLog myLog;

    protected CliTask () {
        serverConfigManager = ServerConfigManager.getInstance();
    }

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", 1);
    }

    @Override
    @NotNull
    public Map<String, String> generateTaskConfigMap(
            @NotNull final ActionParametersMap params,
            @Nullable final TaskDefinition previousTaskDefinition
    ) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put("jfrog.task.cli.serverid", params.getString("jfrog.task.cli.serverid"));
        config.put("jfrog.task.cli.command", params.getString("jfrog.task.cli.command"));
        return config;
    }

    @Override
    public @NotNull TaskResult execute(final TaskContext taskContext) throws TaskException {
        myLog = new MyLog(log, taskContext.getBuildLogger());
        try{
            // Download CLI (if needed) and retrieve path
            this.jfExecutablePath = JFrogCliInstaller.getJfExecutable("", customVariableContext, myLog);

            BuildContext buildContext = taskContext.getBuildContext();
            myLog.info("my build: "+ buildContext);
            Map<String, String> environmentVariables = new HashMap<>();
            environmentVariables.put("JFROG_CLI_SERVER_ID", "3333");
            environmentVariables.put("JFROG_CLI_BUILD_NAME", buildContext.getPlanName());
            environmentVariables.put("JFROG_CLI_BUILD_NUMBER", String.valueOf(buildContext.getBuildNumber()));
            environmentVariables.put("JFROG_CLI_BUILD_URL", "");
            environmentVariables.put("JFROG_CLI_HOME_DIR", "");

            myLog.info("Get config map");
            // Creating JFrog server config
            ConfigurationMap confMap =  taskContext.getConfigurationMap();
            String serverId = confMap.get("jfrog.task.cli.serverid");
            myLog.info("Get server config");
            ServerConfig serverConfig = getArtifactoryServerConfig(Integer.parseInt(serverId));
            myLog.info(format("Using ServerID: %s (%s)", serverId, serverConfig.getUrl()));
            runCliCommand(environmentVariables,
                    "config",
                    "add",
                    serverId,
                    "--url=" + serverConfig.getUrl(),
                    "--username=" + serverConfig.getUsername(),
                    "--password=" + serverConfig.getPassword(),
                    "--interactive=false",
                    "--overwrite=true"
            );



            // Running JFrog CLI command
            String cliCommand =  confMap.get("jfrog.task.cli.command");
            String[] cliCommandArgs = cliCommand.split(" ");
            runCliCommand(environmentVariables, cliCommandArgs);

        } catch (Exception e){
            myLog.error("Error!!!: " + e  +"\n"+ ExceptionUtils.getStackTrace(e));
            return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
        }
        myLog.info("Success!!!!");
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private ServerConfig getArtifactoryServerConfig(int serverId) {
        ServerConfig selectedServerConfig = serverConfigManager.getServerConfigById(serverId);
        if (selectedServerConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server. Please check the Artifactory server in the task configuration.");
        }
        return selectedServerConfig;
    }

    private void runCliCommand(Map<String, String> jfrogEnvs, String... command) {
        List<String> commandList = new ArrayList<>();
        commandList.add(this.jfExecutablePath);
        commandList.addAll(Arrays.asList(command));

        try {
            myLog.info("Running command: " + String.join(" ", commandList));

            // Create the process builder
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);

            // Set the additional environment variables from the jfrogEnvs map
            processBuilder.environment().putAll(jfrogEnvs);

            // Redirect the process output to the console
            processBuilder.redirectErrorStream(true);

            // Start the process
            Process process = processBuilder.start();

            // Read the output of the process
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                myLog.info(line);
            }

            // Wait for the process to complete
            int exitCode = process.waitFor();
            myLog.info("Process executed with exit code: " + exitCode);
        }
        catch (Exception e){
            myLog.info("Failed running cli command:" + e);
        }
    }

    // setCustomVariableContext triggered by bamboo internal code with the task's Context
    public void setCustomVariableContext(CustomVariableContext customVariableContext) {
        this.customVariableContext = customVariableContext;
    }

}