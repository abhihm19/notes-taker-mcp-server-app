package com.mcp_server.notes.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotesServiceImplTest {

    @TempDir
    Path tempDir;

    private NotesServiceImpl notesService;

    @BeforeEach
    void setUp() {
        notesService = new NotesServiceImpl();
        ReflectionTestUtils.setField(notesService, "notesPath", tempDir.toString());
        notesService.init();
    }

    // -------------------------------------------------------
    // Create Note Tests
    // -------------------------------------------------------
    @Test
    void createNotes_shouldCreateNewNote() {
        String result = notesService.createNotes("Test content", "test-note");

        assertEquals("Note created: test-note.txt", result);
        assertTrue(new File(tempDir.toFile(), "test-note.txt").exists());
    }

    @Test
    void createNotes_shouldFailIfNoteExists() {
        notesService.createNotes("First content", "existing-note");

        String result = notesService.createNotes("Second content", "existing-note");

        assertEquals("Note already exists: existing-note.txt", result);
    }

    @Test
    void createNotes_shouldSanitizeFileName() {
        String result = notesService.createNotes("Content", "my../../../etc/passwd");

        // Verify the note was created with sanitized name (dots and slashes replaced
        // with underscores)
        assertTrue(result.startsWith("Note created: my_"));
        assertTrue(result.contains("etc_passwd.txt"));
        assertFalse(new File("/etc/passwd.txt").exists());
    }

    @Test
    void createNotes_shouldRejectInvalidName() {
        String result = notesService.createNotes("Content", "");

        assertEquals("Error: Invalid note name", result);
    }

    @Test
    void createNotes_shouldEnforceSizeLimit() {
        String largeContent = "x".repeat(1024 * 1024 + 1); // Over 1MB

        String result = notesService.createNotes(largeContent, "large-note");

        assertEquals("Error: Note content exceeds maximum size of 1MB", result);
    }

    // -------------------------------------------------------
    // Read Note Tests
    // -------------------------------------------------------
    @Test
    void readNoteContent_shouldReturnContent() throws IOException {
        createTestNote("readable-note.txt", "Hello World");

        String result = notesService.readNoteContent("readable-note");

        assertEquals("Hello World", result);
    }

    @Test
    void readNoteContent_shouldReturnNotFoundForMissingNote() {
        String result = notesService.readNoteContent("nonexistent");

        assertEquals("Note not found: nonexistent", result);
    }

    @Test
    void readNoteContent_shouldPreventPathTraversal() {
        String result = notesService.readNoteContent("../../../etc/passwd");

        assertNotEquals("root:x:0:0:", result); // Should not read /etc/passwd
    }

    // -------------------------------------------------------
    // Search Notes Tests
    // -------------------------------------------------------
    @Test
    void searchNotesByFileName_shouldFindMatchingNotes() throws IOException {
        createTestNote("meeting-notes.txt", "Content");
        createTestNote("project-notes.txt", "Content");
        createTestNote("todo.txt", "Content");

        List<String> results = notesService.searchNotesByFileName("notes");

        assertEquals(2, results.size());
        assertTrue(results.contains("meeting-notes.txt"));
        assertTrue(results.contains("project-notes.txt"));
    }

    @Test
    void searchNotesByFileName_shouldBeCaseInsensitive() throws IOException {
        createTestNote("MyNotes.txt", "Content");

        List<String> results = notesService.searchNotesByFileName("mynotes");

        assertEquals(1, results.size());
    }

    // -------------------------------------------------------
    // List All Notes Tests
    // -------------------------------------------------------
    @Test
    void listAllNotes_shouldReturnAllNotes() throws IOException {
        createTestNote("note1.txt", "Content");
        createTestNote("note2.txt", "Content");
        createTestNote("note3.txt", "Content");

        List<String> results = notesService.listAllNotes();

        assertEquals(3, results.size());
    }

    @Test
    void listAllNotes_shouldReturnEmptyListWhenNoNotes() {
        List<String> results = notesService.listAllNotes();

        assertTrue(results.isEmpty());
    }

    // -------------------------------------------------------
    // Delete Note Tests
    // -------------------------------------------------------
    @Test
    void deleteNotes_shouldDeleteExistingNote() throws IOException {
        createTestNote("to-delete.txt", "Content");

        String result = notesService.deleteNotes("to-delete");

        assertEquals("Note deleted: to-delete", result);
        assertFalse(new File(tempDir.toFile(), "to-delete.txt").exists());
    }

    @Test
    void deleteNotes_shouldReturnNotFoundForMissingNote() {
        String result = notesService.deleteNotes("nonexistent");

        assertEquals("Note not found: nonexistent", result);
    }

    // -------------------------------------------------------
    // Append to Note Tests
    // -------------------------------------------------------
    @Test
    void addToExistingNotes_shouldAppendContent() throws IOException {
        createTestNote("append-test.txt", "Original");

        String result = notesService.addToExistingNotes("Appended", "append-test");

        assertEquals("Content added to note: append-test", result);
        String content = notesService.readNoteContent("append-test");
        assertTrue(content.contains("Original"));
        assertTrue(content.contains("Appended"));
    }

    @Test
    void addToExistingNotes_shouldFailForMissingNote() {
        String result = notesService.addToExistingNotes("Content", "nonexistent");

        assertEquals("Note not found: nonexistent", result);
    }

    // -------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------
    private void createTestNote(String filename, String content) throws IOException {
        File file = new File(tempDir.toFile(), filename);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
