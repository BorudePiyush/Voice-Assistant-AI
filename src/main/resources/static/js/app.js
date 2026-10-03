/**
 * Voice Assist AI - Complete Frontend Application Logic
 * Next-Gen Conversational AI & Voice Assistant
 */

// ==================== App State & Configurations ====================
const state = {
    sessionId: localStorage.getItem('va_session_id') || 'session-' + Date.now().toString(36),
    persona: localStorage.getItem('va_persona') || 'jarvis',
    personas: [],
    sessions: [],
    messages: [],
    tasks: [],
    ws: null,
    isWsConnected: false,
    isRecording: false,
    isAiSpeaking: false,
    isAiThinking: false,
    recognition: null,
    audioContext: null,
    analyser: null,
    microphoneStream: null,
    visualizerAnimId: null,
    plasmaOrbAnimId: null,
    settings: {
        geminiApiKey: localStorage.getItem('va_gemini_api_key') || '',
        geminiModel: localStorage.getItem('va_gemini_model') || 'gemini-1.5-flash',
        voiceName: localStorage.getItem('va_voice_name') || '',
        voiceRate: parseFloat(localStorage.getItem('va_voice_rate') || '1.0'),
        voicePitch: parseFloat(localStorage.getItem('va_voice_pitch') || '1.0'),
        autoSpeak: localStorage.getItem('va_auto_speak') !== 'false',
        soundFx: localStorage.getItem('va_sound_fx') !== 'false',
        sttLanguage: localStorage.getItem('va_stt_lang') || 'en-US',
        theme: localStorage.getItem('va_theme') || 'cyber-dark'
    }
};

// ==================== Audio Synthesizer Sound FX ====================
class SoundFX {
    constructor() {
        this.ctx = null;
    }

    init() {
        if (!this.ctx) {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            if (AudioContext) {
                this.ctx = new AudioContext();
            }
        }
    }

    playTone(freq, type = 'sine', duration = 0.12, gainVal = 0.08) {
        if (!state.settings.soundFx) return;
        try {
            this.init();
            if (!this.ctx) return;
            if (this.ctx.state === 'suspended') {
                this.ctx.resume();
            }

            const osc = this.ctx.createOscillator();
            const gain = this.ctx.createGain();

            osc.type = type;
            osc.frequency.setValueAtTime(freq, this.ctx.currentTime);

            gain.gain.setValueAtTime(gainVal, this.ctx.currentTime);
            gain.gain.exponentialRampToValueAtTime(0.0001, this.ctx.currentTime + duration);

            osc.connect(gain);
            gain.connect(this.ctx.destination);

            osc.start();
            osc.stop(this.ctx.currentTime + duration);
        } catch (e) {
            console.warn('Sound FX error', e);
        }
    }

    playSend() {
        this.playTone(587.33, 'sine', 0.1, 0.06); // D5
        setTimeout(() => this.playTone(880, 'sine', 0.15, 0.05), 60); // A5
    }

    playReceive() {
        this.playTone(783.99, 'triangle', 0.1, 0.06); // G5
        setTimeout(() => this.playTone(1046.50, 'sine', 0.18, 0.06), 70); // C6
    }

    playMicStart() {
        this.playTone(440, 'sine', 0.08, 0.07);
        setTimeout(() => this.playTone(659.25, 'sine', 0.12, 0.07), 50);
    }

    playMicStop() {
        this.playTone(659.25, 'sine', 0.08, 0.07);
        setTimeout(() => this.playTone(440, 'sine', 0.12, 0.07), 50);
    }

    playTaskComplete() {
        this.playTone(523.25, 'sine', 0.1, 0.08); // C5
        setTimeout(() => this.playTone(659.25, 'sine', 0.1, 0.08), 80); // E5
        setTimeout(() => this.playTone(783.99, 'sine', 0.2, 0.08), 160); // G5
    }
}

const sfx = new SoundFX();

// ==================== Application Initialization ====================
document.addEventListener('DOMContentLoaded', () => {
    applyTheme(state.settings.theme);
    initializeSpeechRecognition();
    initializeSpeechSynthesis();
    fetchPersonas();
    fetchSessions();
    loadChatHistory(state.sessionId);
    fetchTasks();
    connectWebSocket();
    startTelemetryHeartbeat();
    startVisualizerAnimation();

    // Event Listeners for theme selector & dropdowns
    setupDropdownListeners();

    // Save session ID
    localStorage.setItem('va_session_id', state.sessionId);

    // Initial Welcome Log
    console.log('%c Voice Assist AI 2.0 Initialized ', 'background: #6366f1; color: #fff; font-weight: bold; border-radius: 4px; padding: 4px 8px;');
});

// ==================== Dropdown & UI Controls ====================
function setupDropdownListeners() {
    // Theme Switcher dropdown toggle
    const themeBtn = document.getElementById('themeBtn');
    const themeMenu = document.getElementById('themeDropdownMenu');
    themeBtn?.addEventListener('click', (e) => {
        e.stopPropagation();
        themeMenu?.classList.toggle('show');
        document.getElementById('personaDropdownMenu')?.classList.remove('show');
    });

    // Theme options click
    document.querySelectorAll('.theme-opt').forEach(btn => {
        btn.addEventListener('click', () => {
            const themeVal = btn.getAttribute('data-theme-val');
            applyTheme(themeVal);
            themeMenu?.classList.remove('show');
        });
    });

    // Persona dropdown toggle
    const personaBtn = document.getElementById('personaSelectBtn');
    const personaMenu = document.getElementById('personaDropdownMenu');
    personaBtn?.addEventListener('click', (e) => {
        e.stopPropagation();
        personaMenu?.classList.toggle('show');
        themeMenu?.classList.remove('show');
    });

    // Close dropdowns on outside click
    document.addEventListener('click', () => {
        themeMenu?.classList.remove('show');
        personaMenu?.classList.remove('show');
    });

    // Sidebar Toggle
    const sidebarToggle = document.getElementById('sidebarToggleBtn');
    const sidebar = document.getElementById('appSidebar');
    sidebarToggle?.addEventListener('click', () => {
        if (window.innerWidth <= 900) {
            sidebar?.classList.toggle('mobile-open');
        } else {
            sidebar?.classList.toggle('collapsed');
        }
    });

    // Modals Open Triggers
    document.getElementById('openSettingsBtn')?.addEventListener('click', openSettingsModal);
    document.getElementById('openTasksBtn')?.addEventListener('click', openTasksModal);
    document.getElementById('openApiTesterBtn')?.addEventListener('click', openApiTesterModal);
    document.getElementById('openVoiceOrbBtn')?.addEventListener('click', openVoiceOrbMode);
}

