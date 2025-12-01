package org.jfrog.bamboo.config;

import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

@Getter
@Setter
public class JfrogServerConfigAction extends BambooActionSupport implements GlobalAdminSecurityAware {

    private static final Logger log = LogManager.getLogger(JfrogServerConfigAction.class);

    private static final String MODE_ADD = "add";
    private static final String AUTH_TYPE_TOKEN = "token";
    private static final String AUTH_TYPE_BASIC = "basic";
    private static final String AUTH_TYPE_NO_AUTH = "noAuth";

    private String mode;
    private String authType;
    private boolean specificVersion;
    private boolean fromArtifactory;

    private String serverId;
    private String url;
    private String username;
    private String password;
    private String accessToken;
    private String cliVersion;
    private String cliRepository;
    private final ServerConfigManager serverConfigManager;
    private String testConnection;

    @Inject
    public JfrogServerConfigAction(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
        mode = MODE_ADD;
        authType = AUTH_TYPE_TOKEN;
        specificVersion = false;
        fromArtifactory = false;
    }

    @Override
    public void validate() {
        clearErrorsAndMessages();
        if (StringUtils.isBlank(serverId)) {
            addFieldError("serverId", "Please specify a Server ID identifier.");
        } else if (MODE_ADD.equals(mode) && serverConfigManager.getServerConfigById(serverId) != null) {
            addFieldError("serverId", "Server ID already exists.");
        } else if (!serverId.equals(StringEscapeUtils.escapeHtml4(serverId))) {
            addFieldError("serverId", "Server ID cannot contain html content");
        }

        if (StringUtils.isBlank(url)) {
            addFieldError("url", "Please specify a URL of a JFrog Platform.");
        } 
        else if (!StringUtils.startsWithIgnoreCase(url, "https://") && !StringUtils.startsWithIgnoreCase(url, "http://")) {
            addFieldError("url", "URL should start with 'https://' or 'http://'");
        } else if (StringUtils.startsWithIgnoreCase(url, "http://")) {
            addActionMessage("Warning: Using HTTP connection. HTTPS is recommended for secure communication.");
        }
        
        if (StringUtils.startsWithIgnoreCase(url, "https://") || StringUtils.startsWithIgnoreCase(url, "http://")) {
            try {
                new URL(url);
            } catch (MalformedURLException mue) {
                addFieldError("url", "Please specify a valid URL of a JFrog Platform. Invalid URL format provided.");
            }
        }

        if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)) {
            addFieldError("password", "Please specify the password of your JFrog Platform.");
        } else if (StringUtils.isBlank(username) && StringUtils.isNotBlank(password)) {
            addFieldError("username", "Please specify the username of your JFrog Platform.");
        }
    }

    @SuppressWarnings("unused")
    public String doAdd() {
        return INPUT;
    }

    @SuppressWarnings("unused")
    public String doCreate() {
        if (isTestConnection()) {
            testConnection();
            return INPUT;
        }

        serverConfigManager.addServerConfiguration(createServerConfig());
        return SUCCESS;
    }

    // Get current server config details from bandana to show on UI form
    @SuppressWarnings("unused")
    public String doEdit() throws IllegalArgumentException {
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(serverId);
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server configuration by the ID " + serverId);
        }
        serverId = serverConfig.getServerId();
        url = serverConfig.getUrl();
        username = serverConfig.getUsername();
        cliVersion = serverConfig.getCliVersion();
        cliRepository = serverConfig.getCliRepository();
        password = EncryptionHelper.encryptForUi(serverConfig.getPassword());
        accessToken = EncryptionHelper.encryptForUi(serverConfig.getAccessToken());

        if (StringUtils.isNotBlank(accessToken)) {
            authType = AUTH_TYPE_TOKEN;
        } else if (StringUtils.isNotBlank(password)) {
            authType = AUTH_TYPE_BASIC;
        }

        if (StringUtils.isNotBlank(cliVersion)) {
            specificVersion = true;
        }

        if (StringUtils.isNotBlank(cliRepository)) {
            fromArtifactory = true;
        }
        return INPUT;
    }

    @SuppressWarnings("unused")
    public String doUpdate() {
        // Decrypt password from UI, if encrypted.
        password = EncryptionHelper.decryptIfNeeded(password);
        accessToken = EncryptionHelper.decryptIfNeeded(accessToken);

        if (isTestConnection()) {
            testConnection();
            // Encrypt password when returning it to UI.
            password = EncryptionHelper.encryptForUi(password);
            accessToken = EncryptionHelper.encryptForUi(accessToken);
            return INPUT;
        }
        serverConfigManager.updateServerConfiguration(createServerConfig());
        return SUCCESS;
    }

    private void testConnection() {
        ServerConfig serverConfig = createServerConfig();
        try (ArtifactoryManager manager = new ArtifactoryManager(serverConfig.getUrl() + "/artifactory", serverConfig.getUsername(), serverConfig.getPassword(), serverConfig.getAccessToken(), new BuildLog())) {
            ArtifactoryVersion rtVersion = manager.getVersion();
            if (rtVersion == null || rtVersion.equals(ArtifactoryVersion.NOT_FOUND)) {
                addActionError("Couldn't reach JFrog Artifactory server");
            }
            addActionMessage("Connection successful! JFrog Artifactory version: " + rtVersion);
        } catch (Exception e) {
            addActionError("Connection failed: Unable to connect to JFrog Platform");
            log.error("Error while testing the connection to Artifactory server", e);
        }
    }

    @NotNull
    private ServerConfig createServerConfig() {
        return new ServerConfig(
                serverId,
                StringUtils.removeEnd(url, "/"),
                authType.equals(AUTH_TYPE_BASIC) ? username : "",
                authType.equals(AUTH_TYPE_BASIC) ? password : "",
                authType.equals(AUTH_TYPE_TOKEN) ? accessToken : "",
                specificVersion ? StringUtils.removeStart(cliVersion, "v") : "",
                fromArtifactory ? cliRepository : ""
        );
    }

    @SuppressWarnings("unused")
    public String doDelete() {
        serverConfigManager.deleteServerConfiguration(serverId);
        return SUCCESS;
    }

    @SuppressWarnings("unused")
    public String doBrowse() throws Exception {
        return super.execute();
    }

    @SuppressWarnings("unused")
    public String browse() throws Exception {
        return super.execute();
    }

    @SuppressWarnings("unused")
    public String confirm() {
        return SUCCESS;
    }

    private boolean isTestConnection() {
        return StringUtils.isNotBlank(testConnection);
    }

}
