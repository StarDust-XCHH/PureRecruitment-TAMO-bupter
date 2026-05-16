package com.bupt.tarecruit.common.ai;

import com.alibaba.dashscope.common.Message;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AiProvider {
    AiProviderStatus getStatus();

    AiChatResult chat(AiChatRequest request, Consumer<String> chunkConsumer) throws IOException;

    record AiProviderStatus(String providerName,
                            boolean configured,
                            boolean available,
                            String message) {
    }

    record AiChatRequest(String ownerId,
                         String sessionId,
                         String userMessage,
                         Map<String, Object> context,
                         List<Message> messages,
                         List<AiAttachmentRef> attachments) {
    }

    record AiAttachmentRef(String attachmentId,
                           String fileName,
                           String mimeType,
                           Path path,
                           String sourceType,
                           String sourcePath,
                           String courseCode,
                           String applicationId) {
    }

    record AiChatResult(String providerName,
                        String text) {
    }
}
