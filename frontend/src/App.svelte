<script>
  import GameCanvas from './lib/GameCanvas.svelte'
  import Leaderboard from './lib/Leaderboard.svelte'
  import ComboMeter from './lib/ComboMeter.svelte'

  let combo = $state(0);
  let maxCombo = $state(0);
</script>

<main>
  <h1>Programmer Speed Typing</h1>
  <div class="layout">
    <div class="game-area">
      <GameCanvas bind:combo={combo} bind:maxCombo={maxCombo} />
    </div>
    <div class="sidebar">
      <ComboMeter count={combo} />
      <Leaderboard />
    </div>
  </div>
</main>

<style>
  main {
    max-width: 1200px;
    margin: 0 auto;
    padding: 20px;
  }
  h1 {
    text-align: center;
    margin-bottom: 2rem;
  }
  .layout {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 2rem;
  }
  .sidebar {
    display: flex;
    flex-direction: column;
    gap: 2rem;
  }
</style>
