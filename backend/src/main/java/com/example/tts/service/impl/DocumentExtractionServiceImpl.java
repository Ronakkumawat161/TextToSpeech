package com.example.tts.service.impl;

import com.example.tts.dto.DocumentExtractResponse;
import com.example.tts.exception.InvalidRequestException;
import com.example.tts.exception.UnsupportedDocumentException;
import com.example.tts.service.DocumentExtractionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class DocumentExtractionServiceImpl implements DocumentExtractionService {

    // Maximum allowed document file size: 10 Megabytes
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // Supported extensions in lowercase
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "pdf", "docx");

    // Standard MIME types mapped to extensions
    private static final Set<String> PDF_MIME_TYPES = Set.of(
            "application/pdf", "application/x-pdf", "application/acrobat", "applications/vnd.pdf", "text/pdf"
    );

    private static final Set<String> DOCX_MIME_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/zip",
            "application/x-zip-compressed"
    );

    private static final Set<String> TXT_MIME_TYPES = Set.of(
            "text/plain", "text/csv", "text/tab-separated-values", "text/markdown", "application/octet-stream"
    );

    @Override
    public DocumentExtractResponse extractText(MultipartFile file) {
        // 1. Validate file presence
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Please select a file to upload. The file cannot be empty.");
        }

        // 2. Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidRequestException(
                    "File size exceeds the maximum limit of 10MB (file size: " + formatFileSize(file.getSize()) + ")."
            );
        }

        // 3. Sanitize client-provided filename (strip path traversals, null bytes, control characters)
        String rawFilename = file.getOriginalFilename();
        String extension = extractExtension(rawFilename);
        String sanitizedFilename = sanitizeFilename(rawFilename, extension);

        log.info("Processing document extraction for file: '{}' (type: '{}', size: {} bytes)",
                sanitizedFilename, extension, file.getSize());

        // 4. Validate extension
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            log.warn("Rejected unsupported document extension '{}' for file '{}'", extension, sanitizedFilename);
            throw new UnsupportedDocumentException(
                    "Unsupported file format '." + (extension.isEmpty() ? "unknown" : extension) +
                    "'. Only TXT, PDF, and DOCX files are supported."
            );
        }

        // 5. Read file bytes securely in-memory
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read file bytes for '{}': {}", sanitizedFilename, e.getMessage());
            throw new InvalidRequestException("Failed to read uploaded document content: " + e.getMessage());
        }

        if (fileBytes.length == 0) {
            throw new InvalidRequestException("The uploaded file contains 0 bytes.");
        }

        // 6. Security & Magic Bytes Header Verification
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase().trim() : "";
        validateFileSignatures(fileBytes, extension, contentType, sanitizedFilename);

        // 7. Extract text safely based on verified document type
        String extractedText;
        switch (extension) {
            case "pdf":
                extractedText = extractPdfText(fileBytes, sanitizedFilename);
                break;
            case "docx":
                extractedText = extractDocxText(fileBytes, sanitizedFilename);
                break;
            case "txt":
                extractedText = extractPlainText(fileBytes, sanitizedFilename);
                break;
            default:
                throw new UnsupportedDocumentException("Unsupported document type: " + extension);
        }

        // 8. Validate extracted content
        if (extractedText == null || extractedText.trim().isEmpty()) {
            throw new InvalidRequestException(
                    "No readable text could be extracted from '" + sanitizedFilename +
                    "'. The document may be empty, image-only, or password-protected."
            );
        }

        String cleanedText = normalizeExtractedText(extractedText);
        long charCount = cleanedText.length();
        long wordCount = Arrays.stream(cleanedText.split("\\s+"))
                .filter(w -> !w.isBlank())
                .count();

        log.info("Successfully extracted {} characters ({} words) from '{}'",
                charCount, wordCount, sanitizedFilename);

        return DocumentExtractResponse.builder()
                .success(true)
                .text(cleanedText)
                .filename(sanitizedFilename)
                .fileType(extension)
                .characterCount(charCount)
                .wordCount(wordCount)
                .fileSize(file.getSize())
                .message("Text extracted successfully from " + sanitizedFilename)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Extracts readable text from PDF documents using Apache PDFBox 3.x.
     */
    private String extractPdfText(byte[] bytes, String filename) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.isEncrypted()) {
                log.warn("Rejected encrypted/password-protected PDF '{}'", filename);
                throw new InvalidRequestException(
                        "The PDF document '" + filename + "' is password-protected or encrypted. Please provide an unencrypted PDF."
                );
            }

            int pageCount = document.getNumberOfPages();
            log.debug("Extracting text from PDF '{}' with {} pages", filename, pageCount);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);

        } catch (InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDFBox extraction failed for '{}': {}", filename, e.getMessage(), e);
            throw new InvalidRequestException(
                    "Unable to read PDF file '" + filename + "'. The file may be damaged, corrupted, or invalid: " + e.getMessage()
            );
        }
    }

    /**
     * Extracts paragraphs and table contents from DOCX documents using Apache POI.
     */
    private String extractDocxText(byte[] bytes, String filename) {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder sb = new StringBuilder();

            // Extract document paragraphs
            for (XWPFParagraph p : doc.getParagraphs()) {
                String pText = p.getText();
                if (pText != null && !pText.isBlank()) {
                    sb.append(pText.trim()).append("\n\n");
                }
            }

            // Also extract structured tables if present
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cText = cell.getText();
                        if (cText != null && !cText.isBlank()) {
                            cells.add(cText.trim());
                        }
                    }
                    if (!cells.isEmpty()) {
                        sb.append(String.join(" | ", cells)).append("\n");
                    }
                }
                sb.append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("Apache POI DOCX extraction failed for '{}': {}", filename, e.getMessage(), e);
            throw new InvalidRequestException(
                    "Unable to read DOCX document '" + filename + "'. Please ensure it is a valid Word (.docx) document: " + e.getMessage()
            );
        }
    }

    /**
     * Safely reads plain text with automatic encoding detection (UTF-8, UTF-16, ISO-8859-1).
     */
    private String extractPlainText(byte[] bytes, String filename) {
        // Detect and handle Byte Order Marks (BOM)
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            // UTF-8 with BOM
            return new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
        }

        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            // UTF-16 Big Endian
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }

        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            // UTF-16 Little Endian
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }

        // Try strict UTF-8 decoding
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            log.debug("Strict UTF-8 decoding failed for '{}', falling back to ISO-8859-1: {}", filename, e.getMessage());
            // Fallback to ISO-8859-1 which maps each byte 1:1 without crashing
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }

    /**
     * Verifies file header magic bytes against expected file signatures to prevent disguised malware.
     */
    private void validateFileSignatures(byte[] bytes, String extension, String contentType, String filename) {
        if ("pdf".equals(extension)) {
            // PDF Magic Bytes: %PDF (0x25, 0x50, 0x44, 0x46)
            if (bytes.length < 4 ||
                    bytes[0] != 0x25 || bytes[1] != 0x50 || bytes[2] != 0x44 || bytes[3] != 0x46) {
                log.warn("Rejected file '{}' claiming to be PDF: invalid magic bytes signature", filename);
                throw new UnsupportedDocumentException(
                        "Invalid PDF file. The file content does not match the standard PDF document format."
                );
            }
        } else if ("docx".equals(extension)) {
            // DOCX Magic Bytes: PK\x03\x04 (ZIP signature 0x50, 0x4B, 0x03, 0x04)
            if (bytes.length < 4 ||
                    bytes[0] != 0x50 || bytes[1] != 0x4B || bytes[2] != 0x03 || bytes[3] != 0x04) {
                log.warn("Rejected file '{}' claiming to be DOCX: invalid ZIP header signature", filename);
                throw new UnsupportedDocumentException(
                        "Invalid DOCX file. The file content is not a valid Microsoft Word OpenXML document."
                );
            }
        } else if ("txt".equals(extension)) {
            // Guard against binary executable signatures disguised as .txt
            if (bytes.length >= 2 && bytes[0] == 0x4D && bytes[1] == 0x5A) {
                // Windows MZ executable signature
                log.warn("Security alert: executable binary header detected in text upload '{}'", filename);
                throw new UnsupportedDocumentException("Binary executable files cannot be processed as text documents.");
            }
            if (bytes.length >= 4 && bytes[0] == 0x7F && bytes[1] == 0x45 && bytes[2] == 0x4C && bytes[3] == 0x46) {
                // Linux ELF binary signature
                log.warn("Security alert: ELF binary header detected in text upload '{}'", filename);
                throw new UnsupportedDocumentException("Binary executable files cannot be processed as text documents.");
            }
        }
    }

    /**
     * Sanitizes untrusted client-provided filenames to prevent directory traversal and null-byte attacks.
     */
    private String sanitizeFilename(String raw, String ext) {
        if (raw == null || raw.isBlank()) {
            return "document_" + System.currentTimeMillis() + (ext.isEmpty() ? "" : "." + ext);
        }

        // Normalize separators and strip path
        String name = raw.replace('\\', '/');
        int slashIdx = name.lastIndexOf('/');
        if (slashIdx >= 0) {
            name = name.substring(slashIdx + 1);
        }

        // Strip path traversal dots, control characters, null bytes
        name = name.replaceAll("[\\p{Cntrl}\0]+", "");
        name = name.replaceAll("^\\.+", ""); // leading dots

        if (name.trim().isEmpty()) {
            return "document_" + System.currentTimeMillis() + (ext.isEmpty() ? "" : "." + ext);
        }

        return name.trim();
    }

    /**
     * Extracts extension safely from filename.
     */
    private String extractExtension(String filename) {
        if (filename == null || filename.isBlank()) return "";
        String clean = filename.replace('\\', '/');
        int slashIdx = clean.lastIndexOf('/');
        if (slashIdx >= 0) {
            clean = clean.substring(slashIdx + 1);
        }
        int dotIdx = clean.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < clean.length() - 1) {
            return clean.substring(dotIdx + 1).trim().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    /**
     * Normalizes line breaks and removes non-printable control characters (while preserving \t, \n, \r).
     */
    private String normalizeExtractedText(String text) {
        if (text == null) return "";
        // Remove null characters and non-printable control characters (except tab, newline, CR)
        String cleaned = text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
        // Normalize CRLF to LF
        cleaned = cleaned.replace("\r\n", "\n").replace("\r", "\n");
        // Collapse 3 or more consecutive newlines into 2
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        return cleaned.trim();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.ROOT, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
