package com.teamup.server.modules.message.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "chat_messages")
@CompoundIndex(name = "conv_created_idx", def = "{'conversation_id': 1, 'created_at': -1}")
public class ChatMessageDoc {
    @Id
    private String id;

    @Indexed
    @Field("conversation_id")
    private Long conversationId;

    @Field("sender_id")
    private Long senderId;

    @Field("message_type")
    private String messageType;

    private String content;

    @Field("file_url")
    private String fileUrl;

    @Field("file_name")
    private String fileName;

    @Field("file_size")
    private Long fileSize;

    @Field("is_read")
    private Boolean isRead;

    @Field("is_recalled")
    private Boolean isRecalled;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("read_at")
    private LocalDateTime readAt;

    @Field("mysql_id")
    private Long mysqlId;
}
