package com.example.rag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    TokenTextSplitter tokenTextSplitter(RagProperties properties) {
        return TokenTextSplitter.builder()
                .withChunkSize(properties.chunkSize())
                .withMinChunkSizeChars(Math.max(200, properties.chunkSize() / 4))
                .withMinChunkLengthToEmbed(20)
                .withKeepSeparator(true)
                .build();
    }
}
