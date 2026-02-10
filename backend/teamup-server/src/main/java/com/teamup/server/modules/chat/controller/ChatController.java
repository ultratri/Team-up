package com.teamup.server.modules.chat.controller;

import com.teamup.server.common.utils.Result;
import com.teamup.server.modules.chat.entity.Message;
import com.teamup.server.modules.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/history/{teamId}")
    public Result<List<Message>> getHistory(@PathVariable Long teamId, @RequestParam(defaultValue = "50") Integer limit) {
        return Result.success(chatService.getTeamHistory(teamId, limit));
    }
}
