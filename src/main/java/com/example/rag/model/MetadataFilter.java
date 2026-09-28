package com.example.rag.model;

public record MetadataFilter(String documentId, String documentType, Integer pageNumber) {
    public boolean isEmpty() {
        return documentId == null && documentType == null && pageNumber == null;
    }
}
