// ==================== Global Variables ====================
let socket = null;
let messageCount = 0;
let sessionStartTime = Date.now();
let lastResponseTime = 0;

// ==================== WebSocket Management ====================
function connectWebSocket() {
    if (socket && socket.readyState === WebSocket.OPEN) {
        addMessage('WebSocket already connected', 'system');
        return;
    }

    try {
        const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
        socket = new WebSocket(`${protocol}//${window.location.host}/ws/voice`);

        socket.onopen = () => {
            updateWebSocketStatus(true);
            addMessage('WebSocket connected successfully!', 'system');
            console.log('WebSocket connected');
        };

        socket.onmessage = (event) => {
            try {
                const response = JSON.parse(event.data);
                displayMessage(response, 'assistant');
                lastResponseTime = Date.now();
                updateResponseTime();
            } catch (e) {
                console.error('Error parsing response:', e);
                addMessage('Error: Invalid response format', 'system');
            }
        };

        socket.onerror = (error) => {
            console.error('WebSocket error:', error);
            updateWebSocketStatus(false);
            addMessage('WebSocket error occurred', 'system');
        };

        socket.onclose = () => {
            updateWebSocketStatus(false);
            addMessage('WebSocket disconnected', 'system');
            console.log('WebSocket closed');
        };
    } catch (error) {
        console.error('Failed to connect WebSocket:', error);
        addMessage('Failed to connect WebSocket', 'system');
        updateWebSocketStatus(false);
    }
}

function updateWebSocketStatus(connected) {
    const statusBadge = document.getElementById('wsStatus');
    const statusText = document.getElementById('wsStatusText');
    const statusIcon = document.getElementById('wsStatusIcon');
    const statusDetail = document.getElementById('wsStatusDetail');

    if (connected) {
        statusBadge.className = 'status-badge connected';
        statusText.textContent = 'WebSocket Connected';
        statusIcon.className = 'status-icon success';
        statusDetail.textContent = 'Connected';
    } else {
        statusBadge.className = 'status-badge disconnected';
        statusText.textContent = 'WebSocket Disconnected';
        statusIcon.className = 'status-icon';
        statusDetail.textContent = 'Disconnected';
    }
}

// ==================== Message Handling ====================
function sendTextMessage() {
    const input = document.getElementById('textInput');
    const message = input.value.trim();

    if (!message) return;

    addMessage(message, 'user');
    input.value = '';
    messageCount++;
    updateMessageCount();

    handleLocalOpenCommand(message);

    if (socket && socket.readyState === WebSocket.OPEN) {
        const voiceMessage = {
            type: 'text',
            content: message,
            language: 'en',
            sessionId: getSessionId(),
            timestamp: Date.now()
        };
        socket.send(JSON.stringify(voiceMessage));
        lastResponseTime = Date.now();
    } else {
        // Fallback to REST API if WebSocket is not connected
        sendViaREST(message);
    }
}

function sendQuickMessage(message) {
    const input = document.getElementById('textInput');
    input.value = message;
    sendTextMessage();
}

function sendViaREST(message) {
    const voiceMessage = {
        type: 'text',
        content: message,
        language: 'en',
        sessionId: getSessionId()
    };

    fetch('/api/v1/voice/process', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(voiceMessage)
    })
    .then(response => response.json())
    .then(data => {
        displayMessage(data, 'assistant');
        updateResponseTime();
    })
    .catch(error => {
        console.error('Error:', error);
        addMessage('Error: Failed to process message', 'system');
    });
}

