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
        String httpUrl = "http://example.com";
        URL url = new URL(httpUrl);
        assertEquals("http", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test  
    public void testHttpsUrlIsValid() throws MalformedURLException {
        String httpsUrl = "https://example.com";
        URL url = new URL(httpsUrl);
        assertEquals("https", url.getProtocol());
        assertEquals("example.com", url.getHost());
    }

    @Test
    public void testHttpUrlWithPort() throws MalformedURLException {
        String httpUrlWithPort = "http://artifactory.company.com:8080";
        URL url = new URL(httpUrlWithPort);
        assertEquals("http", url.getProtocol());
        assertEquals("artifactory.company.com", url.getHost());
        assertEquals(8080, url.getPort());
    }

    @Test
    public void validate_shouldAddError_whenUrlIsMalformed() {
        action.setServerId("testServer");
        action.setUrl("invalid-url-format");

        action.validate();

        assertTrue("Expected field error on url", action.hasFieldErrors());
        assertNotNull("Expected url field error", action.getFieldErrors().get("url"));
        assertTrue("Expected MalformedURL error message", 
            action.getFieldErrors().get("url").get(0).contains("Please specify a valid URL"));
    }

    @Test
    public void validate_shouldAddError_whenUrlHasInvalidProtocol() {
        action.setServerId("testServer");
        action.setUrl("ftp://example.com");

        action.validate();

        assertTrue("Expected field error on url", action.hasFieldErrors());
        assertNotNull("Expected url field error", action.getFieldErrors().get("url"));
        assertTrue("Expected protocol error message", 
            action.getFieldErrors().get("url").get(0).contains("URL should start with 'https://' or 'http://'"));
    }

    @Test
    public void validate_shouldAddError_whenUrlIsJustProtocol() {
        action.setServerId("testServer");
        action.setUrl("https://");

        action.validate();

        assertTrue("Expected field error on url", action.hasFieldErrors());
        assertNotNull("Expected url field error", action.getFieldErrors().get("url"));
        assertTrue("Expected MalformedURL error message", 
            action.getFieldErrors().get("url").get(0).contains("Please specify a valid URL"));
    }

    @Test
    public void validate_shouldAddError_whenUrlIsNull() {
        action.setServerId("testServer");
        action.setUrl(null);

        action.validate();

        assertTrue("Expected field error on url", action.hasFieldErrors());
        assertNotNull("Expected url field error", action.getFieldErrors().get("url"));
        assertTrue("Expected empty URL error message", 
            action.getFieldErrors().get("url").get(0).contains("Please specify a URL of a JFrog Platform"));
    }
}