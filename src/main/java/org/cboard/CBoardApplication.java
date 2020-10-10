package org.cboard;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(exclude = {
        SolrAutoConfiguration.class
})
@MapperScan(basePackages = {
        "org.cboard.dao"
})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CBoardApplication {
    public static void main(String[] args) {
        SpringApplication.run(CBoardApplication.class,args);
    }
}
