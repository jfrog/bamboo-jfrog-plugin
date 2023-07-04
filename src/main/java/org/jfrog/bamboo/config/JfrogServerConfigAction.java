package org.jfrog.bamboo.config;

import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.net.MalformedURLException;
import java.net.URL;

@Getter
@Setter
public class JfrogServerConfigAction extends BambooActionSupport implements GlobalAdminSecurityAware {

    private static final Logger log = LogManager.getLogger(JfrogServerConfigAction.class);

    private String mode;
    private String serverId;
    private String url;
    private String username;
    private String password;
    private String accessToken;
    private final ServerConfigManager serverConfigManager;
    private String testConnection;

    public JfrogServerConfigAction(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
        mode = "add";
    }

    @Override
    public void validate() {
        clearErrorsAndMessages();
        if (StringUtils.isBlank(serverId)) {
            addFieldError("serverId", "Please specify a Server ID identifier.");
        } else if ("add".equals(mode) && serverConfigManager.getServerConfigById(serverId) != null) {
            addFieldError("serverId", "Server ID already exists.");
        }

        if (StringUtils.isBlank(url)) {
            addFieldError("url", "Please specify a URL of a JFrog Platform.");
        } else if (!StringUtils.startsWithIgnoreCase(url, "https://")) {
            addFieldError("url", "URL should start with 'https://'");
        } else {
            try {
                new URL(url);
            } catch (MalformedURLException mue) {
                addFieldError("url", "Please specify a valid URL of a JFrog Platform. " + ExceptionUtils.getRootCauseMessage(mue));
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

        serverConfigManager.addServerConfiguration(
                new ServerConfig(serverId, url, username, password, accessToken));
        return SUCCESS;
    }

    @SuppressWarnings("unused")
    public String doEdit() throws IllegalArgumentException {
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(serverId);
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server configuration by the ID " + serverId);
        }
        updateFieldsFromServerConfig(serverConfig);
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

    private void testConnection() {
        try (ArtifactoryManager manager = new ArtifactoryManager(url + "/artifactory", username, password, accessToken, new BuildLog(log))) {
            ArtifactoryVersion rtVersion = manager.getVersion();
            if (rtVersion == null || rtVersion.equals(ArtifactoryVersion.NOT_FOUND)) {
                addActionError("Couldn't reach JFrog Artifactory server");
            }
            addActionMessage("Connection successful! JFrog Artifactory version: " + rtVersion);
        } catch (Exception e) {
            addActionError("Connection failed: " + ExceptionUtils.getRootCauseMessage(e));
            log.error("Error while testing the connection to Artifactory server " + url, e);
        }
    }

    /**
     * Update fields to show in the server update page in the UI.
     * Encrypting password, so it won't show in the UI inspection.
     *
     * @param serverConfig - Server being updated.
     */
    private void updateFieldsFromServerConfig(ServerConfig serverConfig) {
        setServerId(serverConfig.getServerId());
        setUrl(serverConfig.getUrl());
        setUsername(serverConfig.getUsername());
        setPassword(EncryptionHelper.encryptForUi(serverConfig.getPassword()));
        setAccessToken(EncryptionHelper.encryptForUi(serverConfig.getAccessToken()));
    }

    @NotNull
    private ServerConfig createServerConfig() {
        return new ServerConfig(serverId, url, username, password, accessToken);
    }
}