function applyTheme(themeName) {
    document.documentElement.setAttribute('data-theme', themeName);
    state.settings.theme = themeName;
    localStorage.setItem('va_theme', themeName);

    document.querySelectorAll('.theme-opt').forEach(opt => {
        if (opt.getAttribute('data-theme-val') === themeName) {
            opt.classList.add('active');
        } else {
            opt.classList.remove('active');
        }
    });
}

// ==================== WebSocket Management ====================
function connectWebSocket(manual = false) {
    if (state.ws && (state.ws.readyState === WebSocket.OPEN || state.ws.readyState === WebSocket.CONNECTING)) {
        if (manual) showToast('WebSocket is already connected', 'info');
        return;
    }

    try {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        const wsUrl = `${protocol}//${window.location.host}/ws/voice`;
        state.ws = new WebSocket(wsUrl);

        state.ws.onopen = () => {
            state.isWsConnected = true;
            updateWsIndicator(true);
            if (manual) showToast('WebSocket Connected', 'success');
        };

        state.ws.onmessage = (event) => {
            try {
                const response = JSON.parse(event.data);
                if (response.status === 'connected') {
                    console.log('WS Session ACK:', response.sessionId);
                    return;
                }
                handleIncomingAiResponse(response);
            } catch (e) {
                console.error('Error parsing WS message', e);
            }
        };

        state.ws.onerror = (err) => {
            state.isWsConnected = false;
            updateWsIndicator(false);
        };

        state.ws.onclose = () => {
            state.isWsConnected = false;
            updateWsIndicator(false);
            // Auto reconnect in 5s
            setTimeout(() => {
                if (!state.isWsConnected) connectWebSocket();
            }, 5000);
        };
    } catch (e) {
        state.isWsConnected = false;
        updateWsIndicator(false);
    }
}

function updateWsIndicator(connected) {
    const pill = document.getElementById('wsIndicatorPill');
    const label = document.getElementById('wsStatusLabel');
    if (!pill || !label) return;

    if (connected) {
        pill.className = 'ws-indicator-pill connected';
        label.textContent = 'WS Online';
        pill.title = 'WebSocket Connected (Real-Time)';
    } else {
        pill.className = 'ws-indicator-pill disconnected';
        label.textContent = 'WS Offline';
        pill.title = 'Click to reconnect WebSocket';
    }
}

// ==================== Personas Management ====================
async function fetchPersonas() {
    try {
        const res = await fetch('/api/v1/chat/personas');
        if (res.ok) {
            state.personas = await res.json();
            renderPersonaDropdown();
            updateActivePersonaUI(state.persona);
        }
    } catch (e) {
        console.warn('Failed to fetch personas', e);
    }
}

function renderPersonaDropdown() {
    const menu = document.getElementById('personaDropdownMenu');
    if (!menu || !state.personas.length) return;

    menu.innerHTML = state.personas.map(p => `
        <button class="persona-option ${p.id === state.persona ? 'active' : ''}" onclick="selectPersona('${p.id}')">
            <div class="persona-opt-avatar" style="background: ${p.color || '#6366f1'}">
                <i class="${p.avatar || 'fas fa-robot'}"></i>
            </div>
            <div class="persona-opt-content">
                <span class="persona-opt-title">${escapeHtml(p.name)}</span>
                <span class="persona-opt-desc">${escapeHtml(p.description)}</span>
            </div>
        </button>
    `).join('');
}

function selectPersona(personaId) {
    state.persona = personaId;
    localStorage.setItem('va_persona', personaId);
    updateActivePersonaUI(personaId);
    renderPersonaDropdown();
    document.getElementById('personaDropdownMenu')?.classList.remove('show');
    showToast(`Switched persona to ${getPersonaName(personaId)}`, 'info');
}

function updateActivePersonaUI(personaId) {
    const persona = state.personas.find(p => p.id === personaId) || {
        name: 'Jarvis AI',
        tag: 'General',
        avatar: 'fas fa-robot',
        color: '#6366f1'
    };

    const headerAvatar = document.getElementById('headerPersonaAvatar');
    const headerName = document.getElementById('headerPersonaName');
    const headerTag = document.getElementById('headerPersonaTag');
    const currentPill = document.getElementById('currentPersonaPill');

    if (headerAvatar) headerAvatar.innerHTML = `<i class="${persona.avatar}"></i>`;
    if (headerName) headerName.textContent = persona.name;
    if (headerTag) headerTag.textContent = persona.tag || 'AI Persona';
    if (currentPill) currentPill.innerHTML = `<i class="${persona.avatar}"></i> ${escapeHtml(persona.name)}`;
}

function getPersonaName(id) {
    const p = state.personas.find(item => item.id === id);
    return p ? p.name : 'AI Assistant';
}

// ==================== Chat Session Management ====================
async function fetchSessions() {
    try {
        const res = await fetch('/api/v1/chat/sessions');
        if (res.ok) {
            state.sessions = await res.json();
            renderSessionsList();
        }
    } catch (e) {
        console.warn('Failed to load sessions', e);
    }
}

function renderSessionsList() {
    const list = document.getElementById('sessionsList');
    const count = document.getElementById('totalSessionsCount');
    if (!list) return;

    if (count) count.textContent = state.sessions.length;

    if (state.sessions.length === 0) {
        list.innerHTML = `<div style="text-align: center; padding: 20px; color: var(--text-dim); font-size: 0.8rem;">No previous conversations</div>`;
        return;
    }

    list.innerHTML = state.sessions.map(s => `
        <div class="session-item-card ${s.id === state.sessionId ? 'active' : ''}" onclick="switchSession('${s.id}')">
            <i class="fas fa-message session-item-icon"></i>
            <div class="session-item-details">
                <div class="session-item-title">${escapeHtml(s.title || 'Conversation')}</div>
                <div class="session-item-preview">${escapeHtml(s.lastMessagePreview || 'Empty chat')}</div>
            </div>
            <button class="session-delete-btn" onclick="deleteSession(event, '${s.id}')" title="Delete conversation">
                <i class="fas fa-trash"></i>
            </button>
        </div>
    `).join('');
}

