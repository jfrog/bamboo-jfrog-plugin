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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerConfigManager implements Serializable {

    private final transient Logger log = LogManager.getLogger(ServerConfigManager.class);

    private static final String JFROG_CONFIG_KEY = "org.jfrog.bamboo.server.config";
    private final List<ServerConfig> configuredServers = new CopyOnWriteArrayList<>();
    private BandanaManager bandanaManager = null;

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
        } catch (IllegalAccessException | UnsupportedEncodingException e) {
            log.error("Could not add Artifactory configuration.", e);
        }
    }

    public void deleteServerConfiguration(final String serverId) {
        for (ServerConfig configuredServer : configuredServers) {
            if (Objects.equals(configuredServer.getServerId(), serverId)) {
                configuredServers.remove(configuredServer);
                try {
                    persist();
                } catch (IllegalAccessException | UnsupportedEncodingException e) {
                    log.error("Could not delete Artifactory configuration.", e);
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
                } catch (IllegalAccessException | UnsupportedEncodingException e) {
                    log.error("Could not update Artifactory configuration.", e);
                }
                break;
            }
        }
    }

    @Autowired
    public void setBandanaManager(BandanaManager bandanaManager) {
        this.bandanaManager = bandanaManager;
        try {
            setArtifactoryServers(bandanaManager);
        } catch (InstantiationException | IllegalAccessException | IOException e) {
            log.error("Could not load Artifactory configuration.", e);
        }
    }

    private void setArtifactoryServers(BandanaManager bandanaManager)
            throws IOException, InstantiationException, IllegalAccessException {

        String existingArtifactoryConfig = (String) bandanaManager.getValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, JFROG_CONFIG_KEY);
        if (StringUtils.isNotBlank(existingArtifactoryConfig)) {
            List<ServerConfig> serverConfigList = getServersFromXml(existingArtifactoryConfig);
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

    private synchronized void persist() throws IllegalAccessException, UnsupportedEncodingException {
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
        String serverConfigsString = toXMLString(serverConfigs);
        bandanaManager.setValue(PlanAwareBandanaContext.GLOBAL_CONTEXT, JFROG_CONFIG_KEY, serverConfigsString);
    }

    private List<ServerConfig> getServersFromXml(String stringXml) throws IllegalAccessException, InstantiationException {
        List<ServerConfig> serverConfigs = new ArrayList<>();
        List<String> stringServerConfigs = findAllObjects(stringXml);
        for (String stringServerConfig : stringServerConfigs) {
            serverConfigs.add(getObjectFromStringXml(stringServerConfig, ServerConfig.class));
        }
        return serverConfigs;
    }

    private <T> T getObjectFromStringXml(String stringT, Class<T> tClass) throws IllegalAccessException, InstantiationException {
        T object = tClass.newInstance();
        boolean accsessable;
        String value;
        for (Field field : tClass.getDeclaredFields()) {
            accsessable = field.isAccessible();
            field.setAccessible(true);
            value = findFirstObject(field.getName(), stringT, true);
            if (field.getType().equals(long.class)) {
                field.set(object, Long.parseLong(value));
            } else if (field.getType().equals(int.class)) {
                field.set(object, Integer.parseInt(value));
            } else {
                field.set(object, findFirstObject(field.getName(), stringT, true));
            }
            field.setAccessible(accsessable);
        }
        return object;
    }

    private List<String> findAllObjects(String scannedString) {
        List<String> foundStrings = new ArrayList<>();
        String foundString = findFirstObject(ServerConfig.class.getSimpleName(), scannedString, false);
        while (!"".equals(foundString)) {
            foundStrings.add(foundString);
            scannedString = scannedString.replaceFirst(foundString, "");
            foundString = findFirstObject(ServerConfig.class.getSimpleName(), scannedString, false);
        }
        return foundStrings;
    }

    private String findFirstObject(String objectToFind, String stringToScan, boolean dataOnly) {
        String patternString = String.format("<%s>?(.*?)</%s>", objectToFind, objectToFind);
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(stringToScan);
        if (matcher.find()) {
            if (dataOnly) {
                return new String(Base64.getDecoder().decode(matcher.group(1).getBytes()));
            }
            return matcher.group(0);
        }
        return "";
    }

    private String toXMLString(List<ServerConfig> serverConfigs) throws IllegalAccessException, UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        openTag(stringBuilder, "List");
        for (ServerConfig serverConfig : serverConfigs) {
            stringBuilder.append(toXMLString(serverConfig));
        }
        closeTag(stringBuilder, "List");
        return stringBuilder.toString();
    }

    private String toXMLString(Object object) throws IllegalAccessException, UnsupportedEncodingException {
        StringBuilder stringBuilder = new StringBuilder();
        openTag(stringBuilder, object.getClass().getSimpleName());
        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            String value = field.get(object) == null ? "" : field.get(object).toString();
            appendAttribute(stringBuilder, field.getName(), value);
        }
        closeTag(stringBuilder, object.getClass().getSimpleName());
        return stringBuilder.toString();
    }

    private void appendAttribute(StringBuilder stringBuilder, String field, String value) throws UnsupportedEncodingException {
        openTag(stringBuilder, field);
        // Encoding the value to Base64 to prevent saving special chars like % to the database
        stringBuilder.append(Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8)));
        closeTag(stringBuilder, field);
    }

    private void openTag(StringBuilder stringBuilder, String fieldName) {
        stringBuilder.append("<");
        stringBuilder.append(fieldName);
        stringBuilder.append(">");
    }

    private void closeTag(StringBuilder stringBuilder, String fieldName) {
        stringBuilder.append("</");
        stringBuilder.append(fieldName);
        stringBuilder.append(">");
    }
}
