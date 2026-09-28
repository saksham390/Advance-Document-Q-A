package com.example.rag.service;

import com.example.rag.config.RagProperties;
import com.example.rag.model.MetadataFilter;
import com.example.rag.model.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RetrievalService {
    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);
    private final VectorStore vectorStore;
    private final RerankingService rerankingService;
    private final RagProperties properties;

    public RetrievalService(VectorStore vectorStore, RerankingService rerankingService, RagProperties properties) {
        this.vectorStore = vectorStore;
        this.rerankingService = rerankingService;
        this.properties = properties;
    }

    public List<RetrievedChunk> retrieve(List<String> queries, MetadataFilter filter) {
        Map<String, RetrievedChunk> unique = new LinkedHashMap<>();
        for (String query : queries) {
            SearchRequest.Builder builder = SearchRequest.builder().query(query).topK(properties.retrievalTopK());
            Filter.Expression expression = filterExpression(filter);
            if (expression != null) builder.filterExpression(expression);
            List<Document> documents = vectorStore.similaritySearch(builder.build());
            if (documents != null) {
                for (Document document : documents) {
                    double score = document.getMetadata().get("distance") instanceof Number number ? number.doubleValue() : 0.0;
                    unique.putIfAbsent(document.getId(), new RetrievedChunk(document, score));
                }
            }
        }
        List<RetrievedChunk> reranked = rerankingService.rerank(queries.get(0), new ArrayList<>(unique.values()), properties.finalContextChunks());
        log.info("retrievedCandidates={} selectedChunks={}", unique.size(), reranked.size());
        return reranked;
    }

    public void deleteByDocumentId(String documentId) {
        vectorStore.delete(new FilterExpressionBuilder().eq("documentId", documentId).build());
    }

    private Filter.Expression filterExpression(MetadataFilter filter) {
        if (filter == null || filter.isEmpty()) return null;
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        List<FilterExpressionBuilder.Op> expressions = new ArrayList<>();
        if (filter.documentId() != null) expressions.add(builder.eq("documentId", filter.documentId()));
        if (filter.documentType() != null) expressions.add(builder.eq("documentType", filter.documentType()));
        if (filter.pageNumber() != null) expressions.add(builder.eq("pageNumber", filter.pageNumber()));
        FilterExpressionBuilder.Op expression = expressions.get(0);
        for (int index = 1; index < expressions.size(); index++) expression = builder.and(expression, expressions.get(index));
        return expression.build();
    }
}