function startNewChat() {
    const newId = 'session-' + Date.now().toString(36);
    state.sessionId = newId;
    localStorage.setItem('va_session_id', newId);
    state.messages = [];

    document.getElementById('currentSessionTitle').textContent = 'New Conversation';
    document.getElementById('welcomeHeroCard').style.display = 'block';
    document.getElementById('messagesStream').innerHTML = '';

    fetchSessions();
    fetchTasks();
    if (window.innerWidth <= 900) {
        document.getElementById('appSidebar')?.classList.remove('mobile-open');
    }
    showToast('New conversation started', 'info');
}

async function switchSession(sessionId) {
    if (sessionId === state.sessionId && state.messages.length > 0) return;
    state.sessionId = sessionId;
    localStorage.setItem('va_session_id', sessionId);
    await loadChatHistory(sessionId);
    fetchSessions();
    fetchTasks();
    if (window.innerWidth <= 900) {
        document.getElementById('appSidebar')?.classList.remove('mobile-open');
    }
}

async function loadChatHistory(sessionId) {
    try {
        const res = await fetch(`/api/v1/chat/history/${sessionId}`);
        if (res.ok) {
            state.messages = await res.json();
            renderChatMessages();
        }
    } catch (e) {
        console.warn('Failed to load history', e);
    }
}

function renderChatMessages() {
    const container = document.getElementById('messagesStream');
    const hero = document.getElementById('welcomeHeroCard');
    if (!container) return;

    if (state.messages.length === 0) {
        if (hero) hero.style.display = 'block';
        container.innerHTML = '';
        return;
    }

    if (hero) hero.style.display = 'none';

    container.innerHTML = state.messages.map(msg => renderMessageHtml(msg)).join('');
    scrollToBottom();
}

function renderMessageHtml(msg) {
    const isUser = msg.role === 'user';
    const roleClass = isUser ? 'user' : 'assistant';
    const avatar = isUser ? '<i class="fas fa-user"></i>' : `<i class="${getPersonaAvatar(msg.persona)}"></i>`;
    const formattedContent = isUser ? escapeHtml(msg.content) : parseMarkdown(msg.content);
    const timeStr = formatTimestamp(msg.timestamp);

    // Rich widget additions
    let widgetHtml = '';
    if (!isUser && msg.data) {
        widgetHtml = renderWidgetData(msg.data);
    }

    return `
        <div class="message-row ${roleClass}">
            <div class="message-avatar">${avatar}</div>
            <div class="message-body-wrapper">
                <div class="message-bubble">${formattedContent}${widgetHtml}</div>
                <div class="message-footer-meta">
                    <span>${timeStr}</span>
                    ${!isUser ? `
                        <button class="msg-action-btn" onclick="speakText('${escapeForAttribute(msg.content)}')" title="Speak message">
                            <i class="fas fa-volume-high"></i>
                        </button>
                        <button class="msg-action-btn" onclick="copyTextToClipboard('${escapeForAttribute(msg.content)}')" title="Copy text">
                            <i class="fas fa-copy"></i>
                        </button>
                    ` : ''}
                </div>
            </div>
        </div>
    `;
}

function renderWidgetData(data) {
    if (!data) return '';
    let html = '';

    // 1. Live Weather Card Widget
    if (data.isWeatherCard) {
        html += `
            <div class="weather-response-card">
                <div class="weather-info-main">
                    <i class="${data.icon || 'fas fa-cloud-sun'} weather-card-icon"></i>
                    <div>
                        <div class="weather-temp-hero">${data.temperature}°C</div>
                        <div class="weather-city-name">${escapeHtml(data.city)}${data.country ? ', ' + escapeHtml(data.country) : ''}</div>
                    </div>
                </div>
                <div class="weather-details-grid">
                    <div class="weather-detail-item"><i class="fas fa-temperature-half"></i> Feels: ${data.feelsLike}°C</div>
                    <div class="weather-detail-item"><i class="fas fa-droplet"></i> Humidity: ${data.humidity}%</div>
                    <div class="weather-detail-item"><i class="fas fa-wind"></i> Wind: ${data.windSpeed} km/h</div>
                    <div class="weather-detail-item"><i class="fas fa-cloud"></i> ${escapeHtml(data.condition || '')}</div>
                </div>
            </div>
        `;
    }

    // 2. Action Links (YouTube, Spotify, etc.)
    if (data.openUrl) {
        html += `
            <div class="action-buttons-container">
                <a href="${data.openUrl}" target="_blank" rel="noopener noreferrer" class="action-launch-btn">
                    <i class="${data.icon || 'fas fa-external-link-alt'}"></i> Open ${escapeHtml(data.label || 'Link')}
                </a>
            </div>
        `;
    }

    return html;
}

function getPersonaAvatar(personaId) {
    const p = state.personas.find(item => item.id === personaId);
    return p ? p.avatar : 'fas fa-robot';
}

