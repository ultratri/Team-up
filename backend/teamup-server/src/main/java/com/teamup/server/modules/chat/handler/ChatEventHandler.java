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

@Component
@Slf4j
public class ChatEventHandler {

    private final SocketIOServer server;
    private final ChatService chatService;

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
    }

    @OnEvent("join_team")
    public void onJoinTeam(SocketIOClient client, Long teamId, AckRequest ackRequest) {
        String room = "team_" + teamId;
        client.joinRoom(room);
        log.info("Client {} joined room {}", client.getSessionId(), room);
        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData("Joined team " + teamId);
        }
    }

    @OnEvent("leave_team")
    public void onLeaveTeam(SocketIOClient client, Long teamId) {
        String room = "team_" + teamId;
        client.leaveRoom(room);
        log.info("Client {} left room {}", client.getSessionId(), room);
    }

    @OnEvent("send_message")
    public void onSendMessage(SocketIOClient client, Message message, AckRequest ackRequest) {
        log.info("Received message: {}", message);
        
        // Save to DB
        chatService.saveMessage(message);
        
        // Broadcast to room
        String room = "team_" + message.getTeamId();
        server.getRoomOperations(room).sendEvent("receive_message", message);
        
        if (ackRequest.isAckRequested()) {
            ackRequest.sendAckData("Message sent");
        }
    }
}
