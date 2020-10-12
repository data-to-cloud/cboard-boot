package org.cboard.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "caso.client")
public class CasClientProperties {

    private String url;

    private String check;
}
