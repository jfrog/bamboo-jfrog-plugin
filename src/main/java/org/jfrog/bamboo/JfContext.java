package org.jfrog.bamboo;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.config.ServerConfig;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.jfrog.bamboo.utils.ExecutableRunner;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuration class for the JFrog Bamboo task.
 */
public class JfContext extends AbstractTaskConfigurator {
    public static final String JF_TASK_SERVER_ID = "jf.task.server.id";
    public static final String JF_TASK_COMMAND = "jf.task.command";
    public static final String JF_TASK_WORKING_DIRECTORY = "jf.task.working.directory";

    @Inject
    private ServerConfigManager serverConfigManager;

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put(JF_TASK_COMMAND, "jf ");
        context.put("serverConfigManager", serverConfigManager);
        context.put("selectedServerId", 1);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        Map<String, String> config = taskDefinition.getConfiguration();
        context.put(JF_TASK_SERVER_ID, config.get(JF_TASK_SERVER_ID));
        context.put(JF_TASK_COMMAND, config.get(JF_TASK_COMMAND));
        context.put(JF_TASK_WORKING_DIRECTORY, config.get(JF_TASK_WORKING_DIRECTORY));
        context.put("serverConfigManager", serverConfigManager);
    }

    @Override
    @NotNull
    public Map<String, String> generateTaskConfigMap(
            @NotNull final ActionParametersMap params,
            @Nullable final TaskDefinition previousTaskDefinition
    ) {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        config.put(JF_TASK_SERVER_ID, params.getString(JF_TASK_SERVER_ID));
        config.put(JF_TASK_COMMAND, params.getString(JF_TASK_COMMAND));
        config.put(JF_TASK_WORKING_DIRECTORY, params.getString(JF_TASK_WORKING_DIRECTORY));
        return config;
    }

    @Override
    public void validate(@NotNull ActionParametersMap params, @NotNull ErrorCollection errorCollection) {
        super.validate(params, errorCollection);
        String cliCommand = params.getString(JF_TASK_COMMAND);
        if (!StringUtils.startsWith(StringUtils.trim(cliCommand), "jf ")) {
            errorCollection.addErrorMessage("JFrog CLI command should start with 'jf '.");
        }
        if (StringUtils.isBlank(params.getString(JF_TASK_SERVER_ID))) {
            errorCollection.addErrorMessage("JFrog configuration should be selected");
        }
    }

    /**
     * Resolves the task working directory: returns the user-supplied custom path if non-blank
     * (failing if it does not exist), otherwise the task's default working directory.
     *
     * <p>Lives on the shared configurator so deployment-project subclasses such as
     * {@link JfDeploymentTask} can reuse it without re-implementing the same logic.
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
     * Runs {@code jf config add} to register a single JFrog server in the CLI's temp home dir.
     *
     * <p>Lives on the shared configurator so deployment-project subclasses such as
     * {@link JfDeploymentTask} can reuse it without re-implementing the same CLI invocation.
     */
    public int runConfigAdd(ExecutableRunner commandRunner, ServerConfig serverConfig)
            throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(List.of(
                "config",
                "add",
                serverConfig.getServerId(),
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
}
