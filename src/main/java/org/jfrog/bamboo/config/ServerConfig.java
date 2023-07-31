package org.jfrog.bamboo.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static java.lang.String.format;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfig implements Serializable {
    private String serverId;
    private String url;
    private String username;
    private String password;
    private String accessToken;
    private String cliVersion;
    private String cliRepository;

    @Override
    public String toString() {
        return format("%s (%s)", serverId, url);
    }

    public void copy(@NotNull ServerConfig other) {
        serverId = other.serverId;
        url = other.url;
        username = other.username;
        password = other.password;
        accessToken = other.accessToken;
        cliVersion = other.cliVersion;
        cliRepository = other.cliRepository;
    }
}