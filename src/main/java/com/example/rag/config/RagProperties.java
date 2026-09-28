package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rag")
public record RagProperties(int chunkSize, int chunkOverlap, int retrievalTopK, int finalContextChunks,
                            int maxContextCharacters, int historyLimit, int multiQueryLimit) {
}
