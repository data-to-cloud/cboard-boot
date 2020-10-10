package org.cboard.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * h2连接池配置
 */
@Configuration
public class H2Configuration {

    @Value("${aggregator.h2.url}")
    private String url;

    @Bean
    public BasicDataSource h2DataSource(){
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        dataSource.setMaxTotal(20);
        return dataSource;
    }
}
