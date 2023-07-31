package ut.org.jfrog.bamboo;

import com.atlassian.bamboo.ResultKey;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.serialization.WhitelistedSerializable;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.v2.build.BuildContext;
import com.atlassian.bamboo.v2.build.CommonContext;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jfrog.bamboo.JfTask;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TaskContextMock implements TaskContext {

    private BuildContext buildContextMock;
    private BuildLogger buildLoggerMock;
    private File rootDirectoryMock;
    private File workingDirectoryMock;
    private ConfigurationMap configurationMapMock;
    private CustomVariableContext customVariableContextMock;

    public TaskContextMock(Path workingDir) {
        buildContextMock = Mockito.mock(BuildContext.class);
        buildLoggerMock = Mockito.mock(BuildLogger.class);
        rootDirectoryMock = Mockito.mock(File.class);
        workingDirectoryMock = workingDir.toFile();
        configurationMapMock = Mockito.mock(ConfigurationMap.class);

        Mockito.when(buildContextMock.getPlanName()).thenReturn("MyPlan");
        Mockito.when(buildContextMock.getBuildNumber()).thenReturn(123);


        // Mock the ResultKey
        ResultKey resultKeyMock = Mockito.mock(ResultKey.class);
        Mockito.when(resultKeyMock.getKey()).thenReturn("MY-PLN-123");
        Mockito.when(buildContextMock.getResultKey()).thenReturn(resultKeyMock);

        Mockito.when(configurationMapMock.get(JfTask.JF_TASK_COMMAND)).thenReturn("jf rt ping");
        Mockito.when(configurationMapMock.get(JfTask.JF_TASK_SERVER_ID)).thenReturn("myserver");
    }

    @NotNull
    @Override
    public BuildContext getBuildContext() {
        return buildContextMock;
    }

    @NotNull
    @Override
    public BuildLogger getBuildLogger() {
        return buildLoggerMock;
    }

    @NotNull
    @Override
    public File getRootDirectory() {
        return rootDirectoryMock;
    }

    @NotNull
    @Override
    public File getWorkingDirectory() {
        return workingDirectoryMock;
    }

    @NotNull
    @Override
    public ConfigurationMap getConfigurationMap() {
        return configurationMapMock;
    }

    @Nullable
    @Override
    public Map<String, String> getRuntimeTaskContext() {
        Map<String, String> runtimeTaskContext = new HashMap<>();
        runtimeTaskContext.put("key1", "value1");
        runtimeTaskContext.put("key2", "value2");
        return runtimeTaskContext;
    }

    @NotNull
    @Override
    public CommonContext getCommonContext() {
        return null;
    }

    @Nullable
    @Override
    public Map<String, WhitelistedSerializable> getRuntimeTaskData() {
        return null;
    }

    @Override
    public boolean doesTaskProduceTestResults() {
        return false;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public @NotNull String getPluginKey() {
        return "plugin-key";
    }

    @Override
    public @Nullable String getUserDescription() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public boolean isFinalising() {
        return false;
    }
}
