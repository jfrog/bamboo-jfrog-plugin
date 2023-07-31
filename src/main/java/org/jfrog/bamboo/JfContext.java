package org.jfrog.bamboo;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.config.ServerConfigManager;

import java.util.Map;

/**
 * Configuration class for the JFrog Bamboo task.
 */
public class JfContext extends AbstractTaskConfigurator {
    public static final String JF_TASK_SERVER_ID = "jf.task.server.id";
    public static final String JF_TASK_COMMAND = "jf.task.command";
    public static final String JF_TASK_WORKING_DIRECTORY = "jf.task.working.directory";

    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context) {
        super.populateContextForCreate(context);
        context.put(JF_TASK_COMMAND, "jf ");
        context.put("serverConfigManager", ServerConfigManager.getInstance());
        context.put("selectedServerId", 1);
    }

    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition) {
        super.populateContextForEdit(context, taskDefinition);
        Map<String, String> config = taskDefinition.getConfiguration();
        context.put(JF_TASK_SERVER_ID, config.get(JF_TASK_SERVER_ID));
        context.put(JF_TASK_COMMAND, config.get(JF_TASK_COMMAND));
        context.put(JF_TASK_WORKING_DIRECTORY, config.get(JF_TASK_WORKING_DIRECTORY));
        context.put("serverConfigManager", ServerConfigManager.getInstance());
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
            errorCollection.addErrorMessage("JFrog CLI command should start with 'jf '");
        }
    }
}
