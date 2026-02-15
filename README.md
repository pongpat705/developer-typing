# Programmer Speed Typing Game

A high-performance typing game designed for programmers, built with **Java 21**, **Vert.x 5**, **RocksDB**, and **Svelte 5**.

## 🚀 Features

-   **Real-time Typing Challenge**: Type actual programming commands (Git, Docker, etc.) against the clock.
-   **Combo System**: Build streaks for accuracy. Visual feedback with "shake" and "glow" effects.
-   **Live Leaderboard**: Top 10 scores persist across restarts using RocksDB.
-   **WPM Calculation**: Accurate Words Per Minute tracking.
-   **High Performance**:
    -   **Backend**: Non-blocking reactive architecture with Vert.x 5.
    -   **Virtual Threads**: Used for blocking I/O (File loading, Database operations).
    -   **Custom Binary Codecs**: Efficient EventBus communication (no JSON overhead for internal messages).
    -   **RocksDB**: Embedded key-value store for ultra-low latency persistence.

## 🛠 Tech Stack

### Backend
-   **Language**: Java 21
-   **Framework**: Vert.x 5.0.0.CR3
-   **Database**: RocksDB 9.7.3 (Embedded)
-   **Concurrency**: Virtual Threads (Project Loom)
-   **Build Tool**: Maven

### Frontend
-   **Framework**: Svelte 5 (Runes mode: `$state`, `$derived`, `$effect`)
-   **Build Tool**: Vite

## 📂 Project Structure

```
├── backend/               # Java Backend
│   ├── src/main/java/     # Source code
│   │   ├── codecs/        # Custom EventBus Codecs
│   │   ├── models/        # Java Records
│   │   └── ...Verticle    # Vert.x Verticles
│   └── pom.xml            # Maven configuration
├── frontend/              # Svelte Frontend
│   ├── src/lib/           # Svelte Components
│   │   ├── GameCanvas.svelte
│   │   ├── Leaderboard.svelte
│   │   └── ComboMeter.svelte
│   └── vite.config.js
└── commands/              # Data source for typing prompts
    ├── git_commands.txt
    └── docker_commands.txt
```

## ⚙️ Setup & Run

### Prerequisites
-   Java 21+
-   Node.js 20+
-   Maven 3.9+

### 1. Start the Backend
```bash
cd backend
mvn clean package
java -jar target/speedtyping-1.0-SNAPSHOT-fat.jar
```
The server will start on `http://localhost:8080`.

### 2. Start the Frontend
```bash
cd frontend
npm install
npm run dev
```
Access the game at `http://localhost:5173`.

## 🧠 Key Implementation Details

### Vert.x EventBus & Codecs
We avoid JSON serialization on the internal EventBus by using custom `MessageCodec` implementations for `ScoreSubmission` and `LeaderboardRequest`. This allows passing Java Records directly between Verticles.

### RocksDB Persistence
-   **Storage**: Scores are stored in RocksDB with a key format optimized for range queries: `score:{total_score}:{timestamp}:{user_id}`.
-   **Buffering**: A `NavigableMap` (Sequenced Collection) buffers writes before flushing to RocksDB.
-   **Reliability**: Automatic backup on startup if corruption is detected.

### Svelte 5 Runes
The frontend leverages the new Svelte 5 reactivity model:
-   `$state`: Manages game input, timer, and score.
-   `$derived`: Automatically recalculates WPM based on `charsTyped` and `timeElapsed`.
-   `$effect`: Handles side effects like timer intervals and focus management.
