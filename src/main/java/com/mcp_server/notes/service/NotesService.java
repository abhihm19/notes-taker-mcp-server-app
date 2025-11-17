package com.mcp_server.notes.service;

import java.util.List;

public interface NotesService {
	
	List<String> searchNotesByFileName(String notesName);
	String createNotes(String notes, String notesName);
	String deleteNotes(String notesName);
	String addToExistingNotes(String notes, String notesName);

}
