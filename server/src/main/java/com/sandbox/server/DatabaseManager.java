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
        String sql = "SELECT id, username, email, password_hash, x, y, direction, " +
                "level, experience, gold, current_hp, current_mana, current_stamina, " +
                "max_hp, max_mana, max_stamina, strength, agility, wisdom, " +
                "attribute_points, skill_points " +
                "FROM players WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password_hash");

                if (BCrypt.checkpw(password, hashedPassword)) {
                    Player player = new Player();
                    player.setId(rs.getObject("id").toString());
                    player.setUsername(rs.getString("username"));
                    player.setEmail(rs.getString("email"));
                    player.setX(rs.getFloat("x"));
                    player.setY(rs.getFloat("y"));
                    player.setDirection(rs.getString("direction") != null ? rs.getString("direction") : "DOWN");

                    // Novos campos
                    player.setLevel(rs.getInt("level"));
                    player.setExperience(rs.getInt("experience"));
                    player.setGold(rs.getInt("gold"));
                    player.setCurrentHp(rs.getInt("current_hp"));
                    player.setCurrentMana(rs.getInt("current_mana"));
                    player.setCurrentStamina(rs.getInt("current_stamina"));
                    player.setMaxHp(rs.getInt("max_hp"));
                    player.setMaxMana(rs.getInt("max_mana"));
                    player.setMaxStamina(rs.getInt("max_stamina"));
                    player.setStrength(rs.getInt("strength"));
                    player.setAgility(rs.getInt("agility"));
                    player.setWisdom(rs.getInt("wisdom"));
                    player.setAttributePoints(rs.getInt("attribute_points"));
                    player.setSkillPoints(rs.getInt("skill_points"));
                    player.setOnline(true);

                    updateLastLogin(player.getId());
                    savePlayerPosition(player); // Salvar para garantir que está online

                    logger.info("✅ Jogador {} carregado - Level {} | HP {}/{} | Gold {}",
                            username, player.getLevel(), player.getCurrentHp(), player.getMaxHp(), player.getGold());
                    return player;
                }
            }
            return null;

        } catch (SQLException e) {
            logger.error("❌ Erro ao autenticar jogador", e);
            return null;
        }
    }

    private void updateLastLogin(String playerId) {
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

    /**
     * Salva a posição do jogador (assíncrono para performance)
     */
    public void savePlayerPositionAsync(Player player) {
        // Usar virtual thread para não bloquear
        Thread.startVirtualThread(() -> {
            savePlayerPosition(player);
        });
    }

    /**
     * Salva a posição do jogador (síncrono - usado no logout)
     */
    public void savePlayerPosition(Player player) {
        String sql = "UPDATE players SET " +
                "x = ?, y = ?, direction = ?, " +
                "last_login = CURRENT_TIMESTAMP, " +
                "is_online = true, " +
                "level = ?, experience = ?, gold = ?, " +
                "current_hp = ?, current_mana = ?, current_stamina = ?, " +
                "max_hp = ?, max_mana = ?, max_stamina = ?, " +
                "strength = ?, agility = ?, wisdom = ?, " +
                "attribute_points = ?, skill_points = ? " +
                "WHERE id = ?::uuid";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setFloat(1, player.getX());
            pstmt.setFloat(2, player.getY());
            pstmt.setString(3, player.getDirection());
            pstmt.setInt(4, player.getLevel());
            pstmt.setInt(5, player.getExperience());
            pstmt.setInt(6, player.getGold());
            pstmt.setInt(7, player.getCurrentHp());
            pstmt.setInt(8, player.getCurrentMana());
            pstmt.setInt(9, player.getCurrentStamina());
            pstmt.setInt(10, player.getMaxHp());
            pstmt.setInt(11, player.getMaxMana());
            pstmt.setInt(12, player.getMaxStamina());
            pstmt.setInt(13, player.getStrength());
            pstmt.setInt(14, player.getAgility());
            pstmt.setInt(15, player.getWisdom());
            pstmt.setInt(16, player.getAttributePoints());
            pstmt.setInt(17, player.getSkillPoints());
            pstmt.setObject(18, UUID.fromString(player.getId()));

            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                logger.debug("✅ Player {} saved: Level {} | HP {}/{} | Gold {}",
                        player.getUsername(), player.getLevel(),
                        player.getCurrentHp(), player.getMaxHp(), player.getGold());
            }

        } catch (SQLException e) {
            logger.error("❌ Erro ao salvar jogador {}", player.getUsername(), e);
        }
    }

    public void setPlayerOffline(String playerId) {
        String sql = "UPDATE players SET is_online = false, last_logout = CURRENT_TIMESTAMP WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error setting player offline", e);
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