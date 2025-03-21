package ut.org.jfrog.bamboo;

import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfrog.bamboo.JfTask;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


public class JfTaskUnitTest {

    private static final Logger log = LogManager.getLogger(JfTaskUnitTest.class);
    protected Path workingDir;

    @Before
    public void setUp() throws IOException {

    }

    @Test
    public void testJfTask() throws IOException, TaskException {
        workingDir = Files.createTempDirectory("bambooIntTest");
        TaskContextMock context = new TaskContextMock(workingDir);
        JfTask jfTask = new JfTask();

        ServerConfigManager serverConfigManager = Mockito.mock(ServerConfigManager.class);
        // ServerConfig serverConfig = new ServerConfig("myserver", "https://example.com", "username", "password", "accessToken","","");
        //when(serverConfigManager.getAllServerConfigs()).thenReturn(List.of(serverConfig));

        CustomVariableContext customVariableContextMock = Mockito.mock(CustomVariableContext.class);
        // Mock the CustomVariableContext
        VariableDefinitionContext variableDefinitionContextMock = Mockito.mock(VariableDefinitionContext.class);
        Map<String, VariableDefinitionContext> variableContextsMap = new HashMap<>();
        variableContextsMap.put("tmp.directory", variableDefinitionContextMock);
        Mockito.when(customVariableContextMock.getVariableContexts()).thenReturn(variableContextsMap);
        Mockito.when(variableDefinitionContextMock.getValue()).thenReturn(workingDir.toAbsolutePath().toString());
        jfTask.setCustomVariableContext(customVariableContextMock);

        PluginAccessor pluginAccessor = Mockito.mock(PluginAccessor.class);
        jfTask.setPluginAccessor(pluginAccessor);
        jfTask.setServerConfigManager(serverConfigManager);
        jfTask.execute(context);
    }

    @After
    public void cleanupWorkingDir() {
        FileUtils.deleteQuietly(workingDir.toFile());
    }
}
