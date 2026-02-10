package com.teamup.server.modules.websocket.service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.teamup.server.modules.message.dto.MessageDTO;
import com.teamup.server.modules.notification.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 推送服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SocketIOServer socketIOServer;
    
    // 用户ID -> SocketIO客户端映射
    private final Map<Long, SocketIOClient> userClients = new ConcurrentHashMap<>();

    /**
     * 注册用户连接
     */
    public void registerUser(Long userId, SocketIOClient client) {
        userClients.put(userId, client);
        log.info("用户 {} 已连接 WebSocket", userId);
    }

    /**
     * 移除用户连接
     */
    public void removeUser(Long userId) {
        userClients.remove(userId);
        log.info("用户 {} 断开 WebSocket", userId);
    }

    /**
     * 推送新消息给用户
     */
    public void pushMessage(Long userId, MessageDTO message) {
        SocketIOClient client = userClients.get(userId);
        if (client != null && client.isChannelOpen()) {
            client.sendEvent("new_message", message);
            log.debug("推送消息给用户 {}: {}", userId, message.getId());
        }
    }

    /**
     * 推送通知给用户
     */
    public void pushNotification(Long userId, Notification notification) {
        SocketIOClient client = userClients.get(userId);
        if (client != null && client.isChannelOpen()) {
            client.sendEvent("new_notification", notification);
            log.debug("推送通知给用户 {}: {}", userId, notification.getId());
        }
    }

    /**
     * 推送会话更新
     */
    public void pushConversationUpdate(Long userId, Object data) {
        SocketIOClient client = userClients.get(userId);
        if (client != null && client.isChannelOpen()) {
            client.sendEvent("conversation_update", data);
        }
    }

    /**
     * 推送正在输入状态
     */
    public void pushTypingStatus(Long userId, Long conversationId, String userName, boolean typing) {
        SocketIOClient client = userClients.get(userId);
        if (client != null && client.isChannelOpen()) {
            Map<String, Object> data = Map.of(
                "conversationId", conversationId,
                "userName", userName,
                "typing", typing
            );
            client.sendEvent("typing_status", data);
        }
    }

    /**
     * 广播消息给会话所有成员
     */
    public void broadcastToConversation(Long conversationId, String event, Object data, Long excludeUserId) {
        // 这里需要查询会话成员，然后推送给所有在线成员
        // 为简化实现，这里仅作示例
        userClients.forEach((userId, client) -> {
            if (!userId.equals(excludeUserId) && client.isChannelOpen()) {
                client.sendEvent(event, data);
            }
        });
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long userId) {
        SocketIOClient client = userClients.get(userId);
        return client != null && client.isChannelOpen();
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return (int) userClients.values().stream()
                .filter(SocketIOClient::isChannelOpen)
                .count();
    }
}