function formatTimestamp(ts) {
    if (!ts) return '';
    const date = new Date(ts);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

async function deleteSession(event, sessionId) {
    event.stopPropagation();
    if (!confirm('Are you sure you want to delete this conversation?')) return;

    try {
        await fetch(`/api/v1/chat/sessions/${sessionId}`, { method: 'DELETE' });
        state.sessions = state.sessions.filter(s => s.id !== sessionId);
        if (state.sessionId === sessionId) {
            startNewChat();
        } else {
            renderSessionsList();
        }
        showToast('Conversation deleted', 'info');
    } catch (e) {
        showToast('Failed to delete conversation', 'error');
    }
}

async function clearCurrentChat() {
    if (!confirm('Clear all messages in this conversation?')) return;
    try {
        await fetch(`/api/v1/chat/history/${state.sessionId}`, { method: 'DELETE' });
        state.messages = [];
        renderChatMessages();
        fetchSessions();
        showToast('Chat cleared', 'info');
    } catch (e) {
        showToast('Failed to clear chat', 'error');
    }
}

function exportCurrentChat() {
    window.open(`/api/v1/chat/export/${state.sessionId}`, '_blank');
}

function filterSessions(query) {
    const q = query.toLowerCase().trim();
    document.querySelectorAll('.session-item-card').forEach(card => {
        const title = card.querySelector('.session-item-title')?.textContent.toLowerCase() || '';
        const preview = card.querySelector('.session-item-preview')?.textContent.toLowerCase() || '';
        if (title.includes(q) || preview.includes(q)) {
            card.style.display = 'flex';
        } else {
            card.style.display = 'none';
        }
    });
}

// ==================== Message Sending & NLP Processing ====================
function submitTextMessage() {
    const input = document.getElementById('chatInput');
    const text = input?.value.trim();
    if (!text) return;

    input.value = '';
    autoResizeInput(input);
    document.getElementById('charCountMeta').textContent = '0 chars';

    sendChatMessage(text);
}

function sendSuggestedPrompt(promptText) {
    const input = document.getElementById('chatInput');
    if (input) input.value = promptText;
    submitTextMessage();
}

function handleInputKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        submitTextMessage();
    }
}

function autoResizeInput(textarea) {
    if (!textarea) return;
    textarea.style.height = 'auto';
    textarea.style.height = Math.min(textarea.scrollHeight, 140) + 'px';
    const chars = textarea.value.length;
    document.getElementById('charCountMeta').textContent = `${chars} chars`;
}

function clearInputField() {
    const input = document.getElementById('chatInput');
    if (input) {
        input.value = '';
        autoResizeInput(input);
    }
}

async function sendChatMessage(userText) {
    // Hide welcome hero
    document.getElementById('welcomeHeroCard').style.display = 'none';

    // Play send sound
    sfx.playSend();

    // Add user message to UI
    const userMsg = {
        id: 'msg-' + Date.now(),
        sessionId: state.sessionId,
        role: 'user',
        content: userText,
        persona: state.persona,
        timestamp: Date.now()
    };
    state.messages.push(userMsg);
    appendMessageToStream(userMsg);
    scrollToBottom();

    // Show thinking indicator
    setAiStatus('thinking');
    showThinkingBubble(true);

    // Update active session title preview
    if (state.messages.length === 1) {
        document.getElementById('currentSessionTitle').textContent = userText.substring(0, 30);
    }

    const payload = {
        sessionId: state.sessionId,
        message: userText,
        persona: state.persona,
        apiKey: state.settings.geminiApiKey,
        model: state.settings.geminiModel,
        language: state.settings.sttLanguage
    };

    // If WebSocket is open, send via WS; else fallback to REST
    if (state.ws && state.ws.readyState === WebSocket.OPEN) {
        const wsMsg = {
            type: 'text',
            content: userText,
            sessionId: state.sessionId,
            language: state.settings.sttLanguage,
            timestamp: Date.now()
        };
        state.ws.send(JSON.stringify(wsMsg));
    } else {
        try {
            const response = await fetch('/api/v1/chat/message', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!response.ok) throw new Error('API request failed');
            const data = await response.json();
            handleIncomingAiResponse(data);
        } catch (e) {
            console.error('Chat error', e);
            handleIncomingAiResponse({
                status: 'error',
                message: 'Failed to connect to assistant service. Please check server status.'
            });
        }
    }
}

function handleIncomingAiResponse(responseObj) {
    showThinkingBubble(false);
    setAiStatus('ready');

    if (responseObj.status === 'success') {
        sfx.playReceive();

        const assistantMsg = {
            id: 'msg-' + Date.now(),
            sessionId: state.sessionId,
            role: 'assistant',
            content: responseObj.message,
            persona: state.persona,
            timestamp: Date.now(),
            data: responseObj.data
        };

        state.messages.push(assistantMsg);
        appendMessageToStream(assistantMsg);
        scrollToBottom();

        // Check if task list updated in background
        if (responseObj.data && responseObj.data.tasks) {
            state.tasks = responseObj.data.tasks;
            renderTasksList();
        }

        // Auto speak if enabled
        if (state.settings.autoSpeak) {
            speakText(responseObj.message);
        }

        // Subtitle in Voice Orb Mode
        const orbSubtitle = document.getElementById('orbSubtitleTranscript');
        if (orbSubtitle) {
            orbSubtitle.textContent = responseObj.message;
        }

        fetchSessions();
    } else {
        const errorMsg = {
            id: 'msg-' + Date.now(),
            sessionId: state.sessionId,
            role: 'assistant',
            content: '⚠️ ' + (responseObj.message || 'An error occurred while processing.'),
            persona: state.persona,
            timestamp: Date.now()
        };
        state.messages.push(errorMsg);
        appendMessageToStream(errorMsg);
        scrollToBottom();
    }
}

function appendMessageToStream(msg) {
    const stream = document.getElementById('messagesStream');
    if (!stream) return;

    const div = document.createElement('div');
    div.innerHTML = renderMessageHtml(msg);
    if (div.firstElementChild) {
        stream.appendChild(div.firstElementChild);
    }
}

function showThinkingBubble(show) {
    const bubble = document.getElementById('thinkingBubble');
    if (bubble) bubble.style.display = show ? 'flex' : 'none';
    if (show) scrollToBottom();
}

function setAiStatus(status) {
    const pill = document.getElementById('aiStatusPill');
    const text = document.getElementById('aiStatusText');
    if (!pill || !text) return;

    pill.className = `ai-status-pill ${status}`;
    if (status === 'thinking') {
        text.textContent = 'Thinking...';
        state.isAiThinking = true;
    } else if (status === 'speaking') {
        text.textContent = 'Speaking...';
        state.isAiSpeaking = true;
    } else {
        text.textContent = 'Ready';
        state.isAiThinking = false;
        state.isAiSpeaking = false;
    }
}

function scrollToBottom() {
    const container = document.getElementById('chatMessagesContainer');
    if (container) {
        setTimeout(() => {
            container.scrollTop = container.scrollHeight;
        }, 50);
    }
}

