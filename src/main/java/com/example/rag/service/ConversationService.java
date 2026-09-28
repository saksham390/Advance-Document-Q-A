package com.example.rag.service;

import com.example.rag.config.RagProperties;
import com.example.rag.model.ConversationMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {
    private final Map<String, Deque<ConversationMessage>> conversations = new ConcurrentHashMap<>();
    private final RagProperties properties;

    public ConversationService(RagProperties properties) {
        this.properties = properties;
    }

    public void add(String conversationId, String role, String content) {
        if (conversationId == null || conversationId.isBlank()) return;
        Deque<ConversationMessage> history = conversations.computeIfAbsent(conversationId, ignored -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(new ConversationMessage(role, content));
            while (history.size() > properties.historyLimit()) history.removeFirst();
        }
    }

    public ArrayList<ConversationMessage> history(String conversationId) {
        if (conversationId == null) return new ArrayList<>();
        Deque<ConversationMessage> history = conversations.get(conversationId);
        if (history == null) return new ArrayList<>();
        synchronized (history) { return new ArrayList<>(history); }
    }
}
