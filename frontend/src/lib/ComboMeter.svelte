<script>
    let { count = 0 } = $props();
</script>

<div class="combo-container">
    <div class="combo-text" class:shake={count > 5} class:glow={count > 10}>
        {#if count > 1}
            x{count} COMBO!
        {:else}
            Typing Speed
        {/if}
    </div>
    <div class="progress-bar">
        <div class="progress" style="width: {Math.min(count * 5, 100)}%"></div>
    </div>
</div>

<style>
    .combo-container {
        text-align: center;
        background: #252526;
        padding: 1rem;
        border-radius: 8px;
        min-height: 100px;
        display: flex;
        flex-direction: column;
        justify-content: center;
    }
    .combo-text {
        font-size: 2rem;
        font-weight: bold;
        color: #61dafb;
        transition: transform 0.1s;
    }
    .shake {
        animation: shake 0.5s infinite;
        color: #ff9800;
    }
    .glow {
        text-shadow: 0 0 10px #ff9800;
        color: #ff5722;
    }
    .progress-bar {
        width: 100%;
        height: 10px;
        background: #3c3c3c;
        margin-top: 10px;
        border-radius: 5px;
        overflow: hidden;
    }
    .progress {
        height: 100%;
        background: linear-gradient(90deg, #61dafb, #2196f3);
        transition: width 0.2s ease-out;
    }

    @keyframes shake {
        0% { transform: translate(1px, 1px) rotate(0deg); }
        10% { transform: translate(-1px, -2px) rotate(-1deg); }
        20% { transform: translate(-3px, 0px) rotate(1deg); }
        30% { transform: translate(3px, 2px) rotate(0deg); }
        40% { transform: translate(1px, -1px) rotate(1deg); }
        50% { transform: translate(-1px, 2px) rotate(-1deg); }
        60% { transform: translate(-3px, 1px) rotate(0deg); }
        70% { transform: translate(3px, 1px) rotate(-1deg); }
        80% { transform: translate(-1px, -1px) rotate(1deg); }
        90% { transform: translate(1px, 2px) rotate(0deg); }
        100% { transform: translate(1px, -2px) rotate(-1deg); }
    }
</style>
