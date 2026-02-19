<script>
    import { onMount } from "svelte";
    import { fly } from "svelte/transition";

    let { combo = $bindable(0), maxCombo = $bindable(0) } = $props();

    let commands = $state([]);
    let currentCommandIndex = $state(0);
    let currentText = $derived(commands[currentCommandIndex] || "Loading...");
    let input = $state("");
    let startTime = $state(0);
    let isPlaying = $state(false);

    // Session State
    let sessionId = $state("");
    let username = $state("Guest");
    let typedHistory = $state([]); // Stores completed commands

    // Time tracking for WPM
    let now = $state(Date.now());

    $effect(() => {
        if (isPlaying) {
            const interval = setInterval(() => {
                now = Date.now();
            }, 500);
            return () => clearInterval(interval);
        }
    });

    let timeElapsed = $derived(isPlaying ? (now - startTime) / 1000 / 60 : 0);
    let charsTyped = $state(0);
    let wpm = $derived(
        timeElapsed > 0 ? Math.round(charsTyped / 5 / timeElapsed) : 0,
    );

    async function startGame() {
        username = prompt("Enter your username:", "Guest") || "Guest";

        try {
            const res = await fetch("http://localhost:8080/api/game/start", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ username })
            });
            const data = await res.json();

            sessionId = data.sessionId;
            commands = data.commands;
            // data.signature is available but unused for now

            isPlaying = true;
            startTime = Date.now();
            input = "";
            combo = 0;
            maxCombo = 0;
            charsTyped = 0;
            currentCommandIndex = 0;
            typedHistory = [];

            startHeartbeat();
        } catch (e) {
            console.error("Failed to start game", e);
            alert("Failed to start game!");
        }
    }

    let heartbeatInterval;

    function startHeartbeat() {
        if (heartbeatInterval) clearInterval(heartbeatInterval);
        heartbeatInterval = setInterval(async () => {
            if (!isPlaying) return;
            try {
                await fetch("http://localhost:8080/api/game/heartbeat", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ sessionId, progress: charsTyped })
                });
            } catch (e) {
                console.error("Heartbeat failed", e);
            }
        }, 2000);
    }

    let lastCorrectLength = $state(0);
    let isShaking = $state(false);

    function triggerShake() {
        isShaking = true;
        setTimeout(() => (isShaking = false), 300);
    }

    function handleInput(e) {
        const val = e.target.value;

        // Check correctness
        if (currentText.startsWith(val)) {
            // Correct so far
            if (val.length > lastCorrectLength) {
                // New correct char typed
                combo++;
                if (combo > maxCombo) maxCombo = combo;
                charsTyped++;
            }
            lastCorrectLength = val.length;

            // Check completion
            if (val === currentText) {
                typedHistory.push(val); // Add completed command
                currentCommandIndex++;
                input = "";
                lastCorrectLength = 0;
                if (currentCommandIndex >= commands.length) {
                    finishGame();
                }
                return;
            }
        } else {
            // Wrong
            combo = 0;
            triggerShake();
        }
        input = val;
    }

    async function generateSignature(data) {
        const secret = "dev-secret"; // Hardcoded deterrent
        const enc = new TextEncoder();
        const key = await crypto.subtle.importKey(
            "raw",
            enc.encode(secret),
            { name: "HMAC", hash: "SHA-256" },
            false,
            ["sign"]
        );
        const signature = await crypto.subtle.sign(
            "HMAC",
            key,
            enc.encode(data)
        );
        return btoa(String.fromCharCode(...new Uint8Array(signature)));
    }

    async function finishGame() {
        isPlaying = false;
        clearInterval(heartbeatInterval);

        // Construct full typed text
        const fullTypedText = typedHistory.join("") + input;

        // Sign payload
        const payloadToSign = sessionId + fullTypedText;
        let sig = "";
        try {
            sig = await generateSignature(payloadToSign);
        } catch(e) {
            console.error("Signing failed", e);
        }

        const score = {
            sessionId,
            username,
            typedText: fullTypedText,
            signature: sig
        };

        try {
            const res = await fetch("http://localhost:8080/api/game/submit", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(score),
            });

            if (!res.ok) {
                const errText = await res.text();
                throw new Error(errText);
            }

            const result = await res.json();
            alert(`Game Over! WPM: ${result.wpm}, Max Combo: ${result.maxCombo}`);

            commands = [];
        } catch (e) {
            console.error(e);
            alert("Failed to submit score: " + e.message);
        }
    }

    // Effect to handle focus
    let inputEl = $state();
    $effect(() => {
        if (isPlaying && inputEl) {
            inputEl.focus();
        }
    });

    let isDark = $state(false);

    function toggleTheme() {
        isDark = !isDark;
        if (isDark) {
            document.body.classList.add('dark-theme');
        } else {
            document.body.classList.remove('dark-theme');
        }
    }
