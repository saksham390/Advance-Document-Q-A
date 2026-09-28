package com.example.rag.service;

import com.example.rag.model.RetrievedChunk;

import java.util.List;

public interface RerankingService {
    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int limit);
}
