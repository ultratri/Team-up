package com.teamup.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRestClientAutoConfiguration.class
}, scanBasePackages = "com.teamup.server")
@MapperScan({"com.teamup.server.modules.*.mapper", "com.teamup.server.common.audit"})
@EnableScheduling
@EnableFeignClients
@org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories(basePackages = "com.teamup.server.disabled")
public class TeamupServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TeamupServerApplication.class, args);
    }
}
