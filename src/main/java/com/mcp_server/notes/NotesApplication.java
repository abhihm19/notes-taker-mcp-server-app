package com.mcp_server.notes;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.mcp_server.notes.service.NotesService;

@SpringBootApplication
public class NotesApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotesApplication.class, args);
		
		// Keep the application running for MCP STDIO communication
		try {
			new CountDownLatch(1).await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Bean
	List<ToolCallback> danTools(NotesService notesService) {
		return List.of(ToolCallbacks.from(notesService));
	}
}
