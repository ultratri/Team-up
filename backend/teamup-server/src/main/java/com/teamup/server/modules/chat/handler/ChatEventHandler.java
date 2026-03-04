package com.teamup.server.modules.chat.handler;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.teamup.server.modules.chat.entity.Message;
import com.teamup.server.modules.chat.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class ChatEventHandler {

    private final SocketIOServer server;
    private final ChatService chatService;
    
    // 存储每个房间的在线用户 Map<roomName, Set<userId>>
    private final Map<String, Set<Long>> roomUsers = new ConcurrentHashMap<>();
    // 存储每个用户所在的房间 Map<sessionId, roomName>
    private final Map<String, String> sessionRooms = new ConcurrentHashMap<>();

    @Autowired
    public ChatEventHandler(SocketIOServer server, ChatService chatService) {
        this.server = server;
        this.chatService = chatService;
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        log.info("Client connected: {}", client.getSessionId());
    }

    @OnDisconnect
    public void onDisconnect(SocketIOClient client) {
        log.info("Client disconnected: {}", client.getSessionId());
        
        // 从房间中移除用户
        String sessionId = client.getSessionId().toString();
        String room = sessionRooms.remove(sessionId);
        
        if (room != null) {
            String userIdStr = client.getHandshakeData().getSingleUrlParam("userId");
            if (userIdStr != null) {
                try {
                    Long userId = Long.parseLong(userIdStr);
                    Set<Long> users = roomUsers.get(room);
                    if (users != null) {
                        users.remove(userId);
                        // 广播在线人数更新
                        broadcastOnlineCount(room);
                    }
                } catch (NumberFormatException e) {
                    log.error("Invalid userId format: {}", userIdStr);
                }
            }
        }
    }

    @OnEvent("join_team")
    public void onJoinTeam(SocketIOClient client, Long teamId, AckRequest ackRequest) {
        String room = "team_" + teamId;
        client.joinRoom(room);
        log.info("Client {} joined room {}", client.getSessionId(), room);
        
        // 获取用户ID
        String userIdStr = client.getHandshakeData().getSingleUrlParam("userId");
        if (userIdStr != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                
                // 添加到房间用户列表
                roomUsers.computeIfAbsent(room, k -> ConcurrentHashMap.newKeySet()).add(userId);
                sessionRooms.put(client.getSessionId().toString(), room);
                
                log.info("User {} joined room {}, online count: {}", userId, room, roomUsers.get(room).size());
                
                // 广播在线人数更新
                broadcastOnlineCount(room);
            } catch (NumberFormatException e) {
                log.error("Invalid userId format: {}", userIdStr);
            }
        }
        
        // 发送历史消息（最近50条）
        try {
            var history = chatService.getTeamHistory(teamId, 50);
            client.sendEvent("message:history", history);
            log.info("Sent {} history messages to client {}", history.size(), client.getSessionId());
        } catch (Exception e) {
            log.error("Failed to load history for team {}", teamId, e);
        }
        
        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData("Joined team " + teamId);
        }
    }

    @OnEvent("leave_team")
    public void onLeaveTeam(SocketIOClient client, Long teamId) {
        String room = "team_" + teamId;
        client.leaveRoom(room);
        log.info("Client {} left room {}", client.getSessionId(), room);
        
        // 从房间用户列表中移除
        String sessionId = client.getSessionId().toString();
        sessionRooms.remove(sessionId);
        
        String userIdStr = client.getHandshakeData().getSingleUrlParam("userId");
        if (userIdStr != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                Set<Long> users = roomUsers.get(room);
                if (users != null) {
                    users.remove(userId);
                    // 广播在线人数更新
                    broadcastOnlineCount(room);
                }
            } catch (NumberFormatException e) {
                log.error("Invalid userId format: {}", userIdStr);
            }
        }
    }

    @OnEvent("send_message")
    public void onSendMessage(SocketIOClient client, Message message, AckRequest ackRequest) {
        log.info("Received message: {}", message);
        
        try {
            // 保存到数据库
            Message savedMessage = chatService.saveMessage(message);
            
            // 广播到房间内所有客户端
            String room = "team_" + message.getTeamId();
            server.getRoomOperations(room).sendEvent("message:new", savedMessage);
            
            log.info("Message saved and broadcasted to room {}", room);
            
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData(savedMessage);
            }
        } catch (Exception e) {
            log.error("Failed to save/broadcast message", e);
            if (ackRequest.isAckRequested()) {
                ackRequest.sendAckData("Error: " + e.getMessage());
            }
        }
    }
    
    /**
     * 广播房间在线人数
     */
    private void broadcastOnlineCount(String room) {
        Set<Long> users = roomUsers.get(room);
        int count = users != null ? users.size() : 0;
        
        Map<String, Object> data = new ConcurrentHashMap<>();
        data.put("onlineCount", count);
        
        server.getRoomOperations(room).sendEvent("user:online", data);
        log.debug("Broadcasted online count {} to room {}", count, room);
    }
}
