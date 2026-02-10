package com.teamup.server.modules.message.mongo;

import com.teamup.server.modules.message.entity.ChatMessage;

public class ChatMessageMongoConverter {

    public static ChatMessageDoc toDoc(ChatMessage entity) {
        if (entity == null) {
            return null;
        }
        ChatMessageDoc doc = new ChatMessageDoc();
        doc.setConversationId(entity.getConversationId());
        doc.setSenderId(entity.getSenderId());
        doc.setMessageType(entity.getMessageType());
        doc.setContent(entity.getContent());
        doc.setFileUrl(entity.getFileUrl());
        doc.setFileName(entity.getFileName());
        doc.setFileSize(entity.getFileSize());
        doc.setIsRead(entity.getIsRead());
        doc.setIsRecalled(entity.getIsRecalled());
        doc.setCreatedAt(entity.getCreatedAt());
        doc.setReadAt(entity.getReadAt());
        doc.setMysqlId(entity.getId());
        return doc;
    }

    public static ChatMessage toEntity(ChatMessageDoc doc) {
        if (doc == null) {
            return null;
        }
        ChatMessage entity = new ChatMessage();
        entity.setId(doc.getMysqlId());
        entity.setConversationId(doc.getConversationId());
        entity.setSenderId(doc.getSenderId());
        entity.setMessageType(doc.getMessageType());
        entity.setContent(doc.getContent());
        entity.setFileUrl(doc.getFileUrl());
        entity.setFileName(doc.getFileName());
        entity.setFileSize(doc.getFileSize());
        entity.setIsRead(doc.getIsRead());
        entity.setIsRecalled(doc.getIsRecalled());
        entity.setCreatedAt(doc.getCreatedAt());
        entity.setReadAt(doc.getReadAt());
        return entity;
    }
}
