package org.jfrog.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;

import org.jetbrains.annotations.NotNull;

public class CliTask implements TaskType {
    @Override
    public @NotNull TaskResult execute(final TaskContext taskContext) throws TaskException {
        final BuildLogger buildLogger = taskContext.getBuildLogger();

        buildLogger.addBuildLogEntry("Hello, World!");
        try{
        JFrogCliInstaller.downloadCLI("");
        } catch (Exception e){
            buildLogger.addBuildLogEntry("Error!!!: " + e);
        }
        buildLogger.addBuildLogEntry("Success!!!!");
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }
}