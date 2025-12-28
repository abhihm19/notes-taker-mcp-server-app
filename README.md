# Notes Taker MCP Server

A **Model Context Protocol (MCP) Server** built with Spring Boot and Spring AI that provides AI assistants with the ability to manage notes through file-based storage.

## 🚀 Overview

This application implements an MCP server that exposes tools for creating, reading, updating, and deleting notes. It integrates with AI assistants (like Claude, Gemini, etc.) via the Model Context Protocol, enabling them to manage notes on behalf of users.

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| **Framework** | Spring Boot 3.5.7 |
| **AI Integration** | Spring AI 1.1.0 |
| **Protocol** | MCP (Model Context Protocol) |
| **Java Version** | 17 |
| **Build Tool** | Maven |

## 📋 Available MCP Tools

The server exposes the following tools for AI assistants:

| Tool Name | Description |
|-----------|-------------|
| `search_notes` | Search notes by filename (case-insensitive partial match) |
| `list_notes` | List all available notes in the notes directory |
| `read_note` | Read and return the full content of a note |
| `create_note` | Create a new note file with content |
| `delete_note` | Delete an existing note file |
| `append_to_note` | Append content to an existing note |

## 🏗️ Project Structure

```
notes-taker-mcp-server-app/
├── src/
│   ├── main/
│   │   ├── java/com/mcp_server/notes/
│   │   │   ├── NotesApplication.java       # Main application entry point
│   │   │   └── service/
│   │   │       ├── NotesService.java       # Service interface
│   │   │       └── impl/
│   │   │           └── NotesServiceImpl.java   # MCP tool implementations
│   │   └── resources/
│   │       └── application.properties      # Configuration
│   └── test/
│       └── java/com/mcp_server/notes/
│           ├── NotesApplicationTests.java  # Context loading test
│           └── service/impl/
│               └── NotesServiceImplTest.java # Unit tests
├── pom.xml
└── README.md
```

## 🔒 Security Features

- **Path Traversal Protection**: Validates all file paths stay within the notes directory
- **Input Sanitization**: Filenames are sanitized to only allow alphanumeric characters, hyphens, and underscores
- **File Size Limits**: Maximum note size of 1MB to prevent abuse

## ⚙️ Configuration

Configure the application in `src/main/resources/application.properties`:

```properties
spring.application.name=notes
spring.main.web-application-type=none
spring.ai.mcp.server.name=notes-taker-app
spring.ai.mcp.server.version=0.0.1

# Notes storage path - defaults to ${user.home}/notes
# Override using: NOTES_PATH environment variable or -Dnotes.path=<path>
notes.path=${NOTES_PATH:${user.home}/notes}
```

### Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `notes.path` | `${user.home}/notes` | Directory where notes are stored |
| `NOTES_PATH` (env var) | - | Environment variable to override notes path |

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Build

```bash
./mvnw clean package
```

### Run Tests

```bash
./mvnw test
```

### Run the Server

```bash
java -jar target/notes-taker-app.jar
```

Or with a custom notes path:

```bash
NOTES_PATH=/path/to/notes java -jar target/notes-taker-app.jar
```

The server will start and listen for MCP STDIO communication.

## 🔗 Integration with AI Clients

### Claude Desktop Configuration

Add to your Claude Desktop config file (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "notes-taker": {
      "command": "java",
      "args": ["-jar", "/path/to/notes-taker-app.jar"],
      "env": {
        "NOTES_PATH": "/path/to/your/notes"
      }
    }
  }
}
```

### VS Code / Gemini Configuration

For VS Code with Gemini, configure in your MCP settings:

```json
{
  "servers": {
    "notes-taker": {
      "command": "java",
      "args": ["-jar", "/path/to/notes-taker-app.jar"],
      "env": {
        "NOTES_PATH": "/path/to/your/notes"
      }
    }
  }
}
```

## 📖 Tool Usage Examples

Once connected, AI assistants can use commands like:

- **"List all my notes"** → Uses `list_notes`
- **"Create a note called 'meeting-notes' with today's discussion"** → Uses `create_note`
- **"Read the content of my project-ideas note"** → Uses `read_note`
- **"Search for notes about project"** → Uses `search_notes`  
- **"Add an action item to my meeting-notes"** → Uses `append_to_note`
- **"Delete the old-draft note"** → Uses `delete_note`

## 📄 License

This project is open source.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.
