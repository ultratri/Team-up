package com.teamup.server.modules.message.mongo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageDocRepository extends MongoRepository<ChatMessageDoc, String> {
    Page<ChatMessageDoc> findByConversationIdAndIsRecalledFalse(Long conversationId, Pageable pageable);
}
