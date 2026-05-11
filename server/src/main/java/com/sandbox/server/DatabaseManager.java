package com.sandbox.server;

import com.common.sandbox.model.Player;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.UUID;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/sandbox_game";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "M@rllon3455";

    private DatabaseManager() {
        // Garantir que as tabelas existem ao iniciar
        initializeTables();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Obtém uma nova conexão (não reutiliza a mesma)
     */
    public Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            logger.debug("Database connection opened");
            return conn;
        } catch (ClassNotFoundException e) {
            logger.error("PostgreSQL driver not found!", e);
            throw new SQLException("Driver not found", e);
        }
    }

    private void initializeTables() {
        String createMapsTable = """
            CREATE TABLE IF NOT EXISTS maps (
                id UUID PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                version INT DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                created_by VARCHAR(50),
                is_active BOOLEAN DEFAULT true
            )
        """;

        String createMapChunksTable = """
            CREATE TABLE IF NOT EXISTS map_chunks (
                map_id UUID REFERENCES maps(id) ON DELETE CASCADE,
                chunk_x INT NOT NULL,
                chunk_y INT NOT NULL,
                chunk_data BYTEA NOT NULL,
                metadata JSONB,
                last_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                modified_by VARCHAR(50),
                PRIMARY KEY (map_id, chunk_x, chunk_y)
            )
        """;

        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS players (
                id UUID PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                x FLOAT DEFAULT 400,
                y FLOAT DEFAULT 300,
                direction VARCHAR(20) DEFAULT 'DOWN',
                last_login TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createChatHistoryTable = """
            CREATE TABLE IF NOT EXISTS chat_history (
                id SERIAL PRIMARY KEY,
                player_id UUID REFERENCES players(id),
                message TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_map_chunks_coords ON map_chunks(chunk_x, chunk_y);
            CREATE INDEX IF NOT EXISTS idx_maps_active ON maps(is_active);
            CREATE INDEX IF NOT EXISTS idx_players_username ON players(username)
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createMapsTable);
            stmt.execute(createMapChunksTable);
            stmt.execute(createPlayersTable);
            stmt.execute(createChatHistoryTable);
            stmt.execute(createIndexes);
            logger.info("Database tables initialized");
        } catch (SQLException e) {
            logger.error("Failed to initialize tables", e);
        }
    }

    public boolean registerPlayer(String username, String email, String password) {
        String sql = "INSERT INTO players (id, username, email, password_hash) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
            UUID id = UUID.randomUUID();

            pstmt.setObject(1, id);
            pstmt.setString(2, username);
            pstmt.setString(3, email);
            pstmt.setString(4, hashedPassword);

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            if (e.getSQLState().equals("23505")) {
                logger.warn("Username or email already exists: {}", username);
                return false;
            }
            logger.error("Error registering player", e);
            return false;
        }
    }

    public Player authenticatePlayer(String username, String password) {
        String sql = "SELECT id, username, email, password_hash, x, y, direction FROM players WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password_hash");

                if (BCrypt.checkpw(password, hashedPassword)) {
                    UUID id = (UUID) rs.getObject("id");
                    String email = rs.getString("email");
                    float x = rs.getFloat("x");
                    float y = rs.getFloat("y");
                    String direction = rs.getString("direction");

                    Player player = new Player(id.toString(), username, email);
                    player.setX(x);
                    player.setY(y);
                    player.setDirection(direction);

                    updateLastLogin(id);
                    return player;
                }
            }
            return null;

        } catch (SQLException e) {
            logger.error("Error authenticating player", e);
            return null;
        }
    }

    private void updateLastLogin(UUID playerId) {
        String sql = "UPDATE players SET last_login = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, playerId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating last login", e);
        }
    }

    public void updatePlayerPosition(Player player) {
        String sql = "UPDATE players SET x = ?, y = ?, direction = ? WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setFloat(1, player.getX());
            pstmt.setFloat(2, player.getY());
            pstmt.setString(3, player.getDirection());
            pstmt.setString(4, player.getUsername());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating player position", e);
        }
    }

    public void saveChatMessage(String playerId, String message) {
        String sql = "INSERT INTO chat_history (player_id, message) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            pstmt.setString(2, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error saving chat message", e);
        }
    }

    public void close() {
        // Não precisamos fechar uma conexão global
        logger.info("DatabaseManager closed");
    }
}