// ==================== Speech Recognition (STT) ====================
function initializeSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        console.warn('Web Speech Recognition API is not supported in this browser.');
        return;
    }

    state.recognition = new SpeechRecognition();
    state.recognition.continuous = false;
    state.recognition.interimResults = true;
    state.recognition.lang = state.settings.sttLanguage;

    state.recognition.onstart = () => {
        state.isRecording = true;
        updateRecordingUI(true);
        sfx.playMicStart();
        document.getElementById('sttLivePreview').style.display = 'flex';
        document.getElementById('sttTranscriptText').textContent = 'Listening to your voice...';
    };

    state.recognition.onresult = (event) => {
        let interim = '';
        let final = '';

        for (let i = event.resultIndex; i < event.results.length; ++i) {
            if (event.results[i].isFinal) {
                final += event.results[i][0].transcript;
            } else {
                interim += event.results[i][0].transcript;
            }
        }

        const currentText = final || interim;
        document.getElementById('sttTranscriptText').textContent = currentText;

        const orbSub = document.getElementById('orbSubtitleTranscript');
        if (orbSub) orbSub.textContent = currentText;

        if (final && final.trim().length > 0) {
            document.getElementById('chatInput').value = final.trim();
            submitTextMessage();
            stopSpeechRecognition();
        }
    };

    state.recognition.onerror = (event) => {
        console.warn('STT Error:', event.error);
        stopSpeechRecognition();
    };

    state.recognition.onend = () => {
        stopSpeechRecognition();
    };
}

function toggleVoiceInput() {
    if (!state.recognition) {
        initializeSpeechRecognition();
        if (!state.recognition) {
            showToast('Speech recognition is not supported in your browser', 'error');
            return;
        }
    }

    if (state.isRecording) {
        stopSpeechRecognition();
    } else {
        try {
            state.recognition.lang = state.settings.sttLanguage;
            state.recognition.start();
        } catch (e) {
            console.warn('Mic start failed', e);
        }
    }
}

function stopSpeechRecognition() {
    if (state.recognition && state.isRecording) {
        state.recognition.stop();
    }
    state.isRecording = false;
    updateRecordingUI(false);
    sfx.playMicStop();
    const sttPreview = document.getElementById('sttLivePreview');
    if (sttPreview) sttPreview.style.display = 'none';
}

function updateRecordingUI(recording) {
    const micBtn = document.getElementById('voiceMicBtn');
    const orbMicBtn = document.getElementById('orbBigMicBtn');

    if (recording) {
        micBtn?.classList.add('recording');
        orbMicBtn?.classList.add('recording');
    } else {
        micBtn?.classList.remove('recording');
        orbMicBtn?.classList.remove('recording');
    }
}

// ==================== Speech Synthesis (TTS) ====================
function initializeSpeechSynthesis() {
    if (!('speechSynthesis' in window)) {
        console.warn('SpeechSynthesis is not supported in this browser.');
        return;
    }

    // Populate voice dropdown when voices are loaded
    window.speechSynthesis.onvoiceschanged = () => {
        populateVoiceSelectOptions();
    };
    populateVoiceSelectOptions();
}

function populateVoiceSelectOptions() {
    const select = document.getElementById('voiceSelect');
    if (!select || !('speechSynthesis' in window)) return;

    const voices = window.speechSynthesis.getVoices();
    if (!voices.length) return;

    select.innerHTML = voices.map(v => `
        <option value="${v.name}" ${v.name === state.settings.voiceName ? 'selected' : ''}>
            ${v.name} (${v.lang})
        </option>
    `).join('');
}

function speakText(text) {
    if (!('speechSynthesis' in window) || !text) return;

    window.speechSynthesis.cancel();

    // Clean markdown before speaking
    const cleanText = stripMarkdownForSpeech(text);

    const utterance = new SpeechSynthesisUtterance(cleanText);
    const voices = window.speechSynthesis.getVoices();

    if (state.settings.voiceName) {
        const found = voices.find(v => v.name === state.settings.voiceName);
        if (found) utterance.voice = found;
    }

    utterance.rate = state.settings.voiceRate;
    utterance.pitch = state.settings.voicePitch;
    utterance.lang = state.settings.sttLanguage;

    utterance.onstart = () => {
        setAiStatus('speaking');
    };

    utterance.onend = () => {
        setAiStatus('ready');
    };

    utterance.onerror = () => {
        setAiStatus('ready');
    };

    window.speechSynthesis.speak(utterance);
}

function toggleSpeechAudio() {
    state.settings.autoSpeak = !state.settings.autoSpeak;
    localStorage.setItem('va_auto_speak', state.settings.autoSpeak);

    const btn = document.getElementById('ttsToggleBtn');
    const icon = document.getElementById('ttsToggleIcon');

    if (state.settings.autoSpeak) {
        btn?.classList.add('active');
        if (icon) icon.className = 'fas fa-volume-high';
        showToast('Voice Audio output enabled', 'info');
    } else {
        btn?.classList.remove('active');
        if (icon) icon.className = 'fas fa-volume-xmark';
        window.speechSynthesis.cancel();
        showToast('Voice Audio muted', 'info');
    }
}

function testVoiceSpeech() {
    speakText('Hello! This is a test of your Voice Assist AI synthesizer voice.');
}

