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

    // Fetch commands
    onMount(async () => {
        try {
            const res = await fetch(
                "http://localhost:8080/api/commands?count=10",
            );
            commands = await res.json();
        } catch (e) {
            console.error("Failed to fetch commands", e);
            commands = ["git status", "docker ps", "mvn clean install"];
        }
    });

    function startGame() {
        isPlaying = true;
        startTime = Date.now();
        input = "";
        combo = 0;
        maxCombo = 0;
        charsTyped = 0;
        currentCommandIndex = 0;
        // Focus logic can be handled via directive or effect
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
            // Don't reset lastCorrectLength completely?
            // Logic: if user typed "docka" instead of "docker", lastCorrectLength was 4.
            // Now it's wrong.
        }
        input = val;
    }

    async function finishGame() {
        const finalWpm = wpm;
        isPlaying = false;
        const score = {
            username: prompt("Enter your username:", "Guest") || "Guest",
            wpm: finalWpm,
            maxCombo: maxCombo,
        };

        try {
            await fetch("http://localhost:8080/api/score", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(score),
            });
            // Refresh commands for next game
            const res = await fetch(
                "http://localhost:8080/api/commands?count=10",
            );
            commands = await res.json();
        } catch (e) {
            console.error(e);
        }
    }

    // Effect to handle focus
    let inputEl;
    $effect(() => {
        if (isPlaying && inputEl) {
            inputEl.focus();
        }
    });
</script>

<div class="game-container">
    {#if !isPlaying}
        <div class="start-screen">
            <button onclick={startGame}>Start Game</button>
            {#if commands.length === 0}
                <p>Loading commands...</p>
            {/if}
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
        display: flex;
        flex-direction: column;
        align-items: center;
        background: #252526;
        padding: 2rem;
        border-radius: 8px;
        min-height: 400px; /* Increased height for vertical queue */
        justify-content: center;
        overflow: hidden;
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
    }

    .queue-item.next {
        position: absolute;
        top: 10px;
        font-size: 1.2rem;
        color: #666;
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
        color: #4caf50;
        display: inline-block;
        animation: pop 0.1s ease-out;
    }
    .wrong {
        color: #f44336;
        background: rgba(244, 67, 54, 0.2);
    }
    .pending {
        color: #bbb; /* Lighter pending text */
    }

    .input-box {
        font-size: 1.5rem;
        padding: 10px;
        width: 80%;
        text-align: center;
        border: 2px solid #0e639c;
        border-radius: 4px;
        background: #1e1e1e;
        color: white;
        transition: border-color 0.2s;
        z-index: 20;
    }

    .shake {
        animation: shake 0.3s;
        border-color: #f44336;
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
        color: #888;
    }
    button {
        font-size: 1.5rem;
        padding: 1rem 2rem;
    }
</style>
