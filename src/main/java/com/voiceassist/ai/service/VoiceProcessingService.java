package com.voiceassist.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.voiceassist.ai.model.AiPersona;
import com.voiceassist.ai.model.ChatMessage;
import com.voiceassist.ai.model.ChatRequest;
import com.voiceassist.ai.model.VoiceMessage;
import com.voiceassist.ai.model.VoiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Voice & Chat Processing Service
 * Intelligent real-time conversational engine with:
 * - Real-time Global Live Weather for ANY city/region (Open-Meteo)
 * - Real-time Live Knowledge & Encyclopedia (Wikipedia & DuckDuckGo APIs)
 * - Generative AI integration (Google Gemini 1.5 Flash/Pro with env/key support)
 * - Built-in Knowledge Base for science, coding, geography, history, and math
 * - Math expression evaluator & multi-unit converter
 * - Multi-turn conversational memory & personalized AI personas (Jarvis, DevBot, Nova, Newton, Sage)
 * - Task manager NLP & Application launcher shortcuts
 */
@Service
public class VoiceProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(VoiceProcessingService.class);

    private static final Pattern CALC_PATTERN = Pattern.compile("^(?:calc|calculate|what is|compute|solve)?\\s*([0-9+\\-*/().^%\\s]+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONVERT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(km|miles|kg|lbs|celsius|fahrenheit|cm|inches|meters|feet|liters|gallons|grams|ounces)\\s+(?:to|in|into)\\s+(km|miles|kg|lbs|celsius|fahrenheit|cm|inches|meters|feet|liters|gallons|grams|ounces)", Pattern.CASE_INSENSITIVE);

    private static final String OPEN_METEO_GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String OPEN_METEO_FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String WIKIPEDIA_SEARCH_URL = "https://en.wikipedia.org/w/api.php";
    private static final String WIKIPEDIA_SUMMARY_URL = "https://en.wikipedia.org/api/rest_v1/page/summary/";
    private static final String DUCKDUCKGO_API_URL = "https://api.duckduckgo.com/";
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String defaultWeatherLocation;
    private final String defaultAiProvider;
    private final String defaultGeminiApiKey;
    private final String defaultGeminiModel;
    private final String defaultSystemPrompt;

    private final ChatSessionService chatSessionService;
    private final TaskManagerService taskManagerService;
    private final Map<String, AiPersona> personas = new LinkedHashMap<>();

    public VoiceProcessingService(
            @Value("${voiceassist.weather.default-location:London}") String defaultWeatherLocation,
            @Value("${voiceassist.ai.provider:gemini}") String defaultAiProvider,
            @Value("${voiceassist.ai.gemini.api-key:}") String defaultGeminiApiKey,
            @Value("${voiceassist.ai.gemini.model:gemini-1.5-flash}") String defaultGeminiModel,
            @Value("${voiceassist.ai.system-prompt:}") String defaultSystemPrompt,
            ChatSessionService chatSessionService,
            TaskManagerService taskManagerService) {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
        this.defaultWeatherLocation = (defaultWeatherLocation == null || defaultWeatherLocation.isBlank()) ? "London" : defaultWeatherLocation;
        this.defaultAiProvider = defaultAiProvider;
        this.defaultGeminiApiKey = defaultGeminiApiKey;
        this.defaultGeminiModel = (defaultGeminiModel == null || defaultGeminiModel.isBlank()) ? "gemini-1.5-flash" : defaultGeminiModel;
        this.defaultSystemPrompt = (defaultSystemPrompt == null || defaultSystemPrompt.isBlank())
                ? "You are Voice Assist AI, an intelligent, helpful, and concise conversational assistant."
                : defaultSystemPrompt;
        this.chatSessionService = chatSessionService;
        this.taskManagerService = taskManagerService;

        initializePersonas();
    }

    private void initializePersonas() {
        personas.put("jarvis", AiPersona.builder()
                .id("jarvis")
                .name("Jarvis AI")
                .description("Smart, concise, highly capable voice & text assistant.")
                .systemPrompt("You are Jarvis, an advanced AI voice assistant. Respond with crisp, accurate, and insightful answers. Use short paragraphs and highlight key points.")
                .avatar("fas fa-robot")
                .color("#6366f1")
                .tag("General")
                .build());

        personas.put("devbot", AiPersona.builder()
                .id("devbot")
                .name("DevBot Pro")
                .description("Expert Senior Software Engineer specializing in Java, Spring Boot, and Full-Stack.")
                .systemPrompt("You are DevBot Pro, a senior principal software engineer. Provide high-quality, production-ready code examples with clean formatting, comments, and modern design patterns.")
                .avatar("fas fa-code")
                .color("#10b981")
                .tag("Engineering")
                .build());

        personas.put("nova", AiPersona.builder()
                .id("nova")
                .name("Nova Creative")
                .description("Expressive, imaginative, storytelling & brainstorming genius.")
                .systemPrompt("You are Nova, an expressive, highly creative companion. Help users brainstorm innovative concepts, write compelling content, and spark fresh ideas.")
                .avatar("fas fa-wand-magic-sparkles")
                .color("#ec4899")
                .tag("Creative")
                .build());

        personas.put("newton", AiPersona.builder()
                .id("newton")
                .name("Newton Math")
                .description("Precise problem solver for math, physics, algorithms, and logic.")
                .systemPrompt("You are Newton, a logical reasoning and mathematics expert. Break down complex calculations step-by-step with clarity.")
                .avatar("fas fa-square-root-variable")
                .color("#f59e0b")
                .tag("Science & Math")
                .build());

        personas.put("sage", AiPersona.builder()
                .id("sage")
                .name("Sage Coach")
                .description("Productivity, mindfulness, habit building, and daily planning mentor.")
                .systemPrompt("You are Sage, a motivational productivity and life coach. Give supportive, structured, actionable advice to help users accomplish their goals.")
                .avatar("fas fa-lightbulb")
                .color("#06b6d4")
                .tag("Productivity")
                .build());
    }

    public List<AiPersona> getAvailablePersonas() {
        return new ArrayList<>(personas.values());
    }

    public AiPersona getPersona(String personaId) {
        if (personaId == null) return personas.get("jarvis");
        return personas.getOrDefault(personaId.toLowerCase(Locale.ROOT), personas.get("jarvis"));
    }

    /**
     * Process REST Chat Request with session and persona context.
     */
    public VoiceResponse processChat(ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
            request.setSessionId(sessionId);
        }

        String input = request.getMessage();
        String persona = request.getPersona() != null ? request.getPersona() : "jarvis";

        // Record User Message
        ChatMessage userMessage = ChatMessage.builder()
                .id("msg-" + UUID.randomUUID().toString().substring(0, 8))
                .sessionId(sessionId)
                .role("user")
                .content(input)
                .persona(persona)
                .timestamp(System.currentTimeMillis())
                .build();
        chatSessionService.addMessage(sessionId, userMessage);

        // Generate response
        AssistantReply reply = generateSmartReply(input, request.getLanguage(), sessionId, persona, request.getApiKey(), request.getModel());

        // Record Assistant Message
        ChatMessage assistantMessage = ChatMessage.builder()
                .id("msg-" + UUID.randomUUID().toString().substring(0, 8))
                .sessionId(sessionId)
                .role("assistant")
                .content(reply.message())
                .intent(reply.intent())
                .persona(persona)
                .timestamp(System.currentTimeMillis())
                .data(reply.data())
                .build();
        chatSessionService.addMessage(sessionId, assistantMessage);

        Map<String, Object> enrichedData = new LinkedHashMap<>();
        enrichedData.put("sessionId", sessionId);
        enrichedData.put("persona", persona);
        enrichedData.put("intent", reply.intent());
        if (reply.data() != null) {
            enrichedData.putAll(reply.data());
        }

        return VoiceResponse.builder()
                .status("success")
                .message(reply.message())
                .data(enrichedData)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * Legacy / WebSocket voice process entry point.
     */
    public String processVoice(VoiceMessage voiceMessage) {
        try {
            String sessionId = voiceMessage.getSessionId();
            if (sessionId == null || sessionId.isBlank()) {
                sessionId = "default-session";
                voiceMessage.setSessionId(sessionId);
            }

            String content = voiceMessage.getContent();
            if ("audio".equalsIgnoreCase(voiceMessage.getType())) {
                content = decodeBase64Content(content);
            }

            ChatRequest chatReq = ChatRequest.builder()
                    .sessionId(sessionId)
                    .message(content)
                    .language(voiceMessage.getLanguage())
                    .build();

            VoiceResponse response = processChat(chatReq);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            logger.error("Error processing voice message", e);
            return createErrorResponse("Internal server error: " + e.getMessage());
        }
    }

    /**
     * Main NLP Router & Smart Reply Generator.
     */
    private AssistantReply generateSmartReply(
            String input,
            String language,
            String sessionId,
            String personaKey,
            String customApiKey,
            String customModel) {

        String text = input == null ? "" : input.trim();
        String normalized = text.toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return AssistantReply.text("I'm listening! You can ask me questions about any topic, check real-time weather anywhere in the world, calculate math, manage tasks, or speak a command.", "empty-input");
        }

        // 1. Check Task / Todo NLP
        TaskManagerService.TaskActionResult taskResult = taskManagerService.handleNaturalLanguage(text, sessionId);
        if (taskResult != null) {
            return new AssistantReply(taskResult.replyText(), taskResult.intent(), taskResult.data());
        }

        // 2. Greetings
        if (isGreeting(normalized)) {
            AiPersona persona = getPersona(personaKey);
            String greeting = "Hello! I'm " + persona.getName() + ". I'm ready to answer any questions, look up real-time weather worldwide, solve problems, or assist you with coding and tasks. How can I help you today?";
            return AssistantReply.text(greeting, "greeting");
        }

        // 3. Real-Time Weather Query for ANY City/Region
        if (isWeatherQuery(normalized)) {
            return fetchWeatherReply(text);
        }

        // 4. Live Time / Date / World Timezone Query
        if (isTimeQuery(normalized)) {
            return handleTimeQuery(text, normalized);
        }

        // 5. Unit Conversion
        AssistantReply conversionReply = handleUnitConversion(text);
        if (conversionReply != null) {
            return conversionReply;
        }

        // 6. Math & Calculator Calculation
        AssistantReply mathReply = handleMathCalculation(text);
        if (mathReply != null) {
            return mathReply;
        }

        // 7. System / Quick Web Actions
        AssistantReply actionReply = handleActionCommands(normalized);
        if (actionReply != null) {
            return actionReply;
        }

        // 8. Jokes / Trivia / Motivation
        AssistantReply funReply = handleFunAndQuotes(normalized);
        if (funReply != null) {
            return funReply;
        }

        // 9. Help & Capabilities
        if (normalized.contains("help") || normalized.contains("what can you do") || normalized.contains("commands") || normalized.contains("features")) {
            String helpText = "Here is what I can do for you:\n\n"
                    + "- 🌦️ **Global Real-Time Weather**: Ask about any city or region (e.g. *\"Weather in Tokyo\"*, *\"Is it raining in Paris?\"*, *\"Mumbai temperature\"*)\n"
                    + "- 🌍 **Answer Any Question**: Live real-time encyclopedic search & reasoning for science, history, people, concepts, and trivia\n"
                    + "- 🎙️ **Voice & Conversational AI**: Full speech recognition and speech synthesis across multiple personas (Jarvis, DevBot, Nova, Newton, Sage)\n"
                    + "- 💻 **Coding & Architecture**: Ask for Java, Spring Boot, Python, SQL, REST APIs, and algorithms\n"
                    + "- 🧮 **Math & Unit Converter**: *\"Calculate (450 * 12) / 8\"*, *\"Convert 100 km to miles\"*\n"
                    + "- 📋 **Task Manager**: *\"Add task: finish the report\"*, *\"Show my tasks\"*, *\"Clear tasks\"*\n"
                    + "- ⏰ **World Clock**: *\"Current time in New York\"*, *\"Time in Tokyo\"*\n"
                    + "- 🚀 **App Shortcuts**: *\"Open YouTube\"*, *\"Open GitHub\"*, *\"Open Spotify\"*";
            return AssistantReply.text(helpText, "assistant-help");
        }

        // 10. Universal Real-Time Knowledge & Generative AI Pipeline
        return fetchUniversalKnowledgeOrAiReply(text, language, sessionId, personaKey, customApiKey, customModel);
    }

    private boolean isGreeting(String normalized) {
        return normalized.equals("hello") || normalized.equals("hi") || normalized.equals("hey")
                || normalized.equals("greetings") || normalized.equals("good morning")
                || normalized.equals("good evening") || normalized.equals("good afternoon")
                || normalized.startsWith("hello ") || normalized.startsWith("hi ") || normalized.startsWith("hey ");
    }

    private boolean isWeatherQuery(String normalized) {
        return normalized.contains("weather")
                || normalized.contains("temperature")
                || normalized.contains("temp in")
                || normalized.contains("temp of")
                || normalized.contains("forecast")
                || normalized.contains("climate")
                || normalized.contains("is it raining")
                || normalized.contains("is it sunny")
                || normalized.contains("is it cold")
                || normalized.contains("is it hot")
                || normalized.contains("rain in")
                || normalized.contains("snow in");
    }

    private boolean isTimeQuery(String normalized) {
        return normalized.contains("what time is it")
                || normalized.equals("time")
                || normalized.contains("current time")
                || normalized.contains("today's date")
                || normalized.contains("what is today's date")
                || normalized.contains("what day is it")
                || (normalized.contains("time in") || normalized.contains("time of") || normalized.contains("time at"));
    }

    private AssistantReply handleTimeQuery(String originalText, String normalized) {
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");

        // Check if query specifies a city/timezone
        String targetCity = extractLocationFromTimeQuery(originalText);
        if (targetCity != null && !targetCity.isBlank()) {
            ZoneId zoneId = findZoneIdForCity(targetCity);
            if (zoneId != null) {
                ZonedDateTime targetTime = ZonedDateTime.now(zoneId);
                String timeStr = targetTime.format(timeFmt);
                String dateStr = targetTime.format(dateFmt);
                String msg = "🕒 The current time in **" + capitalizeWords(targetCity) + "** (" + zoneId.getId() + ") is **" + timeStr + "** on **" + dateStr + "**.";
                return AssistantReply.text(msg, "time-query");
            }
        }

        ZonedDateTime now = ZonedDateTime.now();
        String localTime = now.format(timeFmt);
        String localDate = now.format(dateFmt);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("time", localTime);
        data.put("date", localDate);
        data.put("timezone", ZoneId.systemDefault().toString());

        return new AssistantReply("🕒 The current time is **" + localTime + "** on **" + localDate + "** (" + ZoneId.systemDefault().getId() + ").", "time-query", data);
    }

    private String extractLocationFromTimeQuery(String text) {
        Pattern pattern = Pattern.compile("(?:time\\s+(?:in|for|of|at)|in)\\s+([a-zA-Z\\s'-]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).replaceAll("(?i)\\b(now|today|right now|please|currently)\\b", "").trim();
        }
        return null;
    }

    private ZoneId findZoneIdForCity(String city) {
        String lower = city.toLowerCase(Locale.ROOT).trim();
        return switch (lower) {
            case "new york", "nyc" -> ZoneId.of("America/New_York");
            case "los angeles", "la", "california", "san francisco" -> ZoneId.of("America/Los_Angeles");
            case "chicago" -> ZoneId.of("America/Chicago");
            case "london", "uk" -> ZoneId.of("Europe/London");
            case "paris", "france" -> ZoneId.of("Europe/Paris");
            case "berlin", "germany" -> ZoneId.of("Europe/Berlin");
            case "tokyo", "japan" -> ZoneId.of("Asia/Tokyo");
            case "mumbai", "delhi", "bangalore", "india", "pune", "hyderabad", "kolkata", "chennai" -> ZoneId.of("Asia/Kolkata");
            case "dubai", "uae" -> ZoneId.of("Asia/Dubai");
            case "singapore" -> ZoneId.of("Asia/Singapore");
            case "sydney", "australia", "melbourne" -> ZoneId.of("Australia/Sydney");
            case "toronto", "canada", "vancouver" -> ZoneId.of("America/Toronto");
            case "utc", "gmt" -> ZoneId.of("UTC");
            default -> {
                for (String zone : ZoneId.getAvailableZoneIds()) {
                    if (zone.toLowerCase(Locale.ROOT).contains(lower.replace(" ", "_"))) {
                        yield ZoneId.of(zone);
                    }
                }
                yield null;
            }
        };
    }

    private AssistantReply handleMathCalculation(String text) {
        String clean = text.toLowerCase(Locale.ROOT)
                .replace("calculate", "")
                .replace("what is", "")
                .replace("compute", "")
                .replace("solve", "")
                .replace("calc", "")
                .replace("=", "")
                .trim();

        if (clean.isBlank() || !clean.matches(".*[0-9].*")) {
            return null;
        }

        try {
            if (clean.matches("^[0-9+\\-*/().^%\\s]+$")) {
                double result = evaluateMathExpression(clean);
                String formatted = (result == (long) result) ? String.format("%d", (long) result) : String.format("%.4f", result).replaceAll("0+$", "").replaceAll("\\.$", "");

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("expression", clean);
                data.put("result", formatted);
                data.put("isMath", true);

                String msg = "🧮 **Calculation Result:**\n\n`" + clean + " = " + formatted + "`";
                return new AssistantReply(msg, "math-result", data);
            }
        } catch (Exception ignored) {
            // Not a simple math expression
        }

        return null;
    }

    private AssistantReply handleUnitConversion(String text) {
        Matcher m = CONVERT_PATTERN.matcher(text.trim());
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                String from = m.group(2).toLowerCase(Locale.ROOT);
                String to = m.group(3).toLowerCase(Locale.ROOT);

                double converted = 0;
                String resultStr = "";

                if (from.equals("km") && to.equals("miles")) {
                    converted = val * 0.621371;
                    resultStr = String.format("%.2f km = %.2f miles", val, converted);
                } else if (from.equals("miles") && to.equals("km")) {
                    converted = val * 1.60934;
                    resultStr = String.format("%.2f miles = %.2f km", val, converted);
                } else if (from.equals("kg") && to.equals("lbs")) {
                    converted = val * 2.20462;
                    resultStr = String.format("%.2f kg = %.2f lbs", val, converted);
                } else if (from.equals("lbs") && to.equals("kg")) {
                    converted = val * 0.453592;
                    resultStr = String.format("%.2f lbs = %.2f kg", val, converted);
                } else if (from.equals("celsius") && to.equals("fahrenheit")) {
                    converted = (val * 9.0 / 5.0) + 32.0;
                    resultStr = String.format("%.1f°C = %.1f°F", val, converted);
                } else if (from.equals("fahrenheit") && to.equals("celsius")) {
                    converted = (val - 32.0) * 5.0 / 9.0;
                    resultStr = String.format("%.1f°F = %.1f°C", val, converted);
                } else if (from.equals("cm") && to.equals("inches")) {
                    converted = val * 0.393701;
                    resultStr = String.format("%.2f cm = %.2f inches", val, converted);
                } else if (from.equals("inches") && to.equals("cm")) {
                    converted = val * 2.54;
                    resultStr = String.format("%.2f inches = %.2f cm", val, converted);
                } else if (from.equals("meters") && to.equals("feet")) {
                    converted = val * 3.28084;
                    resultStr = String.format("%.2f meters = %.2f feet", val, converted);
                } else if (from.equals("feet") && to.equals("meters")) {
                    converted = val * 0.3048;
                    resultStr = String.format("%.2f feet = %.2f meters", val, converted);
                } else if (from.equals("liters") && to.equals("gallons")) {
                    converted = val * 0.264172;
                    resultStr = String.format("%.2f liters = %.2f gallons", val, converted);
                } else if (from.equals("gallons") && to.equals("liters")) {
                    converted = val * 3.78541;
                    resultStr = String.format("%.2f gallons = %.2f liters", val, converted);
                } else if (from.equals("grams") && to.equals("ounces")) {
                    converted = val * 0.035274;
                    resultStr = String.format("%.2f grams = %.2f ounces", val, converted);
                } else if (from.equals("ounces") && to.equals("grams")) {
                    converted = val * 28.3495;
                    resultStr = String.format("%.2f ounces = %.2f grams", val, converted);
                }

                if (!resultStr.isEmpty()) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("conversion", resultStr);
                    return new AssistantReply("📐 **Unit Conversion:**\n\n" + resultStr, "unit-conversion", data);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private AssistantReply handleActionCommands(String normalized) {
        if (normalized.contains("open youtube") || normalized.equals("youtube") || normalized.contains("play youtube") || normalized.contains("go to youtube")) {
            return AssistantReply.action("Opening YouTube for you.", "open-url", Map.of("openUrl", "https://www.youtube.com", "label", "YouTube", "icon", "fab fa-youtube"));
        }
        if (normalized.contains("open spotify") || normalized.equals("spotify") || normalized.contains("play music on spotify")) {
            return AssistantReply.action("Opening Spotify for you.", "open-url", Map.of("openUrl", "https://open.spotify.com", "label", "Spotify", "icon", "fab fa-spotify"));
        }
        if (normalized.contains("open github") || normalized.equals("github") || normalized.contains("go to github")) {
            return AssistantReply.action("Opening GitHub for you.", "open-url", Map.of("openUrl", "https://github.com", "label", "GitHub", "icon", "fab fa-github"));
        }
        if (normalized.contains("open google maps") || normalized.contains("google maps") || normalized.contains("open maps")) {
            return AssistantReply.action("Opening Google Maps.", "open-url", Map.of("openUrl", "https://maps.google.com", "label", "Google Maps", "icon", "fas fa-map-location-dot"));
        }
        if (normalized.contains("open wikipedia") || normalized.equals("wikipedia")) {
            return AssistantReply.action("Opening Wikipedia.", "open-url", Map.of("openUrl", "https://www.wikipedia.org", "label", "Wikipedia", "icon", "fab fa-wikipedia-w"));
        }
        if (normalized.contains("open stack overflow") || normalized.contains("stackoverflow")) {
            return AssistantReply.action("Opening Stack Overflow.", "open-url", Map.of("openUrl", "https://stackoverflow.com", "label", "Stack Overflow", "icon", "fab fa-stack-overflow"));
        }
        return null;
    }

    private AssistantReply handleFunAndQuotes(String normalized) {
        if (normalized.contains("joke") || normalized.contains("funny") || normalized.contains("make me laugh")) {
            String[] jokes = {
                    "Why do Java programmers have to wear glasses? Because they don't C#!",
                    "There are 10 types of people in the world: those who understand binary, and those who don't.",
                    "A SQL query walks into a bar, walks up to two tables and asks: 'Can I join you?'",
                    "Why was the JavaScript developer sad? Because they didn't Node how to Express themselves!",
                    "Debugging: Removing the needles from the haystack, only to realize the haystack is also made of needles.",
                    "Why do programmers prefer dark mode? Because light attracts bugs!"
            };
            String joke = jokes[new Random().nextInt(jokes.length)];
            return AssistantReply.text("😄 " + joke, "joke");
        }

        if (normalized.contains("quote") || normalized.contains("inspire me") || normalized.contains("motivation")) {
            String[] quotes = {
                    "\"The secret of getting ahead is getting started.\" - Mark Twain",
                    "\"Simplicity is prerequisite for reliability.\" - Edsger W. Dijkstra",
                    "\"It always seems impossible until it's done.\" - Nelson Mandela",
                    "\"Code is like humor. When you have to explain it, it's bad.\" - Cory House",
                    "\"Any fool can write code that a computer can understand. Good programmers write code that humans can understand.\" - Martin Fowler",
                    "\"The only way to do great work is to love what you do.\" - Steve Jobs"
            };
            String quote = quotes[new Random().nextInt(quotes.length)];
            return AssistantReply.text("✨ " + quote, "quote");
        }

        return null;
    }

    /**
     * Real-Time Global Weather Engine for ANY City / Region worldwide using Open-Meteo.
     */
    private AssistantReply fetchWeatherReply(String input) {
        try {
            String targetLocation = extractWeatherLocation(input);
            String encodedCity = URLEncoder.encode(targetLocation, StandardCharsets.UTF_8);

            // 1. Geocoding Query (Resolves any city, district, province, state, or country)
            String geocodingUrl = OPEN_METEO_GEOCODING_URL + "?name=" + encodedCity + "&count=5&language=en&format=json";
            JsonNode geocodingResponse = fetchJson(geocodingUrl);

            JsonNode resultsArray = geocodingResponse.path("results");
            if (resultsArray.isMissingNode() || !resultsArray.isArray() || resultsArray.isEmpty()) {
                return AssistantReply.text("I could not locate weather coordinates for **\"" + targetLocation + "\"**. Please verify the spelling or try adding a country/state (e.g. *\"Paris, France\"* or *\"Austin, Texas\"*).", "weather-query");
            }

            JsonNode topResult = resultsArray.get(0);
            double latitude = topResult.path("latitude").asDouble();
            double longitude = topResult.path("longitude").asDouble();
            String resolvedCity = topResult.path("name").asText(targetLocation);
            String admin1 = topResult.path("admin1").asText("");
            String country = topResult.path("country").asText("");

            // 2. High-Precision Forecast Query
            String forecastUrl = OPEN_METEO_FORECAST_URL
                    + "?latitude=" + latitude
                    + "&longitude=" + longitude
                    + "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m,relative_humidity_2m,surface_pressure,cloud_cover,is_day"
                    + "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max"
                    + "&timezone=auto";

            JsonNode forecastResponse = fetchJson(forecastUrl);
            JsonNode current = forecastResponse.path("current");

            if (current.isMissingNode()) {
                return AssistantReply.text("Weather data for **" + resolvedCity + "** is currently unavailable from the sensor network. Please try again shortly.", "weather-query");
            }

            double tempC = current.path("temperature_2m").asDouble();
            double tempF = (tempC * 9.0 / 5.0) + 32.0;
            double feelsLikeC = current.path("apparent_temperature").asDouble(tempC);
            double feelsLikeF = (feelsLikeC * 9.0 / 5.0) + 32.0;
            double windSpeed = current.path("wind_speed_10m").asDouble(0.0);
            int humidity = current.path("relative_humidity_2m").asInt(0);
            int cloudCover = current.path("cloud_cover").asInt(0);
            int weatherCode = current.path("weather_code").asInt(-1);
            int isDay = current.path("is_day").asInt(1);

            // Daily Forecast High/Low if available
            JsonNode daily = forecastResponse.path("daily");
            String maxTempStr = "";
            String minTempStr = "";
            if (!daily.isMissingNode()) {
                JsonNode maxArr = daily.path("temperature_2m_max");
                JsonNode minArr = daily.path("temperature_2m_min");
                if (maxArr.isArray() && !maxArr.isEmpty() && minArr.isArray() && !minArr.isEmpty()) {
                    maxTempStr = String.format("High: %.0f°C (%.0f°F)", maxArr.get(0).asDouble(), (maxArr.get(0).asDouble() * 9.0 / 5.0) + 32.0);
                    minTempStr = String.format("Low: %.0f°C (%.0f°F)", minArr.get(0).asDouble(), (minArr.get(0).asDouble() * 9.0 / 5.0) + 32.0);
                }
            }

            String description = weatherCodeDescription(weatherCode);
            String icon = weatherCodeIcon(weatherCode, isDay == 1);

            StringBuilder locationBuilder = new StringBuilder(resolvedCity);
            if (!admin1.isBlank() && !admin1.equalsIgnoreCase(resolvedCity)) {
                locationBuilder.append(", ").append(admin1);
            }
            if (!country.isBlank()) {
                locationBuilder.append(", ").append(country);
            }
            String fullLocation = locationBuilder.toString();

            Map<String, Object> weatherData = new LinkedHashMap<>();
            weatherData.put("city", resolvedCity);
            weatherData.put("country", country.isBlank() ? admin1 : country);
            weatherData.put("temperature", Math.round(tempC));
            weatherData.put("temperatureF", Math.round(tempF));
            weatherData.put("feelsLike", Math.round(feelsLikeC));
            weatherData.put("feelsLikeF", Math.round(feelsLikeF));
            weatherData.put("windSpeed", Math.round(windSpeed));
            weatherData.put("humidity", humidity);
            weatherData.put("cloudCover", cloudCover);
            weatherData.put("condition", description);
            weatherData.put("icon", icon);
            weatherData.put("isWeatherCard", true);

            StringBuilder replyBuilder = new StringBuilder();
            replyBuilder.append("🌦️ **Real-Time Weather in ").append(fullLocation).append(":**\n\n");
            replyBuilder.append("• **Condition:** ").append(description).append("\n");
            replyBuilder.append("• **Temperature:** ").append(Math.round(tempC)).append("°C / ").append(Math.round(tempF)).append("°F\n");
            replyBuilder.append("• **Feels Like:** ").append(Math.round(feelsLikeC)).append("°C / ").append(Math.round(feelsLikeF)).append("°F\n");
            replyBuilder.append("• **Humidity:** ").append(humidity).append("%\n");
            replyBuilder.append("• **Wind Speed:** ").append(Math.round(windSpeed)).append(" km/h\n");
            if (!maxTempStr.isEmpty() && !minTempStr.isEmpty()) {
                replyBuilder.append("• **Today's Range:** ").append(minTempStr).append(" | ").append(maxTempStr);
            }

            return new AssistantReply(replyBuilder.toString(), "weather-query", weatherData);
        } catch (Exception e) {
            logger.error("Failed to fetch live weather", e);
            return AssistantReply.text("I encountered a network issue while retrieving real-time weather. Please check your internet connection and try again.", "weather-query");
        }
    }

    /**
     * Smart location extractor for diverse natural language weather queries.
     */
    private String extractWeatherLocation(String input) {
        if (input == null || input.isBlank()) {
            return defaultWeatherLocation;
        }

        String raw = input.trim();
        String cleaned = raw.replaceAll("(?i)\\b(what is|what's|how is|how's|tell me|give me|check|show|the|a|an|current|live|today's|today|tomorrow|tonight|now|right now|outside|currently|please|this weekend|like|report|conditions|condition|forecast|weather|temperature|temp|climate|is it raining in|is it raining|is it sunny in|is it sunny|is it cold in|is it hot in)\\b", " ").trim();

        // Check for "in <city>", "for <city>", "of <city>", "at <city>"
        Pattern inPattern = Pattern.compile("(?:in|for|of|at|around)\\s+([a-zA-Z\\s,.'-]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = inPattern.matcher(raw);
        if (m.find() && m.group(1) != null && !m.group(1).isBlank()) {
            String candidate = m.group(1).replaceAll("(?i)\\b(today|tomorrow|now|right now|currently|please|outside)\\b", "").trim();
            if (!candidate.isBlank()) {
                return cleanCityString(candidate);
            }
        }

        // If cleaned string has remaining valid city text
        String candidate = cleanCityString(cleaned);
        if (!candidate.isBlank() && candidate.length() >= 2) {
            return candidate;
        }

        return defaultWeatherLocation;
    }

    private String cleanCityString(String str) {
        return str.replaceAll("[^a-zA-Z\\s,.'-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Universal Real-Time Knowledge & Generative AI Router:
     * 1. If Gemini API Key is configured (via Settings, environment variable GEMINI_API_KEY, or application.properties), query Gemini.
     * 2. Otherwise, query Real-Time Wikipedia REST & DuckDuckGo Instant Knowledge APIs.
     * 3. If offline/error, use the comprehensive built-in Knowledge & Reasoning engine.
     */
    private AssistantReply fetchUniversalKnowledgeOrAiReply(
            String input,
            String language,
            String sessionId,
            String personaKey,
            String customApiKey,
            String customModel) {

        String resolvedApiKey = resolveApiKey(customApiKey);
        String resolvedModel = (customModel != null && !customModel.isBlank()) ? customModel : defaultGeminiModel;
        AiPersona persona = getPersona(personaKey);

        // 1. If API Key is present, use Gemini Generative AI
        if (resolvedApiKey != null && !resolvedApiKey.isBlank()) {
            AssistantReply geminiReply = queryGeminiApi(input, sessionId, persona, resolvedApiKey, resolvedModel);
            if (geminiReply != null) {
                return geminiReply;
            }
        }

        // 2. Real-Time Live Wikipedia & DuckDuckGo Knowledge Engine
        AssistantReply liveWebReply = queryLiveEncyclopedicKnowledge(input, persona);
        if (liveWebReply != null) {
            return liveWebReply;
        }

        // 3. Comprehensive Built-In Knowledge Base & Reasoning
        return queryBuiltInKnowledgeEngine(input, persona);
    }

    private String resolveApiKey(String customApiKey) {
        if (customApiKey != null && !customApiKey.isBlank()) {
            return customApiKey.trim();
        }
        if (defaultGeminiApiKey != null && !defaultGeminiApiKey.isBlank()) {
            return defaultGeminiApiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        String googleKey = System.getenv("GOOGLE_API_KEY");
        if (googleKey != null && !googleKey.isBlank()) {
            return googleKey.trim();
        }
        return System.getProperty("gemini.api.key", "");
    }

    private AssistantReply queryGeminiApi(String input, String sessionId, AiPersona persona, String apiKey, String model) {
        try {
            String systemPrompt = persona.getSystemPrompt();

            // Construct multi-turn context
            List<ChatMessage> history = chatSessionService.getHistory(sessionId);
            List<Map<String, Object>> contents = new ArrayList<>();

            int startIdx = Math.max(0, history.size() - 6);
            for (int i = startIdx; i < history.size(); i++) {
                ChatMessage m = history.get(i);
                if (m.getContent() == null || m.getContent().isBlank()) continue;
                String role = "user".equalsIgnoreCase(m.getRole()) ? "user" : "model";
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", m.getContent()))
                ));
            }

            if (contents.isEmpty() || !"user".equals(contents.get(contents.size() - 1).get("role"))) {
                contents.add(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", input))
                ));
            }

            Map<String, Object> requestPayload = new LinkedHashMap<>();
            requestPayload.put("contents", contents);
            requestPayload.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", systemPrompt))
            ));
            requestPayload.put("generationConfig", Map.of(
                    "temperature", 0.7,
                    "maxOutputTokens", 1024
            ));

            String requestBody = objectMapper.writeValueAsString(requestPayload);
            String url = GEMINI_BASE_URL + "/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8)
                    + ":generateContent?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode contentNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
                String content = contentNode.asText("").trim();

                if (!content.isBlank()) {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("intent", "ai-response");
                    data.put("provider", "gemini");
                    data.put("model", model);
                    data.put("persona", persona.getId());
                    return new AssistantReply(content, "ai-response", data);
                }
            } else {
                logger.warn("Gemini API returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.warn("Gemini API request failed, falling back to real-time knowledge engine: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Real-Time Wikipedia REST & DuckDuckGo Instant Knowledge API
     * Provides live answers to real-time facts, people, places, definitions, science, history, and questions.
     */
    private AssistantReply queryLiveEncyclopedicKnowledge(String input, AiPersona persona) {
        String cleanQuery = cleanKnowledgeQuery(input);
        if (cleanQuery.isBlank() || cleanQuery.length() < 3) {
            return null;
        }

        try {
            // Step 1: Query Wikipedia Search API for the most relevant topic page
            String searchUrl = WIKIPEDIA_SEARCH_URL + "?action=query&list=search&srsearch="
                    + URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8)
                    + "&utf8=&format=json&srlimit=1";

            JsonNode searchRes = fetchJson(searchUrl);
            JsonNode searchResults = searchRes.path("query").path("search");

            if (searchResults.isArray() && !searchResults.isEmpty()) {
                String pageTitle = searchResults.get(0).path("title").asText("");
                if (!pageTitle.isBlank()) {
                    // Step 2: Fetch clean summary and extract
                    String summaryUrl = WIKIPEDIA_SUMMARY_URL + URLEncoder.encode(pageTitle.replace(" ", "_"), StandardCharsets.UTF_8);
                    JsonNode summaryRes = fetchJson(summaryUrl);

                    String extract = summaryRes.path("extract").asText("").trim();
                    String description = summaryRes.path("description").asText("").trim();
                    String displayTitle = summaryRes.path("title").asText(pageTitle);

                    if (!extract.isBlank()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("📖 **").append(displayTitle).append("**");
                        if (!description.isBlank()) {
                            sb.append(" *(").append(description).append(")*");
                        }
                        sb.append("\n\n").append(extract);

                        Map<String, Object> data = new LinkedHashMap<>();
                        data.put("intent", "knowledge-response");
                        data.put("source", "Wikipedia");
                        data.put("topic", displayTitle);
                        data.put("persona", persona.getId());

                        return new AssistantReply(sb.toString(), "knowledge-response", data);
                    }
                }
            }

            // Step 3: Try DuckDuckGo Instant Answer API
            String ddgUrl = DUCKDUCKGO_API_URL + "?q=" + URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8)
                    + "&format=json&no_html=1&skip_disambig=1";
            JsonNode ddgRes = fetchJson(ddgUrl);
            String abstractText = ddgRes.path("AbstractText").asText("").trim();
            String heading = ddgRes.path("Heading").asText("").trim();

            if (!abstractText.isBlank()) {
                String headingStr = heading.isBlank() ? capitalizeWords(cleanQuery) : heading;
                String reply = "💡 **" + headingStr + ":**\n\n" + abstractText;

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("intent", "knowledge-response");
                data.put("source", "DuckDuckGo");
                data.put("persona", persona.getId());
                return new AssistantReply(reply, "knowledge-response", data);
            }

        } catch (Exception e) {
            logger.debug("Live knowledge lookup skipped: {}", e.getMessage());
        }

        return null;
    }

    private String cleanKnowledgeQuery(String input) {
        return input.replaceAll("(?i)^(?:who is|who was|who are|what is|what are|what was|what were|tell me about|explain|describe|define|where is|where are|how does|how do|why is|why are|meaning of|history of)\\s+", "")
                .replaceAll("[?.,!]", "")
                .trim();
    }

    /**
     * Built-In Encyclopedic Knowledge Base & Coding Expert Engine (Instant Local Answers)
     */
    private AssistantReply queryBuiltInKnowledgeEngine(String input, AiPersona persona) {
        String lower = input.toLowerCase(Locale.ROOT).trim();

        // 1. Coding & Tech Questions
        if (lower.contains("java") || lower.contains("spring boot") || lower.contains("rest api") || lower.contains("code") || lower.contains("function") || lower.contains("class") || lower.contains("algorithm")) {
            String codeReply = """
                    Here is a modern Java Spring Boot REST Controller snippet:

                    ```java
                    package com.example.demo.controller;

                    import org.springframework.http.ResponseEntity;
                    import org.springframework.web.bind.annotation.*;
                    import java.util.Map;

                    @RestController
                    @RequestMapping("/api/v1/assistant")
                    @CrossOrigin(origins = "*")
                    public class DemoController {

                        @GetMapping("/status")
                        public ResponseEntity<Map<String, Object>> getStatus() {
                            return ResponseEntity.ok(Map.of(
                                "service", "Voice Assist AI",
                                "status", "ONLINE",
                                "timestamp", System.currentTimeMillis()
                            ));
                        }
                    }
                    ```

                    > 💡 **Tip:** Add your free Gemini API key in **Settings (⚙️)** to generate code in any language with full architectural explanations!""";
            return AssistantReply.text(codeReply, "code-helper");
        }

        // 2. Science & Astronomy
        if (lower.contains("earth") && lower.contains("moon")) {
            return AssistantReply.text("🌙 The average distance from Earth to the Moon is approximately **384,400 kilometers (238,855 miles)**. Light takes about 1.3 seconds to travel between them.", "knowledge-response");
        }
        if (lower.contains("earth") && lower.contains("sun")) {
            return AssistantReply.text("☀️ The average distance from Earth to the Sun is approximately **149.6 million kilometers (93 million miles)**, also known as **1 Astronomical Unit (AU)**. Sunlight takes about 8 minutes and 20 seconds to reach Earth.", "knowledge-response");
        }
        if (lower.contains("speed of light")) {
            return AssistantReply.text("⚡ The speed of light in a vacuum is exactly **299,792,458 meters per second** (approximately **300,000 km/s** or **186,282 miles per second**).", "knowledge-response");
        }
        if (lower.contains("photosynthesis")) {
            return AssistantReply.text("🌿 **Photosynthesis** is the process used by plants, algae, and some bacteria to convert sunlight, water, and carbon dioxide into oxygen and chemical energy (glucose).\n\n$$\\text{6CO}_2 + \\text{6H}_2\\text{O} + \\text{Light} \\rightarrow \\text{C}_6\\text{H}_{12}\\text{O}_6 + \\text{6O}_2$$", "knowledge-response");
        }
        if (lower.contains("gravity") || lower.contains("gravitation")) {
            return AssistantReply.text("🌌 **Gravity** is a fundamental interaction which causes mutual attraction between all things with mass or energy. On Earth, the acceleration due to gravity is approximately **9.81 m/s²**.", "knowledge-response");
        }

        // 3. Geography & Capitals
        if (lower.contains("capital of france")) return AssistantReply.text("🇫🇷 The capital of France is **Paris**.", "knowledge-response");
        if (lower.contains("capital of japan")) return AssistantReply.text("🇯🇵 The capital of Japan is **Tokyo**.", "knowledge-response");
        if (lower.contains("capital of india")) return AssistantReply.text("🇮🇳 The capital of India is **New Delhi**.", "knowledge-response");
        if (lower.contains("capital of usa") || lower.contains("capital of united states")) return AssistantReply.text("🇺🇸 The capital of the United States is **Washington, D.C.**", "knowledge-response");
        if (lower.contains("capital of uk") || lower.contains("capital of united kingdom") || lower.contains("capital of england")) return AssistantReply.text("🇬🇧 The capital of the United Kingdom is **London**.", "knowledge-response");
        if (lower.contains("capital of germany")) return AssistantReply.text("🇩🇪 The capital of Germany is **Berlin**.", "knowledge-response");
        if (lower.contains("capital of australia")) return AssistantReply.text("🇦🇺 The capital of Australia is **Canberra** (often mistaken for Sydney or Melbourne).", "knowledge-response");
        if (lower.contains("capital of canada")) return AssistantReply.text("🇨🇦 The capital of Canada is **Ottawa**.", "knowledge-response");
        if (lower.contains("capital of italy")) return AssistantReply.text("🇮🇹 The capital of Italy is **Rome**.", "knowledge-response");
        if (lower.contains("capital of spain")) return AssistantReply.text("🇪🇸 The capital of Spain is **Madrid**.", "knowledge-response");

        // 4. Default Conversational Fallback
        String defaultResponse = "I analyzed your question: *\"" + input.trim() + "\"* as **" + persona.getName() + "**.\n\n"
                + "I can answer live questions on any topic, provide real-time global weather, solve calculations, convert units, and organize tasks.\n\n"
                + "✨ **Power Up:** You can connect a free Gemini API Key in **Settings (⚙️)** to enable unlimited multi-turn reasoning and conversational dialogue!";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("intent", "conversational-reply");
        data.put("persona", persona.getId());
        return new AssistantReply(defaultResponse, "conversational-reply", data);
    }

    private JsonNode fetchJson(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "VoiceAssistAI/2.0 (Intelligent Voice Bot)")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP request failed with status " + response.statusCode());
        }

        return objectMapper.readTree(response.body());
    }

    private String weatherCodeDescription(int weatherCode) {
        return switch (weatherCode) {
            case 0 -> "Clear Skies ☀️";
            case 1 -> "Mainly Clear 🌤️";
            case 2 -> "Partly Cloudy ⛅";
            case 3 -> "Overcast ☁️";
            case 45, 48 -> "Foggy 🌫️";
            case 51, 53, 55 -> "Light Drizzle 🌦️";
            case 61, 63 -> "Moderate Rain 🌧️";
            case 65 -> "Heavy Rain 🌧️";
            case 66, 67 -> "Freezing Rain 🌨️";
            case 71, 73, 75 -> "Snowfall ❄️";
            case 77 -> "Snow Grains 🌨️";
            case 80, 81, 82 -> "Rain Showers 🌧️";
            case 85, 86 -> "Snow Showers 🌨️";
            case 95 -> "Thunderstorms ⛈️";
            case 96, 99 -> "Thunderstorms with Hail ⛈️";
            default -> "Variable Conditions 🌤️";
        };
    }

    private String weatherCodeIcon(int weatherCode, boolean isDay) {
        return switch (weatherCode) {
            case 0 -> isDay ? "fas fa-sun" : "fas fa-moon";
            case 1, 2 -> isDay ? "fas fa-cloud-sun" : "fas fa-cloud-moon";
            case 3 -> "fas fa-cloud";
            case 45, 48 -> "fas fa-smog";
            case 51, 53, 55, 61, 63, 65, 80, 81, 82 -> "fas fa-cloud-showers-heavy";
            case 66, 67, 71, 73, 75, 77, 85, 86 -> "fas fa-snowflake";
            case 95, 96, 99 -> "fas fa-bolt";
            default -> "fas fa-cloud-sun";
        };
    }

    private double evaluateMathExpression(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else if (eat('%')) x %= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return +parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor());

                return x;
            }
        }.parse();
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

    private String createErrorResponse(String errorMessage) {
        try {
            VoiceResponse response = VoiceResponse.builder()
                    .status("error")
                    .message(errorMessage)
                    .timestamp(System.currentTimeMillis())
                    .build();

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            return "{\"status\": \"error\", \"message\": \"" + errorMessage + "\"}";
        }
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return "";
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase(Locale.ROOT))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public record AssistantReply(String message, String intent, Map<String, Object> data) {
        public static AssistantReply text(String message, String intent) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("intent", intent);
            return new AssistantReply(message, intent, payload);
        }

        public static AssistantReply action(String message, String intent, Map<String, Object> actionData) {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("intent", intent);
            payload.putAll(actionData);
            return new AssistantReply(message, intent, payload);
        }
    }
}