function stripMarkdownForSpeech(md) {
    if (!md) return '';
    return md
        .replace(/```[\s\S]*?```/g, ' Code snippet omitted for speech. ')
        .replace(/`([^`]+)`/g, '$1')
        .replace(/\*\*([^*]+)\*\*/g, '$1')
        .replace(/\*([^*]+)\*/g, '$1')
        .replace(/#+\s+/g, '')
        .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
        .replace(/[-*]\s+/g, '')
        .trim();
}

// ==================== Real-time Audio Visualizers ====================
function startVisualizerAnimation() {
    const canvas = document.getElementById('audioVisualizerCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    let step = 0;

    function render() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const isSpeaking = state.isAiSpeaking || state.isRecording;
        const barCount = 18;
        const barWidth = 4;
        const gap = 4;
        const totalWidth = (barWidth + gap) * barCount;
        const startX = (canvas.width - totalWidth) / 2;

        for (let i = 0; i < barCount; i++) {
            let height = 4;
            if (isSpeaking) {
                const wave = Math.sin(step * 0.15 + i * 0.5) * 0.5 + 0.5;
                const wave2 = Math.cos(step * 0.1 + i * 0.3) * 0.5 + 0.5;
                height = 6 + (wave * wave2 * 18);
            } else {
                height = 3 + Math.sin(step * 0.05 + i) * 1.5;
            }

            const x = startX + i * (barWidth + gap);
            const y = (canvas.height - height) / 2;

            const grad = ctx.createLinearGradient(0, y, 0, y + height);
            grad.addColorStop(0, '#6366f1');
            grad.addColorStop(1, '#06b6d4');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.roundRect(x, y, barWidth, height, 2);
            ctx.fill();
        }

        step++;
        requestAnimationFrame(render);
    }

    render();
}

// ==================== Fullscreen "Orb Voice Mode" ====================
function openVoiceOrbMode() {
    const modal = document.getElementById('voiceOrbModal');
    if (!modal) return;
    modal.style.display = 'flex';
    initPlasmaOrb();
}

function closeVoiceOrbMode() {
    const modal = document.getElementById('voiceOrbModal');
    if (!modal) return;
    modal.style.display = 'none';
    if (state.plasmaOrbAnimId) {
        cancelAnimationFrame(state.plasmaOrbAnimId);
    }
}

function initPlasmaOrb() {
    const canvas = document.getElementById('plasmaOrbCanvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');

    let angle = 0;
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    const baseRadius = 80;

    function draw() {
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        const isAudioActive = state.isRecording || state.isAiSpeaking;
        const dynamicRadius = isAudioActive ? baseRadius + Math.sin(angle * 3) * 16 : baseRadius + Math.sin(angle) * 4;

        // Outer Neon Glow
        const outerGrad = ctx.createRadialGradient(centerX, centerY, dynamicRadius * 0.4, centerX, centerY, dynamicRadius * 1.6);
        outerGrad.addColorStop(0, 'rgba(99, 102, 241, 0.6)');
        outerGrad.addColorStop(0.5, 'rgba(6, 182, 212, 0.3)');
        outerGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

        ctx.fillStyle = outerGrad;
        ctx.beginPath();
        ctx.arc(centerX, centerY, dynamicRadius * 1.6, 0, Math.PI * 2);
        ctx.fill();

        // Pulsing Swirl Layers
        for (let j = 0; j < 3; j++) {
            ctx.save();
            ctx.translate(centerX, centerY);
            ctx.rotate(angle * (j % 2 === 0 ? 1 : -1) * (0.4 + j * 0.2));

            ctx.beginPath();
            for (let i = 0; i <= Math.PI * 2; i += 0.1) {
                const offset = Math.sin(i * 4 + angle * 2 + j) * (isAudioActive ? 14 : 6);
                const r = dynamicRadius + offset;
                const x = Math.cos(i) * r;
                const y = Math.sin(i) * r;
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.closePath();

            const innerGrad = ctx.createLinearGradient(-dynamicRadius, -dynamicRadius, dynamicRadius, dynamicRadius);
            if (j === 0) {
                innerGrad.addColorStop(0, 'rgba(99, 102, 241, 0.7)');
                innerGrad.addColorStop(1, 'rgba(236, 72, 153, 0.7)');
            } else if (j === 1) {
                innerGrad.addColorStop(0, 'rgba(6, 182, 212, 0.6)');
                innerGrad.addColorStop(1, 'rgba(99, 102, 241, 0.6)');
            } else {
                innerGrad.addColorStop(0, 'rgba(168, 85, 247, 0.5)');
                innerGrad.addColorStop(1, 'rgba(6, 182, 212, 0.5)');
            }

            ctx.fillStyle = innerGrad;
            ctx.fill();
            ctx.restore();
        }

        // Center Core
        const coreGrad = ctx.createRadialGradient(centerX, centerY, 0, centerX, centerY, dynamicRadius * 0.6);
        coreGrad.addColorStop(0, '#ffffff');
        coreGrad.addColorStop(0.6, 'rgba(255, 255, 255, 0.8)');
        coreGrad.addColorStop(1, 'rgba(99, 102, 241, 0.2)');

        ctx.fillStyle = coreGrad;
        ctx.beginPath();
        ctx.arc(centerX, centerY, dynamicRadius * 0.45, 0, Math.PI * 2);
        ctx.fill();

        angle += 0.03;
        state.plasmaOrbAnimId = requestAnimationFrame(draw);
    }

    draw();
}

// ==================== Tasks & Reminders Manager ====================
async function fetchTasks() {
    try {
        const res = await fetch(`/api/v1/tasks/${state.sessionId}`);
        if (res.ok) {
            state.tasks = await res.json();
            renderTasksList();
        }
    } catch (e) {
        console.warn('Failed to load tasks', e);
    }
}

function renderTasksList() {
    const list = document.getElementById('tasksListContainer');
    const badge = document.getElementById('taskCountBadge');
    const remainingText = document.getElementById('tasksRemainingText');

    const remaining = state.tasks.filter(t => !t.completed).length;

    if (badge) badge.textContent = remaining;
    if (remainingText) remainingText.textContent = `${remaining} task${remaining === 1 ? '' : 's'} remaining`;

    if (!list) return;

    if (state.tasks.length === 0) {
        list.innerHTML = `<div style="text-align: center; padding: 30px; color: var(--text-dim); font-size: 0.88rem;">No tasks yet. Say "Add task: ..." to create one!</div>`;
        return;
    }

    list.innerHTML = state.tasks.map(t => `
        <div class="task-card-item ${t.completed ? 'completed' : ''}">
            <input type="checkbox" class="task-checkbox" ${t.completed ? 'checked' : ''} onchange="toggleTaskStatus('${t.id}')">
            <span class="task-text">${escapeHtml(t.title)}</span>
            <span class="task-priority-badge ${t.priority || 'medium'}">${t.priority || 'medium'}</span>
            <button class="task-delete-btn" onclick="deleteTaskItem('${t.id}')" title="Delete task">
                <i class="fas fa-trash"></i>
            </button>
        </div>
    `).join('');
}

async function addNewTaskFromInput() {
    const input = document.getElementById('newTaskInput');
    const prioritySelect = document.getElementById('taskPrioritySelect');
    const title = input?.value.trim();
    if (!title) return;

    try {
        const res = await fetch(`/api/v1/tasks/${state.sessionId}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title: title,
                priority: prioritySelect?.value || 'medium'
            })
        });

        if (res.ok) {
            input.value = '';
            sfx.playSend();
            fetchTasks();
            showToast('Task added', 'success');
        }
    } catch (e) {
        showToast('Failed to add task', 'error');
    }
}

