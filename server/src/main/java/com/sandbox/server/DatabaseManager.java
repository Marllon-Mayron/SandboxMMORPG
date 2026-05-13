package com.sandbox.server;

import com.common.sandbox.model.Player;
import com.common.sandbox.network.packets.FriendListResponse;
import com.common.sandbox.network.packets.PrivateMessagePacket;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.List;
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
                "attribute_points, skill_points, " +
                "hp_regen_per_second, mana_regen_per_second, stamina_regen_per_second " +
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

                    // Regeneração
                    player.setHpRegenPerSecond(rs.getInt("hp_regen_per_second"));
                    player.setManaRegenPerSecond(rs.getInt("mana_regen_per_second"));
                    player.setStaminaRegenPerSecond(rs.getInt("stamina_regen_per_second"));

                    player.setOnline(true);

                    updateLastLogin(player.getId());
                    savePlayerPosition(player);

                    logger.info("✅ Jogador {} carregado - Level {} | HP {}/{} | Stamina {}/{}",
                            username, player.getLevel(),
                            player.getCurrentHp(), player.getMaxHp(),
                            player.getCurrentStamina(), player.getMaxStamina());
                    return player;
                }
            }
            return null;

        } catch (SQLException e) {
            logger.error("❌ Erro ao autenticar jogador", e);
            return null;
        }
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
                "attribute_points = ?, skill_points = ?, " +
                "hp_regen_per_second = ?, mana_regen_per_second = ?, stamina_regen_per_second = ? " +
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
            pstmt.setInt(18, player.getHpRegenPerSecond());
            pstmt.setInt(19, player.getManaRegenPerSecond());
            pstmt.setInt(20, player.getStaminaRegenPerSecond());
            pstmt.setObject(21, UUID.fromString(player.getId()));

            int updated = pstmt.executeUpdate();
            if (updated > 0) {
                logger.debug("✅ Player {} saved", player.getUsername());
            }

        } catch (SQLException e) {
            logger.error("❌ Erro ao salvar jogador {}", player.getUsername(), e);
        }
    }

    private void updateLastLogin(String playerId) {
        String sql = "UPDATE players SET last_login = CURRENT_TIMESTAMP WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));  // Converter para UUID
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

    // ==================== SISTEMA DE AMIGOS ====================

    public static class FriendRequestDetails {
        public String requestId;
        public String fromPlayerId;
        public String toPlayerId;
        public String fromUsername;
    }

    public Player getPlayerByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, level FROM players WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Player player = new Player();
                player.setId(rs.getString("id"));
                player.setUsername(rs.getString("username"));
                player.setLevel(rs.getInt("level"));
                return player;
            }
            return null;
        }
    }

    public Player getPlayerById(String playerId) throws SQLException {
        String sql = "SELECT id, username, level FROM players WHERE id = ?::uuid";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Player player = new Player();
                player.setId(rs.getString("id"));
                player.setUsername(rs.getString("username"));
                player.setLevel(rs.getInt("level"));
                return player;
            }
            return null;
        }
    }

    public boolean areFriends(String playerId, String friendId) throws SQLException {
        String sql = "SELECT 1 FROM friends WHERE (player_id = ?::uuid AND friend_id = ?::uuid) " +
                "OR (player_id = ?::uuid AND friend_id = ?::uuid)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            pstmt.setObject(2, UUID.fromString(friendId));
            pstmt.setObject(3, UUID.fromString(friendId));
            pstmt.setObject(4, UUID.fromString(playerId));
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    public boolean hasPendingRequest(String fromPlayerId, String toPlayerId) throws SQLException {
        String sql = "SELECT 1 FROM friend_requests WHERE from_player_id = ?::uuid " +
                "AND to_player_id = ?::uuid AND status = 'pending'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(fromPlayerId));
            pstmt.setObject(2, UUID.fromString(toPlayerId));
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    public String createFriendRequest(String fromPlayerId, String toPlayerId) throws SQLException {
        String sql = "INSERT INTO friend_requests (from_player_id, to_player_id, status) " +
                "VALUES (?::uuid, ?::uuid, 'pending') RETURNING id";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(fromPlayerId));
            pstmt.setObject(2, UUID.fromString(toPlayerId));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return String.valueOf(rs.getInt("id"));
            }
            return null;
        }
    }

    public boolean acceptFriendRequest(String requestId) throws SQLException {
        String updateSql = "UPDATE friend_requests SET status = 'accepted', updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        String insertFriendSql = "INSERT INTO friends (player_id, friend_id) VALUES (?::uuid, ?::uuid), (?::uuid, ?::uuid)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // Buscar detalhes da solicitacao - especificar colunas com alias
            String selectSql = "SELECT fr.from_player_id, fr.to_player_id FROM friend_requests fr WHERE fr.id = ? AND fr.status = 'pending'";
            String fromId = null, toId = null;

            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, Integer.parseInt(requestId));
                ResultSet rs = selectStmt.executeQuery();
                if (rs.next()) {
                    fromId = rs.getString("from_player_id");
                    toId = rs.getString("to_player_id");
                }
            }

            if (fromId == null || toId == null) return false;

            // Atualizar status da solicitacao
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setInt(1, Integer.parseInt(requestId));
                updateStmt.executeUpdate();
            }

            // Adicionar relacao de amizade (bidirecional)
            try (PreparedStatement insertStmt = conn.prepareStatement(insertFriendSql)) {
                insertStmt.setObject(1, UUID.fromString(fromId));
                insertStmt.setObject(2, UUID.fromString(toId));
                insertStmt.setObject(3, UUID.fromString(toId));
                insertStmt.setObject(4, UUID.fromString(fromId));
                insertStmt.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            logger.error("Error accepting friend request", e);
            throw e;
        }
    }

    public boolean rejectFriendRequest(String requestId) throws SQLException {
        // Deletar em vez de atualizar status
        String sql = "DELETE FROM friend_requests WHERE id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(requestId));
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean removeFriend(String playerId, String friendId) throws SQLException {
        String sql = "DELETE FROM friends WHERE (player_id = ?::uuid AND friend_id = ?::uuid) " +
                "OR (player_id = ?::uuid AND friend_id = ?::uuid)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            pstmt.setObject(2, UUID.fromString(friendId));
            pstmt.setObject(3, UUID.fromString(friendId));
            pstmt.setObject(4, UUID.fromString(playerId));
            return pstmt.executeUpdate() > 0;
        }
    }

    public FriendRequestDetails getFriendRequestDetails(String requestId) throws SQLException {
        String sql = "SELECT fr.id, fr.from_player_id, fr.to_player_id, p.username as from_username " +
                "FROM friend_requests fr " +
                "JOIN players p ON fr.from_player_id = p.id " +
                "WHERE fr.id = ? AND fr.status = 'pending'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(requestId));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                FriendRequestDetails details = new FriendRequestDetails();
                details.requestId = String.valueOf(rs.getInt("id"));
                details.fromPlayerId = rs.getString("from_player_id");
                details.toPlayerId = rs.getString("to_player_id");
                details.fromUsername = rs.getString("from_username");
                return details;
            }
            return null;
        }
    }

    public FriendListResponse getFriendList(String playerId) throws SQLException {
        FriendListResponse response = new FriendListResponse();
        response.friends = new java.util.ArrayList<>();
        response.pendingRequests = new java.util.ArrayList<>();

        // Buscar amigos
        String friendsSql = "SELECT p.id, p.username, p.level, p.is_online " +
                "FROM friends f JOIN players p ON f.friend_id = p.id " +
                "WHERE f.player_id = ?::uuid";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(friendsSql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                FriendListResponse.FriendInfo friend = new FriendListResponse.FriendInfo();
                friend.playerId = rs.getString("id");
                friend.username = rs.getString("username");
                friend.level = rs.getInt("level");
                friend.isOnline = rs.getBoolean("is_online");
                response.friends.add(friend);
            }
        }

        // Buscar solicitacoes pendentes (recebidas)
        String requestsSql = "SELECT fr.id, fr.from_player_id, p.username, p.level, fr.created_at " +
                "FROM friend_requests fr JOIN players p ON fr.from_player_id = p.id " +
                "WHERE fr.to_player_id = ?::uuid AND fr.status = 'pending'";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(requestsSql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                FriendListResponse.FriendRequestInfo request = new FriendListResponse.FriendRequestInfo();
                request.requestId = String.valueOf(rs.getInt("id"));
                request.fromPlayerId = rs.getString("from_player_id");
                request.fromUsername = rs.getString("username");
                request.fromLevel = rs.getInt("level");
                request.createdAt = rs.getTimestamp("created_at").getTime();
                response.pendingRequests.add(request);
            }
        }

        return response;
    }

    public void savePrivateMessage(PrivateMessagePacket packet) throws SQLException {
        String sql = "INSERT INTO private_messages (from_player_id, to_player_id, message, created_at) " +
                "VALUES (?::uuid, ?::uuid, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(packet.fromPlayerId));
            pstmt.setObject(2, UUID.fromString(packet.toPlayerId));
            pstmt.setString(3, packet.message);
            pstmt.setTimestamp(4, new java.sql.Timestamp(packet.timestamp));
            pstmt.executeUpdate();
        }
    }

    public List<PrivateMessagePacket> getPrivateMessageHistory(String playerId, String friendId, int limit) throws SQLException {
        String sql = "SELECT from_player_id, to_player_id, message, created_at, " +
                "(SELECT username FROM players WHERE id = from_player_id) as from_username " +
                "FROM private_messages " +
                "WHERE (from_player_id = ?::uuid AND to_player_id = ?::uuid) " +
                "OR (from_player_id = ?::uuid AND to_player_id = ?::uuid) " +
                "ORDER BY created_at ASC " +
                "LIMIT ?";

        List<PrivateMessagePacket> messages = new java.util.ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setObject(1, UUID.fromString(playerId));
            pstmt.setObject(2, UUID.fromString(friendId));
            pstmt.setObject(3, UUID.fromString(friendId));
            pstmt.setObject(4, UUID.fromString(playerId));
            pstmt.setInt(5, limit);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                PrivateMessagePacket packet = new PrivateMessagePacket();
                packet.fromPlayerId = rs.getString("from_player_id");
                packet.toPlayerId = rs.getString("to_player_id");
                packet.fromUsername = rs.getString("from_username");
                packet.message = rs.getString("message");
                packet.timestamp = rs.getTimestamp("created_at").getTime();
                messages.add(packet);
            }
        }

        return messages;
    }

    public void markMessagesAsRead(String playerId, String friendId) throws SQLException {
        String sql = "UPDATE private_messages SET is_read = true " +
                "WHERE from_player_id = ?::uuid AND to_player_id = ?::uuid AND is_read = false";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(friendId));
            pstmt.setObject(2, UUID.fromString(playerId));
            pstmt.executeUpdate();
        }
    }

    public int getUnreadMessageCount(String playerId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM private_messages WHERE to_player_id = ?::uuid AND is_read = false";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    public void close() {
        // Não precisamos fechar uma conexão global
        logger.info("DatabaseManager closed");
    }
}