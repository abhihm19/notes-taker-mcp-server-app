package com.mcp_server.notes.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcp_server.notes.service.NotesService;

import jakarta.annotation.PostConstruct;

@Service
public class NotesServiceImpl implements NotesService {
	
	@Value("${notes.path}")
	private String notesPath;

	  // Ensures the folder exists
    @PostConstruct
    public void init() {
        File folder = new File(notesPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    // -------------------------------------------------------
    // 1. Search Notes
    // -------------------------------------------------------
    @Tool(
        name = "search_notes",
        description = "Search notes by filename. Returns a list of note filenames that contain the given search term. Useful for locating existing notes."
    )
    @Override
    public List<String> searchNotesByFileName(
            @ToolParam(description = "Partial or full note filename to search") 
            @JsonProperty("notesName") String notesName) {
        File dir = new File(notesPath);

        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }

        // Case-insensitive search
        String search = notesName.toLowerCase();

        return Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(f -> f.getName().toLowerCase().contains(search))
                .map(File::getName)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------
    // 2. Create a New Note
    // -------------------------------------------------------
    @Tool(
        name = "create_note",
        description = "Creates a new note file with the given name and content. Fails if the note already exists."
    )
    @Override
    public String createNotes(
            @ToolParam(description = "Content to write into the note") 
            @JsonProperty("notes") String notes,
            @ToolParam(description = "Name of the note (without extension)") 
            @JsonProperty("notesName") String notesName) {
        try {
            File file = new File(notesPath + File.separator + notesName + ".txt");

            if (file.exists()) {
                return "Note already exists: " + file.getName();
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(notes);
            }

            return "Note created: " + file.getName();

        } catch (Exception e) {
            return "Error creating note: " + e.getMessage();
        }
    }

    // -------------------------------------------------------
    // 3. Delete a Note
    // -------------------------------------------------------
    @Tool(
        name = "delete_note",
        description = "Deletes a note file by name. Returns success or failure message."
    )
    @Override
    public String deleteNotes(
            @ToolParam(description = "Name of the note to delete (without extension)") 
            @JsonProperty("notesName") String notesName) {
        File file = new File(notesPath + File.separator + notesName + ".txt");

        if (!file.exists()) {
            return "Note not found: " + notesName;
        }

        return file.delete()
                ? "Note deleted: " + notesName
                : "Could not delete note: " + notesName;
    }

    // -------------------------------------------------------
    // 4. Append to an Existing Note
    // -------------------------------------------------------
    @Tool(
        name = "append_to_note",
        description = "Appends content to an existing note file. Fails if the note does not exist."
    )
    @Override
    public String addToExistingNotes(
            @ToolParam(description = "Content to append to the note") 
            @JsonProperty("notes") String notes,
            @ToolParam(description = "Name of the note to update (without extension)") 
            @JsonProperty("notesName") String notesName) {
        try {
            File file = new File(notesPath + File.separator + notesName + ".txt");

            if (!file.exists()) {
                return "Note not found: " + notesName;
            }

            try (FileWriter writer = new FileWriter(file, true)) { // append = true
                writer.write(System.lineSeparator());
                writer.write(notes);
            }

            return "Content added to note: " + notesName;

        } catch (Exception e) {
            return "Error writing to note: " + e.getMessage();
        }
    }

}
