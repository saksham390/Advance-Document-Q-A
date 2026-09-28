package com.example.rag.service;

import com.example.rag.config.RagProperties;
import com.example.rag.model.AskRequest;
import com.example.rag.model.AskResponse;
import com.example.rag.model.ConversationMessage;
import com.example.rag.model.RetrievedChunk;
import com.example.rag.model.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {
    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private final QueryRewriteService queryRewriteService;
    private final ConversationService conversationService;
    private final RetrievalService retrievalService;
    private final ChatClient chatClient;
    private final RagProperties properties;

    public RagService(QueryRewriteService queryRewriteService, ConversationService conversationService, RetrievalService retrievalService, ChatClient chatClient, RagProperties properties) {
        this.queryRewriteService = queryRewriteService;
        this.conversationService = conversationService;
        this.retrievalService = retrievalService;
        this.chatClient = chatClient;
        this.properties = properties;
    }

    public AskResponse answer(AskRequest request) {
        List<ConversationMessage> history = conversationService.history(request.conversationId());
        String rewritten = queryRewriteService.rewrite(request.question(), history);
        List<String> queries = queryRewriteService.expand(rewritten);
        List<RetrievedChunk> chunks = retrievalService.retrieve(queries, request.filters());
        if (chunks.isEmpty()) return new AskResponse("I could not find relevant information in the indexed documents.", rewritten, List.of());
        String context = buildContext(chunks);
        String answer = chatClient.prompt()
                .system("You are an enterprise document assistant. Answer only from the supplied retrieved context. Do not invent facts. If the context does not contain the answer, say so clearly. Cite factual claims inline using [filename, page N]. Distinguish retrieved facts from uncertainty.")
                .user("Question: " + rewritten + "\n\nRetrieved context:\n" + context)
                .call().content();
        if (answer == null || answer.isBlank()) throw new IllegalStateException("LLM returned an empty response");
        conversationService.add(request.conversationId(), "user", request.question());
        conversationService.add(request.conversationId(), "assistant", answer);
        List<Source> sources = chunks.stream().map(chunk -> new Source(chunk.filename(), chunk.page(), chunk.chunkId())).distinct().toList();
        log.info("queryReceived=true rewrittenQueryLength={} retrievedChunks={} selectedChunks={} responseLength={}", rewritten.length(), chunks.size(), sources.size(), answer.length());
        return new AskResponse(answer, rewritten, sources);
    }

    private String buildContext(List<RetrievedChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (RetrievedChunk chunk : chunks) {
            String block = "[Source: " + chunk.filename() + ", page " + chunk.page() + ", chunk " + chunk.chunkId() + "]\n" + chunk.document().getText() + "\n\n";
            if (context.length() + block.length() > properties.maxContextCharacters()) break;
            context.append(block);
        }
        if (context.isEmpty()) throw new IllegalStateException("Retrieved context exceeds configured context budget");
        return context.toString();
    }
}
