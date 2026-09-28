package com.example.rag.service;

import com.example.rag.model.RetrievedChunk;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BaselineRerankingService implements RerankingService {
    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int limit) {
        Set<String> terms = Set.of(query.toLowerCase(Locale.ROOT).split("\\W+")).stream().filter(term -> term.length() > 2).collect(Collectors.toSet());
        return candidates.stream()
                .sorted(Comparator.comparingDouble((RetrievedChunk chunk) -> lexicalBoost(chunk, terms)).reversed()
                .thenComparing(Comparator.comparingDouble(RetrievedChunk::score).reversed()))
                .limit(limit)
                .toList();
    }

    private double lexicalBoost(RetrievedChunk chunk, Set<String> terms) {
        String text = chunk.document().getText().toLowerCase(Locale.ROOT);
        long matches = terms.stream().filter(text::contains).count();
        return chunk.score() + (terms.isEmpty() ? 0 : (double) matches / terms.size() * 0.15);
    }
}
