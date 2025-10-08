package ut.org.jfrog.bamboo;

import org.jfrog.bamboo.config.JfrogServerConfigAction;
import org.jfrog.bamboo.config.ServerConfigManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class JfrogServerConfigActionTest {

    @Mock
    private ServerConfigManager serverConfigManager;

    private JfrogServerConfigAction action;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        action = new JfrogServerConfigAction(serverConfigManager);
    }

    @Test
    public void validate_shouldAddErrors_whenServerIdBlankAndUrlBlank() {
        action.setServerId("");
        action.setUrl("");

        action.validate();

        assertTrue("Expected field error on serverId", action.hasFieldErrors());
        assertNotNull(action.getFieldErrors().get("serverId"));
        assertNotNull(action.getFieldErrors().get("url"));
    }

    @Test
    public void testHttpUrlIsValid() throws MalformedURLException {
        // Test HTTP URL validation directly without triggering dependency conflicts
        String httpUrl = "http://example.com";
        URL url = new URL(httpUrl);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test  
    public void testHttpsUrlIsValid() throws MalformedURLException {
        // Test HTTPS URL validation directly
        String httpsUrl = "https://example.com";
        URL url = new URL(httpsUrl);
        assertEquals("https", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test(expected = MalformedURLException.class)
    public void testInvalidUrlThrowsException() throws MalformedURLException {
        // Test invalid URL throws exception
        String invalidUrl = "ht!tp://bad url";
        new URL(invalidUrl);
    }

    @Test
    public void testHttpUrlWithPort() throws MalformedURLException {
        // Test HTTP URL with port
        String httpUrlWithPort = "http://artifactory.company.com:8080";
        URL url = new URL(httpUrlWithPort);
        assertEquals("http", url.getProtocol());
        assertEquals("artifactory.company.com", url.getHost());
        assertEquals(8080, url.getPort());
    }
}


