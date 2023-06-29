package org.jfrog.bamboo.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Override
    public String toString() {
        return format("%s (%s)", serverId , url);
    }
}