function handleTaskInputKeyPress(event) {
    if (event.key === 'Enter') {
        addNewTaskFromInput();
    }
}

async function toggleTaskStatus(taskId) {
    try {
        const res = await fetch(`/api/v1/tasks/${state.sessionId}/${taskId}/toggle`, { method: 'PATCH' });
        if (res.ok) {
            const task = state.tasks.find(t => t.id === taskId);
            if (task) {
                task.completed = !task.completed;
                if (task.completed) sfx.playTaskComplete();
            }
            renderTasksList();
        }
    } catch (e) {
        showToast('Failed to update task', 'error');
    }
}

async function deleteTaskItem(taskId) {
    try {
        await fetch(`/api/v1/tasks/${state.sessionId}/${taskId}`, { method: 'DELETE' });
        state.tasks = state.tasks.filter(t => t.id !== taskId);
        renderTasksList();
        showToast('Task removed', 'info');
    } catch (e) {
        showToast('Failed to delete task', 'error');
    }
}

async function clearAllTasksCurrentSession() {
    if (!confirm('Clear all tasks for this session?')) return;
    try {
        await fetch(`/api/v1/tasks/${state.sessionId}`, { method: 'DELETE' });
        state.tasks = [];
        renderTasksList();
        showToast('All tasks cleared', 'info');
    } catch (e) {
        showToast('Failed to clear tasks', 'error');
    }
}

function openTasksModal() {
    document.getElementById('tasksModal').style.display = 'flex';
    fetchTasks();
}

function closeTasksModal() {
    document.getElementById('tasksModal').style.display = 'none';
}

// ==================== Settings Modal Management ====================
function openSettingsModal() {
    document.getElementById('settingsModal').style.display = 'flex';

    // Populate current settings
    document.getElementById('geminiApiKeyInput').value = state.settings.geminiApiKey;
    document.getElementById('geminiModelSelect').value = state.settings.geminiModel;
    document.getElementById('voiceRateSlider').value = state.settings.voiceRate;
    document.getElementById('voicePitchSlider').value = state.settings.voicePitch;
    document.getElementById('autoSpeakToggle').checked = state.settings.autoSpeak;
    document.getElementById('soundFxToggle').checked = state.settings.soundFx;
    document.getElementById('sttLangSelect').value = state.settings.sttLanguage;

    updateRateLabel(state.settings.voiceRate);
    updatePitchLabel(state.settings.voicePitch);
    populateVoiceSelectOptions();
}

function closeSettingsModal() {
    document.getElementById('settingsModal').style.display = 'none';
}

function saveGeminiApiKey() {
    const key = document.getElementById('geminiApiKeyInput')?.value.trim();
    state.settings.geminiApiKey = key;
    localStorage.setItem('va_gemini_api_key', key);
    showToast(key ? 'Gemini API Key saved' : 'API Key cleared', 'success');
}

function saveGeminiModel(model) {
    state.settings.geminiModel = model;
    localStorage.setItem('va_gemini_model', model);
}

function saveVoicePreference(voiceName) {
    state.settings.voiceName = voiceName;
    localStorage.setItem('va_voice_name', voiceName);
}

function updateRateLabel(val) {
    state.settings.voiceRate = parseFloat(val);
    document.getElementById('rateValLabel').textContent = `${val}x`;
    localStorage.setItem('va_voice_rate', val);
}

function updatePitchLabel(val) {
    state.settings.voicePitch = parseFloat(val);
    document.getElementById('pitchValLabel').textContent = val;
    localStorage.setItem('va_voice_pitch', val);
}

function saveAutoSpeakPref(val) {
    state.settings.autoSpeak = val;
    localStorage.setItem('va_auto_speak', val);
}

function saveSoundFxPref(val) {
    state.settings.soundFx = val;
    localStorage.setItem('va_sound_fx', val);
}

function saveSttLanguage(lang) {
    state.settings.sttLanguage = lang;
    localStorage.setItem('va_stt_lang', lang);
    if (state.recognition) {
        state.recognition.lang = lang;
    }
}

function toggleApiKeyVisibility() {
    const input = document.getElementById('geminiApiKeyInput');
    const icon = document.getElementById('apiKeyVisibilityIcon');
    if (input.type === 'password') {
        input.type = 'text';
        icon.className = 'fas fa-eye-slash';
    } else {
        input.type = 'password';
        icon.className = 'fas fa-eye';
    }
}

function saveAllSettingsAndClose() {
    saveGeminiApiKey();
    closeSettingsModal();
    showToast('Settings saved successfully', 'success');
}

// ==================== API Playground & Docs Tester ====================
let selectedEndpoint = 'health';

function openApiTesterModal() {
    document.getElementById('apiTesterModal').style.display = 'flex';
}

function closeApiTesterModal() {
    document.getElementById('apiTesterModal').style.display = 'none';
}

