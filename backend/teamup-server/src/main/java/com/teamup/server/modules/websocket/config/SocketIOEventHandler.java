package com.teamup.server.modules.websocket.config;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.teamup.server.modules.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * SocketIO 事件处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocketIOEventHandler implements CommandLineRunner {

    private final SocketIOServer server;
    private final WebSocketService webSocketService;

    @Override
    public void run(String... args) {
        // 连接监听
        server.addConnectListener(client -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (userId != null && !userId.isEmpty()) {
                try {
                    Long uid = Long.parseLong(userId);
                    webSocketService.registerUser(uid, client);
                    log.info("用户 {} 连接成功, SessionId: {}", uid, client.getSessionId());
                } catch (NumberFormatException e) {
                    log.error("无效的用户ID: {}", userId);
                }
            }
        });

        // 断开监听
        server.addDisconnectListener(client -> {
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (userId != null && !userId.isEmpty()) {
                try {
                    Long uid = Long.parseLong(userId);
                    webSocketService.removeUser(uid);
                    log.info("用户 {} 断开连接", uid);
                } catch (NumberFormatException e) {
                    log.error("无效的用户ID: {}", userId);
                }
            }
        });

        // 监听正在输入事件
        server.addEventListener("typing", Object.class, (client, data, ackSender) -> {
            log.debug("收到正在输入事件: {}", data);
            // 可以转发给会话其他成员
        });

        // 监听心跳
        server.addEventListener("ping", String.class, (client, data, ackSender) -> {
            client.sendEvent("pong", "pong");
        });

        // 监听用户加入
        server.addEventListener("user:join", java.util.Map.class, (client, data, ackSender) -> {
            log.info("用户加入事件: {}", data);
        });

        // 监听加入团队
        server.addEventListener("team:join", java.util.Map.class, (client, data, ackSender) -> {
            Object teamIdObj = data.get("teamId");
            if (teamIdObj != null) {
                String roomName = "team_" + teamIdObj;
                client.joinRoom(roomName);
                log.info("用户加入团队房间: {}", roomName);
                
                // 广播在线用户更新
                int onlineCount = server.getRoomOperations(roomName).getClients().size();
                server.getRoomOperations(roomName).sendEvent("user:online", java.util.Map.of(
                    "onlineCount", onlineCount
                ));
            }
        });

        // 监听离开团队
        server.addEventListener("team:leave", java.util.Map.class, (client, data, ackSender) -> {
            Object teamIdObj = data.get("teamId");
            if (teamIdObj != null) {
                String roomName = "team_" + teamIdObj;
                client.leaveRoom(roomName);
                log.info("用户离开团队房间: {}", roomName);
            }
        });

        // 监听发送消息
        server.addEventListener("message:send", java.util.Map.class, (client, data, ackSender) -> {
            log.info("收到发送消息事件: {}", data);
            Object teamIdObj = data.get("teamId");
            Object contentObj = data.get("content");
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            
            if (teamIdObj != null && contentObj != null && userId != null) {
                String roomName = "team_" + teamIdObj;
                
                // 构建消息对象
                java.util.Map<String, Object> message = new java.util.HashMap<>();
                message.put("id", System.currentTimeMillis());
                message.put("teamId", teamIdObj);
                message.put("senderId", Long.parseLong(userId));
                message.put("senderName", "用户" + userId);
                message.put("content", contentObj);
                message.put("messageType", data.getOrDefault("messageType", "TEXT"));
                message.put("createdAt", new java.util.Date());
                
                // 广播给团队房间所有成员
                server.getRoomOperations(roomName).sendEvent("message:new", message);
                log.info("消息已广播到房间: {}", roomName);
            }
        });

        // 监听开始输入
        server.addEventListener("typing:start", java.util.Map.class, (client, data, ackSender) -> {
            Object teamIdObj = data.get("teamId");
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (teamIdObj != null && userId != null) {
                String roomName = "team_" + teamIdObj;
                server.getRoomOperations(roomName).sendEvent("user:typing", java.util.Map.of(
                    "userId", Long.parseLong(userId),
                    "username", "用户" + userId
                ));
            }
        });

        // 监听停止输入
        server.addEventListener("typing:stop", java.util.Map.class, (client, data, ackSender) -> {
            Object teamIdObj = data.get("teamId");
            String userId = client.getHandshakeData().getSingleUrlParam("userId");
            if (teamIdObj != null && userId != null) {
                String roomName = "team_" + teamIdObj;
                server.getRoomOperations(roomName).sendEvent("user:stop-typing", java.util.Map.of(
                    "userId", Long.parseLong(userId)
                ));
            }
        });

        log.info("SocketIO 事件监听器已注册");
    }
}
