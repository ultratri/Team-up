package com.teamup.server.modules.chat.config;

import com.corundumstudio.socketio.SocketIOServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SocketIOConfig {

    @Value("${socketio.host:localhost}")
    private String host;

    @Value("${socketio.port:9092}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        // 使用 0.0.0.0 监听所有网络接口
        config.setHostname("0.0.0.0");
        config.setPort(port);
        
        // 允许跨域请求（开发环境）
        config.setOrigin("*");
        
        // 允许所有传输方式
        config.setTransports(com.corundumstudio.socketio.Transport.WEBSOCKET, 
                            com.corundumstudio.socketio.Transport.POLLING);
        
        // 设置连接超时
        config.setPingTimeout(60000);
        config.setPingInterval(25000);
        
        // Configure JSON support for Java 8 dates
        com.corundumstudio.socketio.protocol.JacksonJsonSupport jsonSupport = new com.corundumstudio.socketio.protocol.JacksonJsonSupport(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        config.setJsonSupport(jsonSupport);
        
        return new SocketIOServer(config);
    }
}
