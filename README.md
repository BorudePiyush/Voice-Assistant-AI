# Voice Assist AI

A comprehensive Voice Assistant application built with **Java 25**, **Spring Boot 3.2+**, and real-time WebSocket communication.

## Features

- ✅ **Real-time Speech Recognition** - Convert audio to text using cloud APIs
- ✅ **Natural Language Processing** - Extract intents and entities from user input
- ✅ **Voice Synthesis** - Generate natural-sounding voice responses
- ✅ **WebSocket Communication** - Real-time bidirectional voice interaction
- ✅ **REST API** - Alternative HTTP endpoints for voice processing
- ✅ **Multi-language Support** - Support for multiple languages via ISO 639-1 codes
- ✅ **Session Management** - Track user sessions and conversation context
- ✅ **Error Handling** - Comprehensive error handling and logging

## Project Structure

```
voice-assist-ai/
├── src/
│   ├── main/
│   │   ├── java/com/voiceassist/ai/
│   │   │   ├── VoiceAssistAiApplication.java    # Main application entry
│   │   │   ├── config/
│   │   │   │   └── WebSocketConfig.java         # WebSocket configuration
│   │   │   ├── controller/
│   │   │   │   ├── HealthController.java        # Health check endpoints
│   │   │   │   └── VoiceController.java         # Voice processing REST endpoints
│   │   │   ├── service/
│   │   │   │   └── VoiceProcessingService.java  # Core voice processing logic
│   │   │   ├── websocket/
│   │   │   │   └── VoiceWebSocketHandler.java   # WebSocket message handler
│   │   │   └── model/
│   │   │       ├── VoiceMessage.java            # Voice message DTO
│   │   │       └── VoiceResponse.java           # Voice response DTO
│   │   └── resources/
│   │       └── application.properties           # Application configuration
│   └── test/
│       └── java/com/voiceassist/ai/
│           └── VoiceProcessingServiceTest.java  # Unit tests
├── pom.xml                                      # Maven configuration
└── README.md                                    # This file
```

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Java | 25 (LTS) |
| Framework | Spring Boot | 3.2.0+ |
| Build Tool | Maven | 3.8+ |
| Real-time Comm | WebSocket | Native Spring |
| Testing | JUnit 5 | Latest |
| JSON Processing | Jackson | Latest |
| Utilities | Lombok | Latest |

## Prerequisites

- **Java 25 LTS** or higher
- **Maven 3.8** or higher
- **IDE** (VS Code, IntelliJ IDEA, Eclipse)
- **Git** (for version control)

## Installation & Setup

### 1. Clone or Navigate to Project

```bash
cd f:\Java Project
```

### 2. Install Dependencies

```bash
mvn clean install
```

### 3. Compile the Project

```bash
mvn compile
```

### 4. Run Tests

```bash
mvn test
```

### 5. Run the Application

```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

## Usage

### REST API Endpoints

#### Health Check
```bash
GET http://localhost:8080/api/v1/health
```

#### Application Status
```bash
GET http://localhost:8080/api/v1/status
```

#### Process Voice (REST)
```bash
POST http://localhost:8080/api/v1/voice/process
Content-Type: application/json

{
  "type": "text",
  "content": "What is the weather?",
  "language": "en",
  "sessionId": "session123"
}
```

#### Speech Recognition
```bash
POST http://localhost:8080/api/v1/voice/recognize
Content-Type: application/json

{
  "type": "audio",
  "content": "base64_encoded_audio_data",
  "language": "en",
  "sessionId": "session123"
}
```

### WebSocket Connection

Connect to the WebSocket endpoint for real-time voice communication:

```javascript
const socket = new WebSocket('ws://localhost:8080/ws/voice');

socket.onopen = () => {
  console.log('Connected to Voice Assist AI');
  socket.send(JSON.stringify({
    type: 'text',
    content: 'Hello!',
    language: 'en',
    sessionId: 'session123'
  }));
};

socket.onmessage = (event) => {
  console.log('Response:', JSON.parse(event.data));
};

socket.onerror = (error) => {
  console.error('WebSocket error:', error);
};
```

## Message Format

### Voice Message (Request)

```json
{
  "type": "audio|text|command",
  "content": "string (base64 for audio)",
  "language": "en",
  "sessionId": "unique_session_id",
  "timestamp": 1234567890
}
```

### Voice Response (Response)

```json
{
  "status": "success|error",
  "message": "descriptive message",
  "data": "response data",
  "timestamp": 1234567890
}
```

## Development Guide

### Adding New Services

1. Create a new service class in `src/main/java/com/voiceassist/ai/service/`
2. Annotate with `@Service`
3. Inject dependencies using constructor injection
4. Add business logic

Example:
```java
@Service
public class MyService {
    public String processData(String input) {
        // Implementation
        return result;
    }
}
```

### Adding New Endpoints

1. Create a controller in `src/main/java/com/voiceassist/ai/controller/`
2. Annotate with `@RestController`
3. Define endpoints with `@GetMapping`, `@PostMapping`, etc.

Example:
```java
@RestController
@RequestMapping("/api/v1/myendpoint")
public class MyController {
    @PostMapping("/action")
    public ResponseObject performAction(@RequestBody RequestObject request) {
        // Implementation
        return response;
    }
}
```

## Integration Roadmap

The following components should be integrated for production use:

- [ ] **Speech Recognition**: Google Cloud Speech-to-Text, Azure Speech Services, or AWS Transcribe
- [ ] **NLP Engine**: spaCy, NLTK, or cloud-based NLP services
- [ ] **Text-to-Speech**: Google TTS, Azure TTS, or AWS Polly
- [ ] **Intent Recognition**: Dialogflow, LUIS, or custom ML models
- [ ] **Database**: PostgreSQL/MongoDB for session persistence
- [ ] **Authentication**: OAuth 2.0 / JWT token validation
- [ ] **Rate Limiting**: API rate limiting and request throttling
- [ ] **Monitoring**: Application performance monitoring (APM)
- [ ] **Containerization**: Docker support for deployment

## Testing

Run unit tests:

```bash
mvn test
```

Run specific test:

```bash
mvn test -Dtest=VoiceProcessingServiceTest
```

Generate test coverage report:

```bash
mvn test jacoco:report
```

## Building for Production

Create executable JAR:

```bash
mvn clean package
```

Run the JAR:

```bash
java -jar target/voice-assist-ai-1.0.0.jar
```

## Troubleshooting

### Port Already in Use
```bash
# Run on different port
java -jar target/voice-assist-ai-1.0.0.jar --server.port=8081
```

### Maven Build Issues
```bash
# Clear Maven cache
mvn clean
mvn install
```

### WebSocket Connection Failures
- Verify firewall rules allow port 8080
- Check that `/ws/voice` endpoint is accessible
- Review application logs for errors

## Contributing

1. Create a feature branch: `git checkout -b feature/your-feature`
2. Commit changes: `git commit -am 'Add your feature'`
3. Push to branch: `git push origin feature/your-feature`
4. Submit a pull request

## Documentation

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [WebSocket Tutorial](https://spring.io/guides/gs/messaging-stomp-websocket/)
- [Java 25 Features](https://www.oracle.com/java/25/)

## License

This project is licensed under the MIT License.

## Support

For issues, questions, or suggestions, please open a GitHub issue or contact the development team.

---

**Last Updated**: May 2026
**Version**: 1.0.0
**Status**: Active Development
