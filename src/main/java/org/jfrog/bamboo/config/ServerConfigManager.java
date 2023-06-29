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

import com.atlassian.bamboo.bandana.PlanAwareBandanaContext;
import com.atlassian.bandana.BandanaManager;
import com.atlassian.spring.container.ContainerManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerConfigManager implements Serializable {

    private final transient Logger log = LogManager.getLogger(ServerConfigManager.class);

    private static final String JFROG_CONFIG_KEY = "org.jfrog.bamboo.server.config";
    private final List<ServerConfig> configuredServers = new CopyOnWriteArrayList<>();
    private BandanaManager bandanaManager = null;
    private final ObjectMapper mapper = createMapper();

    public List<ServerConfig> getAllServerConfigs() {
        return new ArrayList<>(configuredServers);
    }

    public ServerConfig getServerConfigById(String serverId) {
        for (ServerConfig configuredServer : configuredServers) {
            if (Objects.equals(configuredServer.getServerId(), serverId)) {
                return configuredServer;
            }
        }

        return null;
    }

    public static ServerConfigManager getInstance() {
        ServerConfigManager serverConfigManager = new ServerConfigManager();
        ContainerManager.autowireComponent(serverConfigManager);
        return serverConfigManager;
    }

    public void addServerConfiguration(ServerConfig serverConfig) {
        configuredServers.add(serverConfig);
        try {
            persist();
        } catch (IllegalAccessException | UnsupportedEncodingException | JsonProcessingException e) {
            log.error("Could not add JFrog configuration.", e);
        }
    }

    public void deleteServerConfiguration(final String serverId) {
        for (ServerConfig configuredServer : configuredServers) {
            if (Objects.equals(configuredServer.getServerId(), serverId)) {
                configuredServers.remove(configuredServer);
                try {
                    persist();
                } catch (IllegalAccessException | UnsupportedEncodingException | JsonProcessingException e) {
                    log.error("Could not delete JFrog configuration.", e);
                }
                break;
            }
        }
    }

    public void updateServerConfiguration(ServerConfig updated) {
        for (ServerConfig configuredServer : configuredServers) {
            if (Objects.equals(configuredServer.getServerId(), updated.getServerId())) {
                configuredServer.setServerId(updated.getServerId());
                configuredServer.setUrl(updated.getUrl());
                configuredServer.setUsername(updated.getUsername());
                configuredServer.setPassword(updated.getPassword());
                configuredServer.setAccessToken(updated.getAccessToken());
                try {
                    persist();
                } catch (IllegalAccessException | UnsupportedEncodingException | JsonProcessingException e) {
                    log.error("Could not update JFrog configuration.", e);
                }
                break;
            }
        }
    }

    @Autowired
    public void setBandanaManager(BandanaManager bandanaManager) {
        this.bandanaManager = bandanaManager;
        try {
            setJfrogServers(bandanaManager);
        } catch (InstantiationException | IllegalAccessException | IOException e) {
            log.error("Could not load JFrog configuration.", e);
        }
    }

    private void setJfrogServers(BandanaManager bandanaManager)
            throws IOException, InstantiationException, IllegalAccessException {

        String existingServersListAction = (String) bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, JFROG_CONFIG_KEY);
        if (StringUtils.isNotBlank(existingServersListAction)) {
            List<ServerConfig> serverConfigList = mapper.readValue(existingServersListAction, new TypeReference<>(){});
            for (Object serverConfig : serverConfigList) {
                // Because of some class loader issues we had to get a workaround,
                // we serialize and deserialize the serverConfig object.
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(serverConfig);
                ServerConfig tempServerConfig = new ObjectMapper().readValue(json, ServerConfig.class);
                configuredServers.add(
                        new ServerConfig(
                                tempServerConfig.getServerId(),
                                tempServerConfig.getUrl(),
                                tempServerConfig.getUsername(),
                                EncryptionHelper.decrypt(tempServerConfig.getPassword()),
                                EncryptionHelper.decrypt(tempServerConfig.getAccessToken())
                        )
                );
            }
        }
    }

    private synchronized void persist() throws IllegalAccessException, UnsupportedEncodingException, JsonProcessingException {
        List<ServerConfig> serverConfigs = new ArrayList<>();

        for (ServerConfig serverConfig : configuredServers) {
            serverConfigs.add(
                    new ServerConfig(
                            serverConfig.getServerId(),
                            serverConfig.getUrl(),
                            serverConfig.getUsername(),
                            EncryptionHelper.encryptForConfig(serverConfig.getPassword()),
                            EncryptionHelper.encryptForConfig(serverConfig.getAccessToken())
                    )
            );
        }
        String serverConfigsString = mapper.writeValueAsString(serverConfigs);
        bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, JFROG_CONFIG_KEY, serverConfigsString);
    }

    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