function selectApiEndpoint(cardEl, endpointKey) {
    document.querySelectorAll('.endpoint-card').forEach(c => c.classList.remove('active'));
    cardEl.classList.add('active');

    selectedEndpoint = endpointKey;
    const methodTag = document.getElementById('currentApiMethod');
    const urlInput = document.getElementById('currentApiUrl');
    const payloadSec = document.getElementById('apiPayloadSection');
    const payloadTextarea = document.getElementById('apiPayloadTextarea');

    if (endpointKey === 'health') {
        methodTag.textContent = 'GET';
        urlInput.value = '/api/v1/health';
        payloadSec.style.display = 'none';
    } else if (endpointKey === 'status') {
        methodTag.textContent = 'GET';
        urlInput.value = '/api/v1/status';
        payloadSec.style.display = 'none';
    } else if (endpointKey === 'personas') {
        methodTag.textContent = 'GET';
        urlInput.value = '/api/v1/chat/personas';
        payloadSec.style.display = 'none';
    } else if (endpointKey === 'sessions') {
        methodTag.textContent = 'GET';
        urlInput.value = '/api/v1/chat/sessions';
        payloadSec.style.display = 'none';
    } else if (endpointKey === 'chat') {
        methodTag.textContent = 'POST';
        urlInput.value = '/api/v1/chat/message';
        payloadSec.style.display = 'flex';
        payloadTextarea.value = JSON.stringify({
            sessionId: state.sessionId,
            message: "What is the weather in Tokyo?",
            persona: "jarvis"
        }, null, 2);
    } else if (endpointKey === 'voice') {
        methodTag.textContent = 'POST';
        urlInput.value = '/api/v1/voice/process';
        payloadSec.style.display = 'flex';
        payloadTextarea.value = JSON.stringify({
            type: "text",
            content: "Hello from API Tester",
            language: "en",
            sessionId: state.sessionId
        }, null, 2);
    } else if (endpointKey === 'tasks') {
        methodTag.textContent = 'GET';
        urlInput.value = `/api/v1/tasks/${state.sessionId}`;
        payloadSec.style.display = 'none';
    }
}

async function executeSelectedApi() {
    const url = document.getElementById('currentApiUrl').value;
    const method = document.getElementById('currentApiMethod').textContent;
    const viewer = document.getElementById('apiJsonViewer');
    const timeBadge = document.getElementById('apiResponseTimeBadge');

    viewer.textContent = 'Executing request...';

    const t0 = performance.now();
    try {
        const options = {
            method: method,
            headers: { 'Content-Type': 'application/json' }
        };

        if (method === 'POST') {
            options.body = document.getElementById('apiPayloadTextarea').value;
        }

        const res = await fetch(url, options);
        const data = await res.json();
        const t1 = performance.now();

        timeBadge.textContent = `${Math.round(t1 - t0)} ms`;
        viewer.textContent = JSON.stringify(data, null, 2);
    } catch (e) {
        viewer.textContent = `Error: ${e.message}`;
        timeBadge.textContent = 'failed';
    }
}

// ==================== Live Telemetry Heartbeat ====================
async function startTelemetryHeartbeat() {
    async function pollStatus() {
        const t0 = performance.now();
        try {
            const res = await fetch('/api/v1/status');
            if (res.ok) {
                const data = await res.json();
                const t1 = performance.now();

                const pingEl = document.getElementById('telemetryPing');
                const uptimeEl = document.getElementById('telemetryUptime');
                const memEl = document.getElementById('telemetryMemory');

                if (pingEl) pingEl.textContent = `${Math.round(t1 - t0)}ms`;
                if (data.data) {
                    if (uptimeEl) uptimeEl.textContent = data.data.uptime || '--';
                    if (memEl) memEl.textContent = `${data.data.memoryUsedMb || 0} MB`;
                }
            }
        } catch (e) {
            // Server might be reloading
        }
    }

    pollStatus();
    setInterval(pollStatus, 8000);
}

// ==================== Toast Notifications & Utilities ====================
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `app-toast ${type}`;

    let icon = 'fas fa-info-circle';
    if (type === 'success') icon = 'fas fa-check-circle';
    if (type === 'error') icon = 'fas fa-circle-exclamation';

    toast.innerHTML = `<i class="${icon}"></i> <span>${escapeHtml(message)}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(40px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3200);
}

function copyTextToClipboard(text) {
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => {
        showToast('Copied to clipboard!', 'success');
    }).catch(() => {
        showToast('Failed to copy', 'error');
    });
}

function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/[&<>"']/g, m => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    }[m]));
}

function escapeForAttribute(text) {
    if (!text) return '';
    return text.replace(/'/g, "\\'").replace(/"/g, '&quot;').replace(/\n/g, ' ');
}

// ==================== Markdown Parser ====================
function parseMarkdown(md) {
    if (!md) return '';

    let out = md;

    // 1. Code blocks with copy button
    out = out.replace(/```([a-zA-Z0-9_-]*)\n([\s\S]*?)```/g, (match, lang, code) => {
        const cleanLang = lang || 'code';
        const escapedCode = escapeHtml(code.trim());
        const rawEscaped = encodeURIComponent(code.trim());
        return `
            <div class="code-block-wrapper">
                <div class="code-block-header">
                    <span>${cleanLang.toUpperCase()}</span>
                    <button class="copy-code-btn" onclick="copyRawCode(this, '${rawEscaped}')">
                        <i class="fas fa-copy"></i> Copy Code
                    </button>
                </div>
                <pre><code>${escapedCode}</code></pre>
            </div>
        `;
    });

    // 2. Inline code
    out = out.replace(/`([^`]+)`/g, '<code style="background: rgba(255,255,255,0.1); padding: 2px 6px; border-radius: 4px; font-size: 0.88rem;">$1</code>');

    // 3. Bold & Italic
    out = out.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    out = out.replace(/\*([^*]+)\*/g, '<em>$1</em>');

    // 4. Blockquotes
    out = out.replace(/^>\s*(.+)$/gm, '<blockquote>$1</blockquote>');

    // 5. Unordered Lists
    out = out.replace(/^[•\-\*]\s+(.+)$/gm, '<li>$1</li>');
    out = out.replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>');

    // 6. Line breaks to paragraphs
    out = out.split('\n\n').map(p => {
        if (p.startsWith('<div class="code-block-wrapper"') || p.startsWith('<ul>') || p.startsWith('<blockquote>')) {
            return p;
        }
        return `<p>${p.replace(/\n/g, '<br>')}</p>`;
    }).join('');

    return out;
}

function copyRawCode(btnEl, encodedCode) {
    const raw = decodeURIComponent(encodedCode);
    navigator.clipboard.writeText(raw).then(() => {
        const origText = btnEl.innerHTML;
        btnEl.innerHTML = '<i class="fas fa-check"></i> Copied!';
        setTimeout(() => {
            btnEl.innerHTML = origText;
        }, 2000);
        showToast('Code copied to clipboard', 'success');
    });
}
