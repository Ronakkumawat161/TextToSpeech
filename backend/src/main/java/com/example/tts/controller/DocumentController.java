package com.example.tts.controller;

import com.example.tts.dto.DocumentExtractResponse;
import com.example.tts.service.DocumentExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentExtractionService documentExtractionService;

    /**
     * POST /api/documents/extract-text
     *
     * Upload document (TXT, PDF, DOCX) and extract readable text.
     * Validates file type, size, and content.
     * Does NOT generate speech automatically.
     * Temporary data is processed in-memory and discarded without permanent storage.
     *
     * @param file The uploaded MultipartFile document
     * @return Extracted text response DTO
     */
    @PostMapping(
            value = "/extract-text",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DocumentExtractResponse> extractText(
            @RequestParam("file") MultipartFile file
    ) {
        String rawFilename = file.getOriginalFilename();
        String safeLogFilename = rawFilename != null ? rawFilename.replaceAll("[\\r\\n]", "_") : "unnamed";
        log.info("Received document upload request: filename='{}', size={} bytes",
                safeLogFilename, file.getSize());

        DocumentExtractResponse response = documentExtractionService.extractText(file);
        return ResponseEntity.ok(response);
    }
}