</script>

<div class="game-container">
    <button class="theme-toggle" onclick={toggleTheme}>
        {isDark ? '☀️' : '🌙'}
    </button>
    {#if !isPlaying}
        <div class="start-screen">
            <button onclick={startGame}>Start Game</button>
        </div>
    {:else}
        <div class="stats">
            <div class="stat">WPM: {wpm}</div>
            <div class="stat">Accuracy: 100%</div>
            <!-- Placeholder -->
        </div>

        <div class="command-queue">
            {#key currentCommandIndex}
                <div
                    class="queue-item next"
                    in:fly={{ y: -20, duration: 300, delay: 50 }}
                >
                    {commands[currentCommandIndex + 1] || "..."}
                </div>

                <div
                    class="queue-item current"
                    in:fly={{ y: -20, duration: 300 }}
                    out:fly={{ y: 20, duration: 300 }}
                >
                    {#each currentText.split("") as char, i}
                        <span
                            class:correct={i < input.length &&
                                input[i] === char}
                            class:wrong={i < input.length && input[i] !== char}
                            class:pending={i >= input.length}
                        >
                            {char}
                        </span>
                    {/each}
                </div>
            {/key}
        </div>

        <input
            bind:this={inputEl}
            type="text"
            value={input}
            oninput={handleInput}
            class="input-box"
            class:shake={isShaking}
        />
        <p class="hint">Type the command above!</p>
    {/if}
</div>

<style>
    .game-container {
        position: relative;
        display: flex;
        flex-direction: column;
        align-items: center;
        background: var(--container-bg);
        padding: 2rem;
        border-radius: 8px;
        min-height: 400px; /* Increased height for vertical queue */
        justify-content: center;
        overflow: hidden;
    }

    .theme-toggle {
        position: absolute;
        top: 1rem;
        right: 1rem;
        font-size: 1.2rem;
        padding: 0.5rem;
        background: transparent;
        border: 1px solid var(--border-color);
        color: var(--text-color);
        border-radius: 50%;
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        z-index: 100;
    }

    .theme-toggle:hover {
        background: var(--input-bg);
    }

    .command-queue {
        position: relative;
        width: 100%;
        height: 150px; /* Fixed height for the queue area */
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        margin: 2rem 0;
    }

    .queue-item {
        font-family: "Fira Code", monospace;
        text-align: center;
        white-space: pre-wrap;
        word-break: break-all;
        max-width: 100%;
        color: var(--text-color);
    }

    .queue-item.next {
        position: absolute;
        top: 10px;
        font-size: 1.2rem;
        color: var(--queue-next-color);
        opacity: 0.6;
        /* Next item doesn't need detailed character spans, just text */
    }

    .queue-item.current {
        font-size: 2rem;
        letter-spacing: 2px;
        z-index: 10;
        margin-top: 2rem; /* Push down slightly to make room for next */
    }

    .correct {
        color: var(--success-color);
        display: inline-block;
        animation: pop 0.1s ease-out;
    }
    .wrong {
        color: var(--error-color);
        background: var(--error-bg);
    }
    .pending {
        color: var(--pending-color); /* Lighter pending text */
    }

    .input-box {
        font-size: 1.5rem;
        padding: 10px;
        width: 80%;
        text-align: center;
        border: 2px solid var(--highlight-color);
        border-radius: 4px;
        background: var(--input-bg);
        color: var(--input-text);
        transition: border-color 0.2s;
        z-index: 20;
    }

    .shake {
        animation: shake 0.3s;
        border-color: var(--error-color);
    }

    @keyframes shake {
        0%,
        100% {
            transform: translateX(0);
        }
        25% {
            transform: translateX(-5px);
        }
        75% {
            transform: translateX(5px);
        }
    }

    @keyframes pop {
        0% {
            transform: scale(1);
        }
        50% {
            transform: scale(1.3);
        }
        100% {
            transform: scale(1);
        }
    }

    .stats {
        display: flex;
        gap: 2rem;
        font-size: 1.2rem;
        color: var(--stats-color);
    }
    button {
        font-size: 1.5rem;
        padding: 1rem 2rem;
    }
</style>
