package com.example.invoicetracker.service;

import com.example.invoicetracker.model.enums.FileType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        fileStorageService = new FileStorageService();

        Field uploadDirField = FileStorageService.class.getDeclaredField("uploadDir");
        uploadDirField.setAccessible(true);
        uploadDirField.set(fileStorageService, tempDir.toString());
    }

    @Nested
    class FileExtensionTests {
        @Test
        void getFileExtension_withVariousExtensions() {
            assertEquals(".txt", fileStorageService.getFileExtension("file.txt"));
            assertEquals(".pdf", fileStorageService.getFileExtension("document.pdf"));
            assertEquals(".jpg", fileStorageService.getFileExtension("image.jpg"));
            assertEquals(".jpeg", fileStorageService.getFileExtension("photo.jpeg"));
            assertEquals(".png", fileStorageService.getFileExtension("picture.png"));
            assertEquals(".docx", fileStorageService.getFileExtension("document.docx"));
        }

        @Test
        void getFileExtension_withMultipleDots() {
            assertEquals(".gz", fileStorageService.getFileExtension("archive.tar.gz"));
            assertEquals(".js", fileStorageService.getFileExtension("app.config.js"));
        }

        @Test
        void getFileExtension_withNoExtension() {
            assertEquals("", fileStorageService.getFileExtension("noextension"));

            assertEquals(".", fileStorageService.getFileExtension("file."));

            assertEquals(".hiddenfile", fileStorageService.getFileExtension(".hiddenfile"));

            assertEquals("", fileStorageService.getFileExtension(""));
        }

        @Test
        void getFileExtension_withNullAndEmpty() {
            assertEquals("", fileStorageService.getFileExtension(null));
            assertEquals("", fileStorageService.getFileExtension(""));
            assertEquals("", fileStorageService.getFileExtension("   "));
        }

        @Test
        void getFileExtension_withSpacesInName() {
            assertEquals(".txt", fileStorageService.getFileExtension("my file.txt"));
            assertEquals(".pdf", fileStorageService.getFileExtension("document final.pdf"));
        }
    }

    @Nested
    class ContentTypeTests {
        @Test
        void determineContentType_withAllFileTypes() {
            assertEquals("application/pdf", fileStorageService.determineContentType(FileType.PDF));
            assertEquals("image/jpeg", fileStorageService.determineContentType(FileType.IMAGE));
        }

        @Test
        void determineContentType_withNull() {
            assertEquals("application/octet-stream", fileStorageService.determineContentType(null));
        }

        @Test
        void determineContentType_withUnsupportedFileType() {
            assertEquals("application/octet-stream", fileStorageService.determineContentType(null));
        }
    }

    @Nested
    class FileResponseTests {
        @Test
        void createFileResponse_success() {
            // Given
            byte[] content = "Hello World".getBytes();
            String fileName = "test.txt";
            String contentType = "text/plain";

            // When
            ResponseEntity<byte[]> response = fileStorageService.createFileResponse(content, contentType, fileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            assertArrayEquals(content, response.getBody());
            assertEquals(contentType, response.getHeaders().getContentType().toString());
            assertEquals("attachment; filename=\"test.txt\"",
                    response.getHeaders().getContentDisposition().toString());
        }

        @Test
        void createFileResponse_withDifferentContentTypes() {
            // Given
            byte[] content = "test".getBytes();

            // When & Then for PDF
            ResponseEntity<byte[]> pdfResponse = fileStorageService.createFileResponse(
                    content, "application/pdf", "document.pdf");
            assertEquals("application/pdf", pdfResponse.getHeaders().getContentType().toString());

            // When & Then for Image
            ResponseEntity<byte[]> imageResponse = fileStorageService.createFileResponse(
                    content, "image/jpeg", "photo.jpg");
            assertEquals("image/jpeg", imageResponse.getHeaders().getContentType().toString());

            // When & Then for Octet Stream
            ResponseEntity<byte[]> streamResponse = fileStorageService.createFileResponse(
                    content, "application/octet-stream", "file.bin");
            assertEquals("application/octet-stream", streamResponse.getHeaders().getContentType().toString());
}

        @Test
        void createFileResponse_withSpecialCharactersInFileName() {
            // Given
            byte[] content = "content".getBytes();
            String fileName = "file with spaces and (special) chars.txt";
            String contentType = "text/plain";

            // When
            ResponseEntity<byte[]> response = fileStorageService.createFileResponse(content, contentType, fileName);

            // Then
            assertTrue(response.getHeaders().getContentDisposition().getFilename()
                    .contains("file with spaces and (special) chars.txt"));
        }

        @Test
        void createFileResponse_withEmptyContent() {
            // Given
            byte[] content = new byte[0];
            String fileName = "empty.txt";
            String contentType = "text/plain";

            // When
            ResponseEntity<byte[]> response = fileStorageService.createFileResponse(content, contentType, fileName);

            // Then
            assertEquals(200, response.getStatusCodeValue());
            assertArrayEquals(content, response.getBody());
            assertEquals(0, response.getBody().length);
        }
    }

    @Nested
    class FileStorageOperationsTests {
        @Test
        void storeFile_success() throws IOException {
            // Given
            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "example.txt", "text/plain", "file content".getBytes());

            // When
            String fileName = fileStorageService.storeFile(mockFile);

            // Then
            assertNotNull(fileName);
            assertTrue(fileName.endsWith(".txt"));
            assertTrue(fileName.length() > 10); // UUID + extension

            // Verify file was actually created
            Path filePath = tempDir.resolve(fileName);
            assertTrue(Files.exists(filePath));
            assertEquals("file content", Files.readString(filePath));
        }

        @Test
        void storeFile_withDifferentFileTypes() throws IOException {
            // Test PDF
            MockMultipartFile pdfFile = new MockMultipartFile(
                    "file", "document.pdf", "application/pdf", "pdf content".getBytes());
            String pdfFileName = fileStorageService.storeFile(pdfFile);
            assertTrue(pdfFileName.endsWith(".pdf"));

            // Test Image
            MockMultipartFile imageFile = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", "image content".getBytes());
            String imageFileName = fileStorageService.storeFile(imageFile);
            assertTrue(imageFileName.endsWith(".jpg"));

            // Test without extension
            MockMultipartFile noExtFile = new MockMultipartFile(
                    "file", "noextension", "text/plain", "content".getBytes());
            String noExtFileName = fileStorageService.storeFile(noExtFile);
            assertFalse(noExtFileName.contains("."));
        }

        @Test
        void storeFile_createsDirectoryIfNotExists() throws IOException, NoSuchFieldException, SecurityException,
                IllegalArgumentException, IllegalAccessException {
            // Given 
            Path newDir = tempDir.resolve("new-subdir");
            Field uploadDirField = FileStorageService.class.getDeclaredField("uploadDir");
            uploadDirField.setAccessible(true);
            uploadDirField.set(fileStorageService, newDir.toString());

            MockMultipartFile mockFile = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "content".getBytes());

            // When
            String fileName = fileStorageService.storeFile(mockFile);

            // Then
            assertTrue(Files.exists(newDir));
            assertTrue(Files.exists(newDir.resolve(fileName)));
        }

        @Test
        void storeFile_overwritesExistingFile() throws IOException {
            // Given
            MockMultipartFile firstFile = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "first content".getBytes());
            String firstFileName = fileStorageService.storeFile(firstFile);

            MockMultipartFile secondFile = new MockMultipartFile(
                    "file", "test.txt", "text/plain", "second content".getBytes());

            // When
            String secondFileName = fileStorageService.storeFile(secondFile);

            Path filePath = tempDir.resolve(secondFileName);
            assertEquals("second content", Files.readString(filePath));

        }

        @Test
        void loadFile_success() throws IOException {
            // Given
            String content = "file content to load";
            Path testFile = tempDir.resolve("test-load.txt");
            Files.write(testFile, content.getBytes());

            // When
            byte[] loadedContent = fileStorageService.loadFile("test-load.txt");

            // Then
            assertArrayEquals(content.getBytes(), loadedContent);
        }

        @Test
        void loadFile_fileNotFound() {
            // When & Then
            IOException exception = assertThrows(IOException.class,
                    () -> fileStorageService.loadFile("nonexistent-file.txt"));
            assertTrue(exception.getMessage().contains("nonexistent-file.txt"));
        }

        @Test
        void deleteFile_success() throws IOException {
            // Given
            Path testFile = tempDir.resolve("test-delete.txt");
            Files.write(testFile, "content".getBytes());
            assertTrue(Files.exists(testFile));

            // When
            fileStorageService.deleteFile("test-delete.txt");

            // Then
            assertFalse(Files.exists(testFile));
        }

        @Test
        void deleteFile_fileNotExists() {
            // When & Then 
            assertDoesNotThrow(() -> fileStorageService.deleteFile("nonexistent-file.txt"));
        }

        @Test
        void deleteFile_afterLoad() throws IOException {
            // Given
            Path testFile = tempDir.resolve("test-cycle.txt");
            Files.write(testFile, "content".getBytes());

            byte[] content = fileStorageService.loadFile("test-cycle.txt");
            assertNotNull(content);

            // When
            fileStorageService.deleteFile("test-cycle.txt");

            // Then
            assertThrows(IOException.class, () -> fileStorageService.loadFile("test-cycle.txt"));
        }
    }

    @Nested
    class EdgeCasesTests {
        @Test
        void storeFile_withNullFile() {
            // When & Then
            assertThrows(NullPointerException.class, () -> fileStorageService.storeFile(null));
        }

        @Test
        void storeFile_withEmptyFile() throws IOException {
            // Given
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.txt", "text/plain", new byte[0]);

            // When
            String fileName = fileStorageService.storeFile(emptyFile);

            // Then
            assertNotNull(fileName);
            Path filePath = tempDir.resolve(fileName);
            assertTrue(Files.exists(filePath));
            assertEquals(0, Files.size(filePath));
        }

        @Test
        void storeFile_withVeryLongFileName() throws IOException {
            // Given
            String longName = "a".repeat(255) + ".txt";
            MockMultipartFile longFile = new MockMultipartFile(
                    "file", longName, "text/plain", "content".getBytes());

            // When
            String fileName = fileStorageService.storeFile(longFile);

            // Then
            assertNotNull(fileName);
            assertTrue(fileName.matches("[a-f0-9-]+\\.txt"));
        }

        @Test
        void storeFile_withUnicodeFileName() throws IOException {
            // Given
            MockMultipartFile unicodeFile = new MockMultipartFile(
                    "file", "文件.txt", "text/plain", "content".getBytes());

            // When
            String fileName = fileStorageService.storeFile(unicodeFile);

            // Then
            assertNotNull(fileName);
            assertTrue(fileName.endsWith(".txt"));
            assertTrue(fileName.matches("[a-f0-9-]+\\.txt"));
        }
    }

    @Nested
    class IntegrationFlowTests {
        @Test
        void completeFileLifecycle() throws IOException {
            // Store
            MockMultipartFile originalFile = new MockMultipartFile(
                    "file", "lifecycle.txt", "text/plain", "lifecycle content".getBytes());
            String fileName = fileStorageService.storeFile(originalFile);

            // Load
            byte[] loadedContent = fileStorageService.loadFile(fileName);
            assertEquals("lifecycle content", new String(loadedContent));

            // Create Response
            ResponseEntity<byte[]> response = fileStorageService.createFileResponse(
                    loadedContent, "text/plain", "downloaded.txt");
            assertEquals(200, response.getStatusCodeValue());

            // Delete
            fileStorageService.deleteFile(fileName);
            assertThrows(IOException.class, () -> fileStorageService.loadFile(fileName));
        }

        @Test
        void multipleFilesOperations() throws IOException {
            // Store multiple files
            MockMultipartFile file1 = new MockMultipartFile("file", "f1.txt", "text/plain", "content1".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("file", "f2.pdf", "application/pdf", "content2".getBytes());

            String name1 = fileStorageService.storeFile(file1);
            String name2 = fileStorageService.storeFile(file2);

            // Verify both exist
            assertArrayEquals("content1".getBytes(), fileStorageService.loadFile(name1));
            assertArrayEquals("content2".getBytes(), fileStorageService.loadFile(name2));

            // Delete one
            fileStorageService.deleteFile(name1);
            assertThrows(IOException.class, () -> fileStorageService.loadFile(name1));
            assertDoesNotThrow(() -> fileStorageService.loadFile(name2));

            // Delete the other
            fileStorageService.deleteFile(name2);
            assertThrows(IOException.class, () -> fileStorageService.loadFile(name2));
        }
    }
}