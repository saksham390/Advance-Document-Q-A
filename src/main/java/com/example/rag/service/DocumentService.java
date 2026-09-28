package com.example.rag.service;

import com.example.rag.model.DocumentMetadata;
import com.example.rag.model.DocumentSummary;
import com.example.rag.model.Source;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DocumentService {
    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;
    private final RetrievalService retrievalService;
    private final Map<String, DocumentMetadata> documents = new ConcurrentHashMap<>();
    private final Map<String, List<Source>> documentSources = new ConcurrentHashMap<>();

    public DocumentService(VectorStore vectorStore, TokenTextSplitter splitter, RetrievalService retrievalService) {
        this.vectorStore = vectorStore;
        this.splitter = splitter;
        this.retrievalService = retrievalService;
    }

    public IngestionResult ingest(List<MultipartFile> files) {
        List<DocumentMetadata> metadata = new ArrayList<>();
        int indexed = 0;
        for (MultipartFile file : files) {
            if (file.isEmpty() || file.getOriginalFilename() == null || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
                throw new IllegalArgumentException("Only non-empty PDF files are accepted");
            }
            String documentId = UUID.randomUUID().toString();
            try (PDDocument pdf = Loader.loadPDF(file.getBytes())) {
                int pages = pdf.getNumberOfPages();
                List<Document> pageDocuments = new ArrayList<>();
                PDFTextStripper stripper = new PDFTextStripper();
                for (int page = 1; page <= pages; page++) {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String text = clean(stripper.getText(pdf));
                    if (!text.isBlank()) {
                        pageDocuments.add(new Document(text, Map.of("documentId", documentId, "filename", file.getOriginalFilename(), "pageNumber", page, "documentType", "pdf")));
                    }
                }
                List<Document> chunks = splitter.apply(pageDocuments);
                List<Document> enriched = new ArrayList<>();
                List<Source> sources = new ArrayList<>();
                for (Document chunk : chunks) {
                    Map<String, Object> chunkMetadata = new java.util.HashMap<>(chunk.getMetadata());
                    chunkMetadata.put("chunkId", chunk.getId());
                    enriched.add(new Document(chunk.getText(), chunkMetadata));
                    sources.add(new Source(file.getOriginalFilename(), Integer.parseInt(String.valueOf(chunkMetadata.get("pageNumber"))), chunk.getId()));
                }
                if (enriched.isEmpty()) throw new IllegalArgumentException("PDF contains no extractable text: " + file.getOriginalFilename());
                vectorStore.add(enriched);
                DocumentMetadata record = new DocumentMetadata(documentId, file.getOriginalFilename(), file.getSize(), pages, Instant.now());
                documents.put(documentId, record);
                documentSources.put(documentId, sources);
                metadata.add(record);
                indexed += enriched.size();
            } catch (IOException exception) {
                throw new IllegalArgumentException("Invalid PDF: " + file.getOriginalFilename(), exception);
            }
        }
        return new IngestionResult(metadata, indexed);
    }

    public List<DocumentSummary> list() {
        return documents.values().stream().map(document -> new DocumentSummary(document.documentId(), document.filename(), document.sizeBytes(), document.pages(), document.ingestedAt())).sorted(Comparator.comparing(DocumentSummary::ingestedAt).reversed()).toList();
    }

    public void delete(String documentId) {
        retrievalService.deleteByDocumentId(documentId);
        documents.remove(documentId);
        documentSources.remove(documentId);
    }

    public List<Source> sources(String documentId) {
        return documentSources.getOrDefault(documentId, List.of());
    }

    private String clean(String text) {
        return text.replace('\u0000', ' ').replaceAll("[ \\t]+", " ").replaceAll("\\n{3,}", "\\n\\n").trim();
    }

    public record IngestionResult(List<DocumentMetadata> documents, int chunksIndexed) {}
}
