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
            #chatbot-bar, #chatbot-inner-card, #chatbot-form, #chatbot-messages { max-width: 100vw; }
            #chatbot-inner-card { padding: 16px 4px 12px 4px; }
        }
    `;
    document.head.appendChild(style);

    // Remove fade-in and typing animation logic for greeting and placeholder
    // Only create chat area and handle messages
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

    // Handle sending message
    const form = bar.querySelector('#chatbot-form');
    const input = bar.querySelector('#chatbot-input');
    const messages = bar.querySelector('#chatbot-messages');

    // Autofocus the chat input when the chatbot loads
    setTimeout(() => { input.focus(); }, 0);

    let lastFollowUp = null; // Track follow-up context

    form.onsubmit = async function(e) {
        e.preventDefault();
        const msg = input.value.trim();
        if (!msg) return;
        appendMessage('user', msg);
        input.value = '';
        appendMessage('bot', '...');
        // Animate the '...' loading message
        const botMsg = messages.lastChild;
        let loading = true;
        let dots = 0;
        function animateDots() {
            if (!loading) return;
            dots = (dots % 3) + 1;
            botMsg.textContent = '.'.repeat(dots);
            setTimeout(animateDots, 350);
        }
        animateDots();
        try {
            // Send lastFollowUp if present
            const body = { message: msg };
            if (lastFollowUp) body.lastFollowUp = lastFollowUp;
            const resp = await fetch('/api/chatbot', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
            const data = await resp.json();
            const reply = data.reply || data.error || 'No response.';
            // Typing animation for bot reply
            loading = false;
            botMsg.textContent = '';
            let i = 0;
            function typeBot() {
                if (i <= reply.length) {
                    botMsg.textContent = reply.slice(0, i);
                    i++;
                    setTimeout(typeBot, 10);
                }
            }
            typeBot();
            // Store lastFollowUp for next turn
            lastFollowUp = data.lastFollowUp || null;
        } catch (err) {
            loading = false;
            messages.lastChild.textContent = 'Error contacting AI.';
        }
        messages.scrollTop = messages.scrollHeight;
    };
    function appendMessage(role, text) {
        const div = document.createElement('div');
        div.className = 'chatbot-msg ' + role;
        div.textContent = text;
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
    }
})(); 