// Wait for marked.js to be loaded before running the chatbot logic
(function waitForMarked() {
    if (typeof marked === 'undefined') {
        setTimeout(waitForMarked, 50);
        return;
    }

    // Modern Responsive Chatbot Bar
    (function() {
        // Style for chat container and bubbles
        const style = document.createElement('style');
        style.textContent = `
            #chatbot-bar-section {
                width: 100%;
                display: flex;
                flex-direction: column;
                align-items: center;
                margin-bottom: 32px;
            }
            #chatbot-bar {
                width: 100%;
                max-width: 1200px;
            }
            #chatbot-inner-card {
                width: 100%;
                background: #fff;
                border-radius: 24px;
                box-shadow: 0 4px 32px rgba(0,0,0,0.10);
                padding: 32px 32px 24px 32px;
                display: flex;
                flex-direction: column;
                align-items: stretch;
                box-sizing: border-box;
            }
            #chatbot-messages {
                min-height: 80px;
                margin-bottom: 18px;
                display: flex;
                flex-direction: column;
                gap: 10px;
                width: 100%;
            }
            .chatbot-msg {
                max-width: 70%;
                padding: 14px 20px;
                border-radius: 18px;
                font-size: 1.08rem;
                line-height: 1.5;
                word-break: break-word;
                margin-bottom: 0;
            }
            .chatbot-msg.user {
                background: #e3f0ff;
                color: #222;
                align-self: flex-end;
                border-bottom-right-radius: 6px;
            }
            .chatbot-msg.bot {
                background: #f6f6f6;
                color: #222;
                align-self: flex-start;
                border-bottom-left-radius: 6px;
            }
            #chatbot-form {
                display: flex;
                align-items: center;
                gap: 8px;
                background: #f5f7fa;
                border-radius: 32px;
                padding: 8px 16px 8px 24px;
                box-shadow: 0 2px 8px rgba(0,0,0,0.04);
                width: 100%;
                margin: 0 auto;
                box-sizing: border-box;
            }
            #chatbot-input {
                flex: 1;
                border: none;
                background: transparent;
                font-size: 1.15rem;
                padding: 16px 0;
                outline: none;
            }
            #chatbot-form button {
                background: #e9e9ee;
                border: none;
                border-radius: 50%;
                width: 44px;
                height: 44px;
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;
                transition: background 0.2s;
                padding: 0;
            }
            #chatbot-form button:hover {
                background: #d1d1d8;
            }
            #chatbot-form button svg {
                width: 22px;
                height: 22px;
                display: block;
            }
            @media (max-width: 900px) {
                #chatbot-bar-section > * { max-width: 100vw; }
                #chatbot-inner-card { padding: 16px 4px 12px 4px; }
            }
            .fake-title {
                margin-top: 1rem;
                margin-bottom: 0.25rem;
                font-weight: bold;
                display: block;
            }
            .fake-bullet {
                display: block; 
                margin: 0.5rem 0 0.5rem 1.2rem;
                text-indent: -1.2rem;
                padding-left: 1.2rem;
                line-height: 1.6;
            }
        `;
        document.head.appendChild(style);

        // Build chat UI
        const bar = document.createElement('div');
        bar.id = 'chatbot-inner-card';
        bar.innerHTML = `
            <div id="chatbot-messages"></div>
            <form id="chatbot-form">
                <input type="text" id="chatbot-input" placeholder="" autocomplete="off" />
                <button type="submit" aria-label="Send">
                    <svg viewBox="0 0 24 24" fill="none" stroke="#222" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="11" fill="#e9e9ee"/><path d="M8 12h8M13 8l4 4-4 4" stroke="#222"/></svg>
                </button>
            </form>
        `;
        document.getElementById('chatbot-bar').appendChild(bar);

        const form = bar.querySelector('#chatbot-form');
        const input = bar.querySelector('#chatbot-input');
        const messages = bar.querySelector('#chatbot-messages');
        setTimeout(() => { input.focus(); }, 0);
        let lastFollowUp = null;

        // Add CSS for loading text and shimmer effect
        const styleLoading = document.createElement('style');
        styleLoading.textContent = `
        .loading-text {
            font-weight: bold;
            font-size: 1.05rem;
            font-family: monospace;
            background: linear-gradient(90deg, #888, #111, #888);
            background-size: 200% auto;
            color: transparent;
            background-clip: text;
            -webkit-background-clip: text;
            animation: shimmer 2.5s linear infinite;
        }
        @keyframes shimmer {
            0% { background-position: 200% center; }
            100% { background-position: -200% center; }
        }
        `;
        document.head.appendChild(styleLoading);

        let loadingInterval = null;
        function showLoading() {
            const div = document.createElement('div');
            div.className = 'chatbot-msg bot loading';
            div.setAttribute('id', 'loading-msg');
            div.innerHTML = '<span class="loading-text">Researching<span class="dots">.</span></span>';
            messages.appendChild(div);
            let dotCount = 1;
            loadingInterval = setInterval(() => {
                const dots = div.querySelector('.dots');
                dotCount = (dotCount % 3) + 1;
                dots.textContent = '.'.repeat(dotCount);
            }, 500);
        }
        function hideLoading() {
            clearInterval(loadingInterval);
            const loadingDiv = document.getElementById('loading-msg');
            if (loadingDiv) loadingDiv.remove();
        }

        const sendButton = form.querySelector('button');
        let isGenerating = false;
        let stopTyping = false;

        // Helper to set button state
        function setButtonState(state) {
            if (state === 'pause') {
                sendButton.innerHTML = '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="#222" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="6" y="6" width="12" height="12" rx="2" fill="#e9e9ee" stroke="#222"/></svg>';
                sendButton.disabled = false;
                // input.disabled = true; // Do not disable input bar
            } else {
                sendButton.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="#222" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="11" fill="#e9e9ee"/><path d="M8 12h8M13 8l4 4-4 4" stroke="#222"/></svg>';
                sendButton.disabled = false;
                input.disabled = false;
            }
        }

        // Add click handler for stop button
        sendButton.addEventListener('click', function(e) {
            if (isGenerating) {
                stopTyping = true;
            }
        });

        // Modify form.onsubmit to handle generation state
        const originalOnSubmit = form.onsubmit;
        form.onsubmit = async function(e) {
            e.preventDefault();
            if (isGenerating) {
                stopTyping = true;
                return;
            }
            const msg = input.value.trim();
            if (!msg) return;
            appendMessage('user', msg);
            input.value = '';
            isGenerating = true;
            stopTyping = false;
            setButtonState('pause');
            showLoading();
            try {
                const body = { message: msg };
                if (lastFollowUp) body.lastFollowUp = lastFollowUp;
                const resp = await fetch('/api/chatbot', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(body)
                });
                const data = await resp.json();
                const reply = data.reply || data.error || 'No response.';
                lastFollowUp = data.lastFollowUp || null;
                hideLoading();
                appendMessage('bot', reply);
            } catch (err) {
                hideLoading();
                appendMessage('bot', 'Error contacting AI.');
                console.error("Actual caught error:", err);
            }
            setButtonState('send');
            isGenerating = false;
            messages.scrollTop = messages.scrollHeight;
        };

        function appendMessage(role, text) {
            const div = document.createElement('div');
            div.className = 'chatbot-msg ' + role;
        
            if (role === 'bot') {
                let normalized = text.replace(/\r\n|\r/g, '\n');

                // Remove useless bullets
                normalized = normalized.replace(/^\s*-\s*\*\*\s*$/gm, '');
                normalized = normalized.replace(/^\s*-\s*$/gm, '');
                
                // Fix nested GPT-style double bullets
                normalized = normalized.replace(/^\s*•\s*/gm, '- ');
                
                 // Handle - **1. Title:** Reason and normalize it to Title + Bullet
                normalized = normalized.replace(/- \*\*(\d+)\.\s*([^*]+?)\*\*:?\s*([^\n]*)/g, function(_, num, title, expl) {
                    const cleanTitle = title.trim().replace(/:+$/, '');
                    const boldTitle = `**${num}. ${cleanTitle}:**`;
                    return `${boldTitle}${expl.trim()}`;
                });
                
                // Prevent triple line breaks
                normalized = normalized.replace(/\n{3,}/g, '\n\n');
                
                const markdown = normalized.trim();
                let i = 0;
                const tempSpan = document.createElement('span');
                div.appendChild(tempSpan);
                messages.appendChild(div);
        
                function typePlain() {
                    let current = markdown.slice(0, i);
                
                    current = current
                        .replace(/\*\*(\d+)\.\s*([^*]+?)\*\*:?\s*([^\n]*)/g, (_match, num, title, rest) => {
                            return `<div class="fake-title"><strong>${num}. ${title.trim()}:</strong> ${rest.trim()}</div>`;
                        }) // numbered titles
                        .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')                                    // bold
                        // .replace(/\*(.*?)\*/g, '<em>$1</em>')                                                // italic
                        .replace(/^-\s+(.*)/gm, '<div class="fake-bullet">• $1</div>')                        // bullets
                        .replace(/\n\s*\n/g, '</p><p>')
                        .replace(/\n/g, '<br>');
                
                    current = `<p>${current}</p>`;
                
                    tempSpan.innerHTML = current;
                    i++;
                
                    if (i <= markdown.length) {
                        setTimeout(typePlain, 5);
                    } else {
                        div.innerHTML = marked.parse(markdown); // Final render
                    }
                }
        
                typePlain();
            } else {
                div.textContent = text;
                messages.appendChild(div);
            }
        
            messages.scrollTop = messages.scrollHeight;
        }

        // Add CSS for chatbot bot message formatting
        const styleBotMsg = document.createElement('style');
        styleBotMsg.textContent = `
        .chatbot-msg.bot ul {
            list-style: disc;
            padding-left: 1.2rem;
            margin: 1rem 0;
        }
        .chatbot-msg.bot li {
            margin-bottom: 8px;
            line-height: 1.6;
        }
        .chatbot-msg.bot p {
            margin-top: 1rem;
            font-style: normal;
        }
        .chatbot-msg.bot p strong {
            font-style: italic;
        }
        `;
        document.head.appendChild(styleBotMsg);
    })();
})();
