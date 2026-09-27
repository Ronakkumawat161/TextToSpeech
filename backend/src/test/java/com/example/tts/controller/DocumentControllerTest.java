package com.example.tts.controller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testExtractTextFromTxtFileSuccess() throws Exception {
        String content = "Welcome to document text extraction in VoxFlow.\nThis is a sample text file.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filename").value("sample.txt"))
                .andExpect(jsonPath("$.fileType").value("txt"))
                .andExpect(jsonPath("$.text").value(containsString("Welcome to document text extraction in VoxFlow.")))
                .andExpect(jsonPath("$.characterCount").isNumber())
                .andExpect(jsonPath("$.wordCount").isNumber());
    }

    @Test
    void testExtractTextFromTxtFileWithBomSuccess() throws Exception {
        byte[] textBytes = "BOM encoded plain text.".getBytes(StandardCharsets.UTF_8);
        byte[] bomBytes = new byte[textBytes.length + 3];
        bomBytes[0] = (byte) 0xEF;
        bomBytes[1] = (byte) 0xBB;
        bomBytes[2] = (byte) 0xBF;
        System.arraycopy(textBytes, 0, bomBytes, 3, textBytes.length);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bom-file.txt",
                "text/plain",
                bomBytes
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.text").value("BOM encoded plain text."));
    }

    @Test
    void testExtractTextFromPdfSuccess() throws Exception {
        byte[] pdfBytes = createPdfBytes("Hello from an uploaded PDF document.");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                pdfBytes
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filename").value("document.pdf"))
                .andExpect(jsonPath("$.fileType").value("pdf"))
                .andExpect(jsonPath("$.text").value(containsString("Hello from an uploaded PDF document.")));
    }

    @Test
    void testExtractTextFromDocxSuccess() throws Exception {
        byte[] docxBytes = createDocxBytes("Hello from a Word docx document paragraph.");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                docxBytes
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filename").value("report.docx"))
                .andExpect(jsonPath("$.fileType").value("docx"))
                .andExpect(jsonPath("$.text").value(containsString("Hello from a Word docx document paragraph.")));
    }

    @Test
    void testEmptyFileFailsValidation() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(emptyFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.error.message").value(containsString("cannot be empty")));
    }

    @Test
    void testUnsupportedExtensionRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.exe",
                "application/octet-stream",
                "executable code".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_DOCUMENT_TYPE"))
                .andExpect(jsonPath("$.error.message").value(containsString("Unsupported file format")));
    }

    @Test
    void testDisguisedPdfWithInvalidMagicBytesRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "This is not a real PDF file".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_DOCUMENT_TYPE"))
                .andExpect(jsonPath("$.error.message").value(containsString("Invalid PDF file")));
    }

    @Test
    void testDisguisedDocxWithInvalidMagicBytesRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "Not a valid zip or docx content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_DOCUMENT_TYPE"))
                .andExpect(jsonPath("$.error.message").value(containsString("Invalid DOCX file")));
    }

    @Test
    void testDisguisedExecutableTxtRejected() throws Exception {
        byte[] mzExecutableBytes = new byte[]{0x4D, 0x5A, 0x00, 0x01, 0x02};
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "virus.txt",
                "text/plain",
                mzExecutableBytes
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_DOCUMENT_TYPE"))
                .andExpect(jsonPath("$.error.message").value(containsString("Binary executable files cannot be processed")));
    }

    @Test
    void testPathTraversalFilenameSanitized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../../../etc/passwd.txt",
                "text/plain",
                "Clean file content for traversal test.".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/documents/extract-text").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.filename").value("passwd.txt"));
    }

    private byte[] createPdfBytes(String text) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(100, 700);
                cs.showText(text);
                cs.endText();
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] createDocxBytes(String text) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            XWPFParagraph p = doc.createParagraph();
            XWPFRun r = p.createRun();
            r.setText(text);
            doc.write(baos);
            return baos.toByteArray();
        }
    }
}
