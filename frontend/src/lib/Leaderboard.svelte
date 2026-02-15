<script>
    import { onMount } from 'svelte';

    let scores = $state([]);

    async function fetchScores() {
        try {
            const res = await fetch('http://localhost:8080/api/leaderboard?limit=10');
            if (res.ok) {
                scores = await res.json();
            }
        } catch (e) {
            console.error(e);
        }
    }

    onMount(() => {
        fetchScores();
        const interval = setInterval(fetchScores, 5000);
        return () => clearInterval(interval);
    });
</script>

<div class="leaderboard">
    <h2>🏆 Top 10 Leaderboard</h2>
    <table>
        <thead>
            <tr>
                <th>#</th>
                <th>Player</th>
                <th>WPM</th>
                <th>Combo</th>
                <th>Total</th>
            </tr>
        </thead>
        <tbody>
            {#each scores as score, i}
                <tr class:top3={i < 3}>
                    <td>{i + 1}</td>
                    <td>{score.username}</td>
                    <td>{score.wpm}</td>
                    <td>{score.maxCombo}</td>
                    <td>{score.totalScore}</td>
                </tr>
            {/each}
        </tbody>
    </table>
</div>

<style>
    .leaderboard {
        background: #252526;
        padding: 1rem;
        border-radius: 8px;
    }
    h2 {
        text-align: center;
        color: #ffca28;
    }
    table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 1rem;
    }
    th, td {
        padding: 0.5rem;
        text-align: left;
        border-bottom: 1px solid #3c3c3c;
    }
    th {
        color: #888;
    }
    .top3 {
        color: #ffca28;
        font-weight: bold;
    }
</style>
