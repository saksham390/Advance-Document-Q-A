package com.example.rag.model;

import java.time.Instant;

public record DocumentSummary(String documentId, String filename, long sizeBytes, int pages, Instant ingestedAt) {
}
