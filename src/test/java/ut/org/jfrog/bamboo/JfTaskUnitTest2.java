package ut.org.jfrog.bamboo;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import org.apache.logging.log4j.Logger;
import org.jfrog.bamboo.JfTask;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.jfrog.bamboo.utils.ExecutableRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JfTaskUnitTest2 {

    @Mock
    private TaskContext taskContext;

    @Mock
    private ConfigurationMap configurationMap;

    @Mock
    private BuildLogger buildLogger;
    @Mock
    private Logger log;

    @Mock
    private BuildLogger buildLog;

    @Mock
    private ServerConfigManager IServerConfigManager;

    @Mock
    private ExecutableRunner commandRunner;

    private JfTask jfTask;

    @Before
    public void setUp() {
        when(taskContext.getConfigurationMap()).thenReturn(configurationMap);
        when(taskContext.getBuildLogger()).thenReturn(buildLogger);
        jfTask = new JfTask();
        jfTask.setCustomVariableContext(null); // Set customVariableContext if needed
        jfTask.setPluginAccessor(null); // Set pluginAccessor if needed
        jfTask.setAdministrationConfigurationAccessor(null); // Set administrationConfigurationAccessor if needed
    }

    @Test
    public void testExecute_Success() throws Exception {
        // Mock necessary objects and configurations
        // ServerConfig serverConfig = new ServerConfig("server1", "https://example.com", "username", "password", "accessToken", "", "");
        // when(serverConfigManager.getAllServerConfigs()).thenReturn(List.of(serverConfig));
        when(configurationMap.get(JfTask.JF_TASK_SERVER_ID)).thenReturn("server1");
        when(configurationMap.get(JfTask.JF_TASK_WORKING_DIRECTORY)).thenReturn("/path/to/working/dir");
        when(configurationMap.get(JfTask.JF_TASK_COMMAND)).thenReturn("jf command");

        // Mock JfInstaller.getJfExecutable()
        String jfExecutablePath = "/path/to/jf/executable";
        //when(JfInstaller.getJfExecutable("", new BuildLog(log, buildLog))).thenReturn(jfExecutablePath);


        // Mock ExecutableRunner
        File workingDir = new File("/path/to/working/dir");
        Map<String, String> envs = new HashMap<>();
        //when(jfTask.createJfrogEnvironmentVariables(any(), anyString())).thenReturn(envs);
        when(jfTask.getWorkingDirectory(anyString(), any())).thenReturn(workingDir);
        when(jfTask.runJFrogCliConfigAddCommand(any())).thenReturn(0);
        when(commandRunner.run(anyList())).thenReturn(0);

        // Execute the task
        TaskResult taskResult = jfTask.execute(taskContext);

        // Verify task result and interactions with dependencies
        assertEquals(TaskResultBuilder.newBuilder(taskContext).success().build(), taskResult);
        //verify(buildLog).info("The following environment variables will be used: " + envs);
        verify(IServerConfigManager).getAllServerConfigs();
        //      verify(jfTask).createJfrogEnvironmentVariables(any(), eq("server1"));
        verify(jfTask).getWorkingDirectory(eq("/path/to/working/dir"), any());
        //  verify(jfTask).runJFrogCliConfigAddCommand(serverConfig);
        verify(commandRunner).run(List.of("config", "use", "server1"));
        verify(commandRunner).run(List.of("command"));
    }
}
