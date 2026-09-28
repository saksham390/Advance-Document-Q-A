package com.example.rag.model;

import java.time.Instant;

public record DocumentMetadata(String documentId, String filename, long sizeBytes, int pages, Instant ingestedAt) {
}
