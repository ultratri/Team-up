package com.teamup.server.modules.chat.service;

import com.teamup.server.modules.chat.entity.Message;
import java.util.List;

public interface ChatService {
    Message saveMessage(Message message);
    List<Message> getTeamHistory(Long teamId, Integer limit);
}
