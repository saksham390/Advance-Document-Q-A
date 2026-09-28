package com.example.rag.model;

import java.util.List;

public record UploadResponse(List<DocumentMetadata> documents, int chunksIndexed) {
}
