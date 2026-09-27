package com.example.tts.service;

import com.example.tts.dto.DocumentExtractResponse;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentExtractionService {

    /**
     * Safely extract text from an uploaded document (TXT, PDF, DOCX).
     * Validates file type, size, MIME type, and signatures.
     * Temporary data is securely disposed of without permanent storage.
     *
     * @param file The uploaded MultipartFile
     * @return Extracted text response DTO
     */
    DocumentExtractResponse extractText(MultipartFile file);
}
