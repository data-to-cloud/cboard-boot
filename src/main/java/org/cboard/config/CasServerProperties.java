package org.cboard.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "caso.server")
public class CasServerProperties {

    private String url;

    private String login;

    private String logout;
}