function addMessage(content, type) {
    const messagesContainer = document.getElementById('messages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${type}`;

    let icon = '';
    if (type === 'system') {
        icon = '<i class="fas fa-info-circle"></i>';
    } else if (type === 'user') {
        icon = '<i class="fas fa-user"></i>';
    } else if (type === 'assistant') {
        icon = '<i class="fas fa-robot"></i>';
    }

    if (type === 'system') {
        messageDiv.innerHTML = `${icon}<span>${escapeHtml(content)}</span>`;
    } else {
        messageDiv.innerHTML = `${icon}<div class="content">${escapeHtml(content)}</div>`;
    }

    messagesContainer.appendChild(messageDiv);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

function displayMessage(responseObj, type) {
    if (responseObj.status === 'success') {
        const message = responseObj.message || responseObj.data || 'Response received';
        addMessage(message, type);
        handleAssistantAction(responseObj);
        if (type === 'assistant') {
            speakMessage(message);
        }
    } else {
        const errorMsg = responseObj.message || 'Unknown error';
        addMessage(`Error: ${errorMsg}`, 'system');
    }
}

function getSessionId() {
    let sessionId = sessionStorage.getItem('voiceAssistSessionId');
    if (!sessionId) {
        sessionId = 'session-' + Date.now();
        sessionStorage.setItem('voiceAssistSessionId', sessionId);
    }
    return sessionId;
}

function handleKeyPress(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendTextMessage();
    }
}

// ==================== API Testing ====================
function testEndpoint(endpoint, method) {
    const responseBox = document.getElementById('apiResponse');
    responseBox.textContent = 'Loading...';

    fetch(endpoint, {
        method: method
    })
    .then(response => response.json())
    .then(data => {
        responseBox.textContent = JSON.stringify(data, null, 2);
        addMessage(`API test completed: ${endpoint}`, 'system');
    })
    .catch(error => {
        responseBox.textContent = `Error: ${error.message}`;
        console.error('Error:', error);
    });
}

function testVoiceEndpoint() {
    const responseBox = document.getElementById('apiResponse');
    responseBox.textContent = 'Loading...';

    const testData = {
        type: 'text',
        content: 'Test message',
        language: 'en',
        sessionId: getSessionId()
    };

    fetch('/api/v1/voice/process', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(testData)
    })
    .then(response => response.json())
    .then(data => {
        responseBox.textContent = JSON.stringify(data, null, 2);
        addMessage('Voice process API test completed', 'system');
    })
    .catch(error => {
        responseBox.textContent = `Error: ${error.message}`;
        console.error('Error:', error);
    });
}

function testRecognizeEndpoint() {
    const responseBox = document.getElementById('apiResponse');
    responseBox.textContent = 'Loading...';

    const testData = {
        type: 'audio',
        content: 'base64encodedaudio',
        language: 'en',
        sessionId: getSessionId()
    };

    fetch('/api/v1/voice/recognize', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(testData)
    })
    .then(response => response.json())
    .then(data => {
        responseBox.textContent = JSON.stringify(data, null, 2);
        addMessage('Voice recognize API test completed', 'system');
    })
    .catch(error => {
        responseBox.textContent = `Error: ${error.message}`;
        console.error('Error:', error);
    });
}

// ==================== Statistics ====================
function updateMessageCount() {
    document.getElementById('messageCount').textContent = messageCount;
}

function updateSessionTime() {
    const elapsed = Math.floor((Date.now() - sessionStartTime) / 1000);
    const minutes = Math.floor(elapsed / 60);
    const seconds = elapsed % 60;
    const timeString = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
    document.getElementById('sessionTime').textContent = timeString;
}

function updateResponseTime() {
    if (lastResponseTime > 0) {
        const now = Date.now();
        const timeDiff = now - lastResponseTime;
        document.getElementById('responseTime').textContent = `${timeDiff}ms`;
    }
}

// ==================== Utilities ====================
function scrollToSection(sectionId) {
    const element = document.getElementById(sectionId);
    if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
    }
}

function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

function handleLocalOpenCommand(message) {
    const normalized = message.toLowerCase();

    if (normalized.includes('open youtube') || normalized === 'youtube' || normalized.includes('go to youtube')) {
        window.open('https://www.youtube.com', '_blank', 'noopener,noreferrer');
        addMessage('Opening YouTube in a new tab.', 'system');
        return true;
    }

    if (normalized.includes('open spotify') || normalized === 'spotify' || normalized.includes('go to spotify')) {
        window.open('https://open.spotify.com', '_blank', 'noopener,noreferrer');
        addMessage('Opening Spotify in a new tab.', 'system');
        return true;
    }

    return false;
}

function handleAssistantAction(responseObj) {
    if (!responseObj || !responseObj.data || typeof responseObj.data !== 'object') {
        return;
    }

    if (responseObj.data.openUrl) {
        addMessage(`Action ready: ${responseObj.data.label || responseObj.data.openUrl}`, 'system');
    }
}

function speakMessage(text) {
    if (!('speechSynthesis' in window) || !text) {
        return;
    }

    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = 'en-US';
    utterance.rate = 1;
    utterance.pitch = 1;
    utterance.volume = 1;

    window.speechSynthesis.speak(utterance);
}

// ==================== Initialization ====================
document.addEventListener('DOMContentLoaded', () => {
    // Auto-connect WebSocket on load
    setTimeout(() => {
        connectWebSocket();
    }, 500);

    // Update session time every second
    setInterval(updateSessionTime, 1000);

    // Add welcome message
    addMessage('Welcome to Voice Assist AI! Type a message or connect via WebSocket.', 'system');

    console.log('Application initialized');
});

// ==================== Keyboard Shortcuts ====================
document.addEventListener('keydown', (event) => {
    // Ctrl+Enter to connect/disconnect WebSocket
    if (event.ctrlKey && event.key === 'Enter') {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.close();
        } else {
            connectWebSocket();
        }
    }
});
