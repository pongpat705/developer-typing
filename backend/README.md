### Speed Typing Backend

This is the backend for the developer-typing speed typing game.

#### Environment Variables

The application can be configured using the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_PATH` | Path to the RocksDB data directory | `rocksdb_data` |
| `BACKUP_PATH` | Path to the RocksDB backup directory | `rocksdb_backup` |
| `COMMANDS_PATH` | Path to the directory containing `.txt` files with commands | `commands` |
| `HTTP_PORT` | The port on which the API server will listen | `8080` |

#### Setup Instructions

1.  **Clone the repository**
2.  **Navigate to the backend directory**: `cd backend`
3.  **Set environment variables** (optional):
    - On Linux/macOS:
      ```bash
      export DB_PATH=./my_db_data
      export BACKUP_PATH=./my_db_backup
      export COMMANDS_PATH=./my_commands
      export HTTP_PORT=9000
      ```
    - On Windows (Command Prompt):
      ```cmd
      set DB_PATH=./my_db_data
      set BACKUP_PATH=./my_db_backup
      set COMMANDS_PATH=./my_commands
      set HTTP_PORT=9000
      ```
4.  **Run the application**:
    ```bash
    ./mvnw clean compile exec:java
    ```

5.  **For IntelliJ users**:
    ```bash
      DB_PATH=/home/pongpat/IdeaProjects/upskill/developer-typing/rocksdb_data;BACKUP_PATH=/home/pongpat/IdeaProjects/upskill/developer-typing/rocksdb_backup;COMMANDS_PATH=/home/pongpat/IdeaProjects/upskill/developer-typing/commands;HTTP_PORT=8080;
    ```
