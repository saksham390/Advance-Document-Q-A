package com.example.rag.model;

import org.springframework.ai.document.Document;

public record RetrievedChunk(Document document, double score) {
    public String documentId() { return String.valueOf(document.getMetadata().get("documentId")); }
    public String chunkId() { return String.valueOf(document.getMetadata().get("chunkId")); }
    public String filename() { return String.valueOf(document.getMetadata().get("filename")); }
    public int page() { return Integer.parseInt(String.valueOf(document.getMetadata().get("pageNumber"))); }
}
