package com.teamup.server.modules.chat.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.teamup.server.modules.chat.handler.ChatEventHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocketIORunner implements CommandLineRunner {

    private final SocketIOServer server;
    private final ChatEventHandler chatEventHandler;

    @Override
    public void run(String... args) throws Exception {
        // 手动添加事件监听器
        server.addListeners(chatEventHandler);
        
        server.start();
        log.info("SocketIO Server started on port " + server.getConfiguration().getPort());
        log.info("ChatEventHandler registered successfully");
    }
}
