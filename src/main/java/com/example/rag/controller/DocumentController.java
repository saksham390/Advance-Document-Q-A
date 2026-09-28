package com.example.rag.controller;

import com.example.rag.model.DocumentMetadata;
import com.example.rag.model.DocumentSummary;
import com.example.rag.model.Source;
import com.example.rag.model.UploadResponse;
import com.example.rag.service.DocumentService;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Validated
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) { this.documentService = documentService; }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestPart("files") @NotEmpty List<MultipartFile> files) {
        DocumentService.IngestionResult result = documentService.ingest(files);
        return new UploadResponse(result.documents(), result.chunksIndexed());
    }

    @GetMapping
    public List<DocumentSummary> list() { return documentService.list(); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) { documentService.delete(id); }

    @GetMapping("/{id}/sources")
    public List<Source> sources(@PathVariable("id") String id) { return documentService.sources(id); }
}
