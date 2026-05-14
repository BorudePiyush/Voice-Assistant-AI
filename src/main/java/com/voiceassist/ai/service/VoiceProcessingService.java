package com.voiceassist.ai.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.voiceassist.ai.model.VoiceMessage;
import com.voiceassist.ai.model.VoiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Voice Processing Service
 * Handles speech recognition, NLP, and voice synthesis.
 */
@Service
public class VoiceProcessingService {

    private static final Pattern WEATHER_QUERY_PATTERN = Pattern.compile("weather(?:\\s+in\\s+([a-zA-Z\\s'-]+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern CITY_AFTER_IN_PATTERN = Pattern.compile("(?:in|for)\\s+([a-zA-Z\\s'-]+)", Pattern.CASE_INSENSITIVE);
    private static final String OPEN_METEO_GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String OPEN_METEO_FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String DEFAULT_SYSTEM_PROMPT = "You are Voice Assist AI, a concise helpful voice assistant. Answer clearly and directly. "
            + "Use short paragraphs. If the user asks for a command you cannot execute, explain the limitation and suggest a next step.";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private static final Logger logger = LoggerFactory.getLogger(VoiceProcessingService.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String defaultWeatherLocation;
    private final String aiProvider;
    private final String geminiApiKey;
    private final String geminiModel;
    private final String aiSystemPrompt;

    public VoiceProcessingService(
            @Value("${voiceassist.weather.default-location:London}") String defaultWeatherLocation,
            @Value("${voiceassist.ai.provider:gemini}") String aiProvider,
            @Value("${voiceassist.ai.gemini.api-key:}") String geminiApiKey,
            @Value("${voiceassist.ai.gemini.model:gemini-1.5-flash}") String geminiModel,
            @Value("${voiceassist.ai.system-prompt:}") String aiSystemPrompt) {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
        this.defaultWeatherLocation = defaultWeatherLocation;
        this.aiProvider = aiProvider;
        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;
        this.aiSystemPrompt = aiSystemPrompt;
    }

    /**
     * Process incoming voice message.
     * Handles audio recognition, NLP processing, and response synthesis.
     *
     * @param voiceMessage The incoming voice message
     * @return JSON string response
     */
    public String processVoice(VoiceMessage voiceMessage) {
        try {
            String messageType = voiceMessage.getType();

            String response = switch (messageType) {
                case "audio" -> processAudio(voiceMessage);
                case "text" -> processText(voiceMessage);
                case "command" -> processCommand(voiceMessage);
                default -> createErrorResponse("Unknown message type: " + messageType);
            };

            return response;
        } catch (Exception e) {
            logger.error("Error processing voice message", e);
            return createErrorResponse("Internal server error");
        }
    }

    /**
     * Process audio input (speech recognition).
     */
    private String processAudio(VoiceMessage voiceMessage) {
        try {
            logger.info("Processing audio with language: {}", voiceMessage.getLanguage());

            String recognizedText = decodeBase64Content(voiceMessage.getContent());
            AssistantReply assistantReply = generateReply(recognizedText, voiceMessage.getLanguage());

            VoiceResponse response = VoiceResponse.builder()
                    .status("success")
                    .message(assistantReply.message())
                    .data(assistantReply.data())
                    .timestamp(System.currentTimeMillis())
                    .build();

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error processing audio", e);
            return createErrorResponse("Failed to process audio");
        }
    }

    /**
     * Process text input (NLP).
     */
    private String processText(VoiceMessage voiceMessage) {
        try {
            logger.info("Processing text: {}", voiceMessage.getContent());

            AssistantReply assistantReply = generateReply(voiceMessage.getContent(), voiceMessage.getLanguage());

            VoiceResponse response = VoiceResponse.builder()
                    .status("success")
                    .message(assistantReply.message())
                    .data(assistantReply.data())
                    .timestamp(System.currentTimeMillis())
                    .build();

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error processing text", e);
            return createErrorResponse("Failed to process text");
        }
    }

    /**
     * Process voice command.
     */
    private String processCommand(VoiceMessage voiceMessage) {
        try {
            String command = voiceMessage.getContent();
            logger.info("Processing command: {}", command);

            AssistantReply assistantReply = generateReply(command, voiceMessage.getLanguage());

            VoiceResponse response = VoiceResponse.builder()
                    .status("success")
                    .message(assistantReply.message())
                    .data(assistantReply.data())
                    .timestamp(System.currentTimeMillis())
                    .build();

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error processing command", e);
            return createErrorResponse("Failed to process command");
        }
    }

    /**
     * Create error response.
     */
    private String createErrorResponse(String errorMessage) {
        try {
            VoiceResponse response = VoiceResponse.builder()
                    .status("error")
                    .message(errorMessage)
                    .timestamp(System.currentTimeMillis())
                    .build();

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error creating error response", e);
            return "{\"status\": \"error\", \"message\": \"Internal server error\"}";
        }
    }

    private AssistantReply generateReply(String input, String language) {
        String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return AssistantReply.text("I did not catch that. Try saying hello, asking about the weather, or giving me a command.", extractIntentLabel(input));
        }

        if (normalized.contains("hello") || normalized.contains("hi") || normalized.contains("hey")) {
            return AssistantReply.text("Hello. I’m ready to help with questions, reminders, and simple commands.", "greeting");
        }

        if (normalized.contains("weather")) {
            return AssistantReply.text(fetchWeatherReply(input), "weather-query");
        }

        if (normalized.contains("time")) {
            return AssistantReply.text("The current time is available from your system clock. I can add a live time response if you want.", "time-query");
        }

        if (normalized.contains("light")) {
            return AssistantReply.text("The lights command was received. Device control can be connected next if you want real automation.", "device-command");
        }

        if (normalized.contains("task") || normalized.contains("todo") || normalized.contains("reminder")) {
            return AssistantReply.text("I can help organize tasks. Tell me what you want to add, and I will format it for you.", "task-management");
        }

        if (isOpenYoutubeCommand(normalized)) {
            return AssistantReply.action(
                    "Opening YouTube.",
                    "open-url",
                    Map.of("openUrl", "https://www.youtube.com", "label", "YouTube")
            );
        }

        if (isOpenSpotifyCommand(normalized)) {
            return AssistantReply.action(
                    "Opening Spotify.",
                    "open-url",
                    Map.of("openUrl", "https://open.spotify.com", "label", "Spotify")
            );
        }

        if (normalized.contains("help") || normalized.contains("what can you do") || normalized.contains("who are you")) {
            return AssistantReply.text(
                    "I can answer common questions, check weather, show time, and open YouTube or Spotify. Try commands like 'open youtube' or 'weather in Tokyo'.",
                    "assistant-help"
            );
        }

        return fetchAiReply(input, language);
    }

    private AssistantReply fetchAiReply(String input, String language) {
        if (aiProvider == null || aiProvider.isBlank() || !"gemini".equalsIgnoreCase(aiProvider)) {
            String readableLanguage = language == null || language.isBlank() ? "English" : language.toUpperCase(Locale.ROOT);
            String fallback = "I heard: \"" + input.trim() + "\". I am responding in " + readableLanguage +
                    " and can help with weather, tasks, commands, or setup guidance.";
            return AssistantReply.text(fallback, extractIntentLabel(input));
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            String readableLanguage = language == null || language.isBlank() ? "English" : language.toUpperCase(Locale.ROOT);
            String fallback = "I heard: \"" + input.trim() + "\". I am responding in " + readableLanguage +
                " and can help with weather, tasks, commands, or setup guidance.";
            return AssistantReply.text(fallback, extractIntentLabel(input));
        }

        try {
            String systemPrompt = (aiSystemPrompt == null || aiSystemPrompt.isBlank()) ? DEFAULT_SYSTEM_PROMPT : aiSystemPrompt;

            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("contents", new Object[] {
                Map.of("role", "user", "parts", new Object[] {
                    Map.of("text", systemPrompt + "\n\nUser: " + input)
                })
            });
            requestPayload.put("generationConfig", Map.of("temperature", 0.7, "maxOutputTokens", 512));

            String requestBody = objectMapper.writeValueAsString(requestPayload);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_BASE_URL + "/models/" + URLEncoder.encode(geminiModel, StandardCharsets.UTF_8) + ":generateContent?key=" + URLEncoder.encode(geminiApiKey, StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("AI API request failed with status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            String content = contentNode.asText("").trim();

            if (content.isBlank()) {
                throw new IOException("AI API returned an empty message");
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("intent", "ai-response");
            data.put("provider", "external-ai");
            data.put("providerName", "gemini");
            data.put("model", geminiModel);
            return new AssistantReply(content, data);
        } catch (Exception e) {
            logger.error("Failed to fetch AI reply", e);
            String fallback = "I could not reach the AI service right now. Please try again in a moment.";
            return AssistantReply.text(fallback, extractIntentLabel(input));
        }
    }

    private String fetchWeatherReply(String input) {
        try {
            String city = extractWeatherLocation(input).orElse(defaultWeatherLocation);
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);

            String geocodingUrl = OPEN_METEO_GEOCODING_URL + "?name=" + encodedCity + "&count=1&language=en&format=json";
            JsonNode geocodingResponse = fetchJson(geocodingUrl);

            JsonNode result = geocodingResponse.path("results").path(0);
            if (result.isMissingNode()) {
                return "I could not find weather data for " + city + ". Try another city name.";
            }

            double latitude = result.path("latitude").asDouble();
            double longitude = result.path("longitude").asDouble();
            String resolvedCity = result.path("name").asText(city);
            String country = result.path("country").asText("");

            String forecastUrl = OPEN_METEO_FORECAST_URL
                    + "?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,relative_humidity_2m"
                    + "&timezone=auto";

            JsonNode forecastResponse = fetchJson(forecastUrl);
            JsonNode current = forecastResponse.path("current");

            if (current.isMissingNode()) {
                return "I found " + resolvedCity + (country.isBlank() ? "" : ", " + country) + ", but current weather data was unavailable.";
            }

            double temperature = current.path("temperature_2m").asDouble();
            double feelsLike = current.path("apparent_temperature").asDouble(temperature);
            double windSpeed = current.path("wind_speed_10m").asDouble(0.0);
            int humidity = current.path("relative_humidity_2m").asInt(0);
            int weatherCode = current.path("weather_code").asInt(-1);
            String description = weatherCodeDescription(weatherCode);

            String locationText = resolvedCity + (country.isBlank() ? "" : ", " + country);
            return "The current weather in " + locationText + " is " + description + ", "
                    + Math.round(temperature) + " degrees Celsius"
                    + " (feels like " + Math.round(feelsLike) + ")"
                    + ", humidity " + humidity + " percent"
                    + ", wind " + Math.round(windSpeed) + " kilometers per hour.";
        } catch (Exception e) {
            logger.error("Failed to fetch live weather", e);
            return "I could not reach the weather service right now. Please try again in a moment.";
        }
    }

    private Optional<String> extractWeatherLocation(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = WEATHER_QUERY_PATTERN.matcher(input.trim());
        if (matcher.find() && matcher.group(1) != null && !matcher.group(1).isBlank()) {
            return Optional.of(matcher.group(1).trim());
        }

        matcher = CITY_AFTER_IN_PATTERN.matcher(input.trim());
        if (matcher.find() && matcher.group(1) != null && !matcher.group(1).isBlank()) {
            return Optional.of(matcher.group(1).trim());
        }

        return Optional.empty();
    }

    private JsonNode fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Weather API request failed with status " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private String weatherCodeDescription(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> "clear skies";
            case 1, 2 -> "partly cloudy";
            case 3 -> "overcast";
            case 45, 48 -> "foggy";
            case 51, 53, 55 -> "light drizzle";
            case 61, 63, 65 -> "rainy";
            case 66, 67 -> "freezing rain";
            case 71, 73, 75 -> "snowy";
            case 77 -> "snow grains";
            case 80, 81, 82 -> "showers";
            case 85, 86 -> "snow showers";
            case 95 -> "thunderstorms";
            case 96, 99 -> "thunderstorms with hail";
            default -> "variable conditions";
        };
    }

    private String extractIntentLabel(String input) {
        String normalized = input == null ? "" : input.trim().toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return "empty-input";
        }
        if (normalized.contains("hello") || normalized.contains("hi") || normalized.contains("hey")) {
            return "greeting";
        }
        if (normalized.contains("weather")) {
            return "weather-query";
        }
        if (normalized.contains("time")) {
            return "time-query";
        }
        if (normalized.contains("light")) {
            return "device-command";
        }
        if (normalized.contains("task") || normalized.contains("todo") || normalized.contains("reminder")) {
            return "task-management";
        }
        if (normalized.contains("youtube") || normalized.contains("spotify")) {
            return "media-launch";
        }
        return "general-conversation";
    }

    private boolean isOpenYoutubeCommand(String normalized) {
        return normalized.contains("open youtube") || normalized.equals("youtube") || normalized.contains("go to youtube");
    }

    private boolean isOpenSpotifyCommand(String normalized) {
        return normalized.contains("open spotify") || normalized.equals("spotify") || normalized.contains("go to spotify");
    }

    private String decodeBase64Content(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(content);
            return new String(decoded, StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException ex) {
            return content.trim();
        }
    }

    private record AssistantReply(String message, Object data) {
        private static AssistantReply text(String message, String intent) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("intent", intent);
            return new AssistantReply(message, payload);
        }

        private static AssistantReply action(String message, String intent, Map<String, Object> actionData) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("intent", intent);
            payload.putAll(actionData);
            return new AssistantReply(message, payload);
        }
    }
}
