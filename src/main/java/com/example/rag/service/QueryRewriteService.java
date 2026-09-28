package com.example.rag.service;

import com.example.rag.config.RagProperties;
import com.example.rag.model.ConversationMessage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryRewriteService {
    private final ChatClient chatClient;
    private final RagProperties properties;

    public QueryRewriteService(ChatClient chatClient, RagProperties properties) {
        this.chatClient = chatClient;
        this.properties = properties;
    }

    public String rewrite(String question, List<ConversationMessage> history) {
        if (history.isEmpty()) return question.trim();
        String transcript = history.stream().map(message -> message.role() + ": " + message.content()).reduce("", (a, b) -> a + b + "\n");
        String result = chatClient.prompt()
                .system("Rewrite the latest user question into one standalone search query. Resolve pronouns using the conversation. Return only the query, with no explanation.")
                .user("Conversation:\n" + transcript + "\nLatest question:\n" + question)
                .call().content();
        return result == null || result.isBlank() ? question.trim() : result.trim();
    }

    public List<String> expand(String rewrittenQuery) {
        String result = chatClient.prompt()
                .system("Generate up to " + properties.multiQueryLimit() + " concise, meaning-preserving search queries for document retrieval. Return one query per line and no bullets.")
                .user(rewrittenQuery)
                .call().content();
        if (result == null || result.isBlank()) return List.of(rewrittenQuery);
        return result.lines().map(String::trim).filter(line -> !line.isBlank()).distinct().limit(properties.multiQueryLimit()).toList();
    }
}
