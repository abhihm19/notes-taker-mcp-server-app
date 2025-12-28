package com.mcp_server.notes.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcp_server.notes.service.NotesService;

import jakarta.annotation.PostConstruct;

@Service
public class NotesServiceImpl implements NotesService {

    private static final Logger logger = LoggerFactory.getLogger(NotesServiceImpl.class);
    private static final int MAX_NOTE_SIZE_BYTES = 1024 * 1024; // 1MB limit
    private static final int MAX_FILENAME_LENGTH = 255;

    @Value("${notes.path:${user.home}/notes}")
    private String notesPath;

    private Path notesDirectory;

    @PostConstruct
    public void init() {
        notesDirectory = Path.of(notesPath).toAbsolutePath().normalize();
        File folder = notesDirectory.toFile();
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                logger.info("Created notes directory: {}", notesDirectory);
            } else {
                logger.warn("Failed to create notes directory: {}", notesDirectory);
            }
        }
        logger.info("Notes service initialized with path: {}", notesDirectory);
    }

    /**
     * Sanitizes the filename to prevent path traversal attacks.
     * Only allows alphanumeric characters, hyphens, and underscores.
     */
    private String sanitizeFileName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        // Remove any path separators and dangerous characters
        String sanitized = name.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        // Limit length
        if (sanitized.length() > MAX_FILENAME_LENGTH) {
            sanitized = sanitized.substring(0, MAX_FILENAME_LENGTH);
        }
        return sanitized;
    }

    /**
     * Validates that the file path is within the notes directory (prevents path
     * traversal).
     */
    private boolean isPathSafe(File file) {
        try {
            Path filePath = file.toPath().toAbsolutePath().normalize();
            return filePath.startsWith(notesDirectory);
        } catch (Exception e) {
            logger.error("Error validating path: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------
    // 1. Search Notes
    // -------------------------------------------------------
    @Tool(name = "search_notes", description = "Search notes by filename. Returns a list of note filenames that contain the given search term. Useful for locating existing notes.")
    @Override
    public List<String> searchNotesByFileName(
            @ToolParam(description = "Partial or full note filename to search") @JsonProperty("notesName") String notesName) {

        logger.debug("Searching for notes with name: {}", notesName);

        File dir = notesDirectory.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            logger.warn("Notes directory does not exist: {}", notesPath);
            return Collections.emptyList();
        }

        // Case-insensitive search
        String search = notesName != null ? notesName.toLowerCase() : "";

        List<String> results = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(f -> f.isFile() && f.getName().toLowerCase().contains(search))
                .map(File::getName)
                .collect(Collectors.toList());

        logger.info("Found {} notes matching '{}'", results.size(), notesName);
        return results;
    }

    // -------------------------------------------------------
    // 2. List All Notes
    // -------------------------------------------------------
    @Tool(name = "list_notes", description = "Lists all available notes in the notes directory. Returns filenames of all notes.")
    @Override
    public List<String> listAllNotes() {
        logger.debug("Listing all notes");

        File dir = notesDirectory.toFile();

        if (!dir.exists() || !dir.isDirectory()) {
            logger.warn("Notes directory does not exist: {}", notesPath);
            return Collections.emptyList();
        }

        List<String> notes = Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(File::isFile)
                .map(File::getName)
                .sorted()
                .collect(Collectors.toList());

        logger.info("Listed {} total notes", notes.size());
        return notes;
    }

    // -------------------------------------------------------
    // 3. Read Note Content
    // -------------------------------------------------------
    @Tool(name = "read_note", description = "Reads and returns the full content of a note file. Useful for viewing what's written in a note.")
    @Override
    public String readNoteContent(
            @ToolParam(description = "Name of the note to read (without extension)") @JsonProperty("notesName") String notesName) {

        String sanitizedName = sanitizeFileName(notesName);
        if (sanitizedName == null) {
            logger.warn("Invalid note name provided: {}", notesName);
            return "Error: Invalid note name";
        }

        logger.debug("Reading note: {}", sanitizedName);

        File file = new File(notesDirectory.toFile(), sanitizedName + ".txt");

        if (!isPathSafe(file)) {
            logger.error("Path traversal attempt detected: {}", notesName);
            return "Error: Invalid file path";
        }

        if (!file.exists()) {
            logger.warn("Note not found: {}", sanitizedName);
            return "Note not found: " + sanitizedName;
        }

        try {
            String content = Files.readString(file.toPath());
            logger.info("Successfully read note: {}", sanitizedName);
            return content;
        } catch (IOException e) {
            logger.error("Error reading note {}: {}", sanitizedName, e.getMessage());
            return "Error reading note: " + e.getMessage();
        }
    }

    // -------------------------------------------------------
    // 4. Create a New Note
    // -------------------------------------------------------
    @Tool(name = "create_note", description = "Creates a new note file with the given name and content. Fails if the note already exists.")
    @Override
    public String createNotes(
            @ToolParam(description = "Content to write into the note") @JsonProperty("notes") String notes,
            @ToolParam(description = "Name of the note (without extension)") @JsonProperty("notesName") String notesName) {

        String sanitizedName = sanitizeFileName(notesName);
        if (sanitizedName == null) {
            logger.warn("Invalid note name provided: {}", notesName);
            return "Error: Invalid note name";
        }

        // Check content size
        if (notes != null && notes.getBytes().length > MAX_NOTE_SIZE_BYTES) {
            logger.warn("Note content too large: {} bytes", notes.getBytes().length);
            return "Error: Note content exceeds maximum size of 1MB";
        }

        logger.debug("Creating note: {}", sanitizedName);

        try {
            File file = new File(notesDirectory.toFile(), sanitizedName + ".txt");

            if (!isPathSafe(file)) {
                logger.error("Path traversal attempt detected: {}", notesName);
                return "Error: Invalid file path";
            }

            if (file.exists()) {
                logger.warn("Note already exists: {}", sanitizedName);
                return "Note already exists: " + file.getName();
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(notes != null ? notes : "");
            }

            logger.info("Created note: {}", file.getName());
            return "Note created: " + file.getName();

        } catch (IOException e) {
            logger.error("Error creating note {}: {}", sanitizedName, e.getMessage());
            return "Error creating note: " + e.getMessage();
        }
    }

    // -------------------------------------------------------
    // 5. Delete a Note
    // -------------------------------------------------------
    @Tool(name = "delete_note", description = "Deletes a note file by name. Returns success or failure message.")
    @Override
    public String deleteNotes(
            @ToolParam(description = "Name of the note to delete (without extension)") @JsonProperty("notesName") String notesName) {

        String sanitizedName = sanitizeFileName(notesName);
        if (sanitizedName == null) {
            logger.warn("Invalid note name provided: {}", notesName);
            return "Error: Invalid note name";
        }

        logger.debug("Deleting note: {}", sanitizedName);

        File file = new File(notesDirectory.toFile(), sanitizedName + ".txt");

        if (!isPathSafe(file)) {
            logger.error("Path traversal attempt detected: {}", notesName);
            return "Error: Invalid file path";
        }

        if (!file.exists()) {
            logger.warn("Note not found for deletion: {}", sanitizedName);
            return "Note not found: " + sanitizedName;
        }

        boolean deleted = file.delete();
        if (deleted) {
            logger.info("Deleted note: {}", sanitizedName);
            return "Note deleted: " + sanitizedName;
        } else {
            logger.error("Failed to delete note: {}", sanitizedName);
            return "Could not delete note: " + sanitizedName;
        }
    }

    // -------------------------------------------------------
    // 6. Append to an Existing Note
    // -------------------------------------------------------
    @Tool(name = "append_to_note", description = "Appends content to an existing note file. Fails if the note does not exist.")
    @Override
    public String addToExistingNotes(
            @ToolParam(description = "Content to append to the note") @JsonProperty("notes") String notes,
            @ToolParam(description = "Name of the note to update (without extension)") @JsonProperty("notesName") String notesName) {

        String sanitizedName = sanitizeFileName(notesName);
        if (sanitizedName == null) {
            logger.warn("Invalid note name provided: {}", notesName);
            return "Error: Invalid note name";
        }

        logger.debug("Appending to note: {}", sanitizedName);

        try {
            File file = new File(notesDirectory.toFile(), sanitizedName + ".txt");

            if (!isPathSafe(file)) {
                logger.error("Path traversal attempt detected: {}", notesName);
                return "Error: Invalid file path";
            }

            if (!file.exists()) {
                logger.warn("Note not found for appending: {}", sanitizedName);
                return "Note not found: " + sanitizedName;
            }

            // Check if appending would exceed size limit
            long currentSize = file.length();
            int appendSize = notes != null ? notes.getBytes().length : 0;
            if (currentSize + appendSize > MAX_NOTE_SIZE_BYTES) {
                logger.warn("Append would exceed size limit. Current: {}, Append: {}", currentSize, appendSize);
                return "Error: Adding this content would exceed maximum note size of 1MB";
            }

            try (FileWriter writer = new FileWriter(file, true)) { // append = true
                writer.write(System.lineSeparator());
                writer.write(notes != null ? notes : "");
            }

            logger.info("Appended content to note: {}", sanitizedName);
            return "Content added to note: " + sanitizedName;

        } catch (IOException e) {
            logger.error("Error appending to note {}: {}", sanitizedName, e.getMessage());
            return "Error writing to note: " + e.getMessage();
        }
    }
}
