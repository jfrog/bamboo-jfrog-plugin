/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.bamboo.config;

import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jfrog.bamboo.utils.BuildLog;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.net.MalformedURLException;
import java.net.URL;

public class JfrogServerConfigAction extends BambooActionSupport implements GlobalAdminSecurityAware {

    private final transient Logger log = LogManager.getLogger(JfrogServerConfigAction.class);

    private String mode;
    private String serverId;
    private String url;
    private String username;
    private String password;
    private String accessToken;
    private String isSendTest;

    private final transient ServerConfigManager serverConfigManager;

    public JfrogServerConfigAction(ServerConfigManager serverConfigManager) {
        this.serverConfigManager = serverConfigManager;
        mode = "add";
    }

    @Override
    public void validate() {
        clearErrorsAndMessages();
        if (StringUtils.isBlank(serverId)) {
            addFieldError("serverId", "Please specify a Server ID identifier.");
        } else if (mode.equals("add") && serverConfigManager.getServerConfigById(serverId) != null) {
            addFieldError("serverId", "Server ID already exists.");
        }

        if (StringUtils.isBlank(url)) {
            addFieldError("url", "Please specify a URL of a JFrog Platform.");
        } else {
            try {
                new URL(url);
            } catch (MalformedURLException mue) {
                addFieldError("url", "Please specify a valid URL of a JFrog Platform.");
            }
        }

        if (StringUtils.isNotBlank(username) && StringUtils.isBlank(password)){
            addFieldError("password", "Please specify the password of your JFrog Platform.");
        } else if (StringUtils.isBlank(username) && StringUtils.isNotBlank(password)){
            addFieldError("username", "Please specify the username of your JFrog Platform.");
        }
    }

    public String doAdd() throws Exception {
        return INPUT;
    }

    public String doCreate() throws Exception {
        if (isTesting()) {
            testConnection();
            return INPUT;
        }

        serverConfigManager.addServerConfiguration(
                new ServerConfig(getServerId(), getUrl(), getUsername(), getPassword(), getAccessToken()));
        return SUCCESS;
    }

    public String doEdit() throws Exception {
        ServerConfig serverConfig = serverConfigManager.getServerConfigById(serverId);
        if (serverConfig == null) {
            throw new IllegalArgumentException("Could not find Artifactory server configuration by the ID " + serverId);
        }
        updateFieldsFromServerConfig(serverConfig);
        return INPUT;
   }


    public String doUpdate() throws Exception {
        // Decrypt password from UI, if encrypted.
        password = EncryptionHelper.decryptIfNeeded(password);
        accessToken = EncryptionHelper.decryptIfNeeded(accessToken);

        if (isTesting()) {
            testConnection();
            // Encrypt password when returning it to UI.
            password = EncryptionHelper.encryptForUi(password);
            accessToken = EncryptionHelper.encryptForUi(accessToken);
            return INPUT;
        }
        serverConfigManager.updateServerConfiguration(createServerConfig());
        return SUCCESS;
    }

    public String doDelete() throws Exception {
        serverConfigManager.deleteServerConfiguration(getServerId());
        return SUCCESS;
    }

    public String doBrowse() throws Exception {
        return super.execute();
    }

    public String browse() throws Exception {
        return super.execute();
    }

    public String confirm() throws Exception {
        return SUCCESS;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    private boolean isTesting() {
        return StringUtils.isNotBlank(isSendTest);
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getIsSendTest() {
        return isSendTest;
    }

    public void setSendTest(String sendTest) {
        isSendTest = sendTest;
    }

    private void testConnection() {
        try (ArtifactoryManager manager = new ArtifactoryManager(url + "/artifactory", username, password, accessToken, new BuildLog(log))) {
            ArtifactoryVersion rtVersion = manager.getVersion();
            if (rtVersion == null || rtVersion.equals(ArtifactoryVersion.NOT_FOUND)){
                addActionError("Couldn't reach JFrog Artifactory server");
            }
            addActionMessage("Connection successful! JFrog Artifactory version: " + rtVersion);
        } catch (Exception e) {
            addActionError("Connection failed: " + e.getMessage());
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
