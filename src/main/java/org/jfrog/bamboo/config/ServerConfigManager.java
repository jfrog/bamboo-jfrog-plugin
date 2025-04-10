package org.jfrog.bamboo.config;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.UnsupportedEncodingException;
import java.util.List;

public interface ServerConfigManager {
    List<ServerConfig> getAllServerConfigs();

    ServerConfig getServerConfigById(String serverId);

    void addServerConfiguration(ServerConfig serverConfig);

    void deleteServerConfiguration(String serverId);

    void updateServerConfiguration(ServerConfig updated);

    void persist() throws IllegalAccessException, UnsupportedEncodingException, JsonProcessingException;
}
