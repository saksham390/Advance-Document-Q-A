package com.example.rag.service;

import com.example.rag.model.RetrievedChunk;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaselineRerankingServiceTest {
    @Test
    void lexicalEvidenceCanImproveOrdering() {
        Document relevant = new Document("Spring Boot advantages include convention over configuration", Map.of("filename", "guide.pdf", "pageNumber", 1, "chunkId", "a"));
        Document generic = new Document("A general introduction to application frameworks", Map.of("filename", "guide.pdf", "pageNumber", 2, "chunkId", "b"));
        List<RetrievedChunk> result = new BaselineRerankingService().rerank("Spring Boot advantages", List.of(new RetrievedChunk(generic, .9), new RetrievedChunk(relevant, .8)), 2);
        assertThat(result.get(0).document().getText()).contains("advantages");
    }
}
