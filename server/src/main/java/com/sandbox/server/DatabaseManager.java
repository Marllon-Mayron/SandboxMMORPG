package com.sandbox.server;

import com.common.sandbox.model.item.Inventory;
import com.common.sandbox.model.player.Player;
import com.common.sandbox.network.packets.social.FriendListResponse;
import com.common.sandbox.network.packets.chat.PrivateMessagePacket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
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

    private final ObjectMapper objectMapper;

    private DatabaseManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        initializeTables();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

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
        String createPlayersTable = """
            CREATE TABLE IF NOT EXISTS players (
                id UUID PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                email VARCHAR(100) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                x FLOAT DEFAULT 400,
                y FLOAT DEFAULT 300,
                direction VARCHAR(20) DEFAULT 'DOWN',
                level INT DEFAULT 1,
                experience INT DEFAULT 0,
                gold INT DEFAULT 0,
                current_hp INT DEFAULT 100,
                current_mana INT DEFAULT 50,
                current_stamina INT DEFAULT 100,
                base_hp INT DEFAULT 100,
                base_mana INT DEFAULT 50,
                base_stamina INT DEFAULT 100,
                strength INT DEFAULT 5,
                agility INT DEFAULT 5,
                wisdom INT DEFAULT 5,
                attribute_points INT DEFAULT 0,
                skill_points INT DEFAULT 0,
                hp_regen_per_second INT DEFAULT 3,
                mana_regen_per_second INT DEFAULT 3,
                stamina_regen_per_second INT DEFAULT 5,
                inventory JSONB DEFAULT '{"slots": {}, "equipped": {}}',
                is_online BOOLEAN DEFAULT false,
                last_login TIMESTAMP,
                last_logout TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

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

        String createChatHistoryTable = """
            CREATE TABLE IF NOT EXISTS chat_history (
                id SERIAL PRIMARY KEY,
                player_id UUID REFERENCES players(id),
                message TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createFriendsTable = """
            CREATE TABLE IF NOT EXISTS friends (
                player_id UUID REFERENCES players(id) ON DELETE CASCADE,
                friend_id UUID REFERENCES players(id) ON DELETE CASCADE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (player_id, friend_id)
            )
        """;

        String createFriendRequestsTable = """
            CREATE TABLE IF NOT EXISTS friend_requests (
                id SERIAL PRIMARY KEY,
                from_player_id UUID REFERENCES players(id) ON DELETE CASCADE,
                to_player_id UUID REFERENCES players(id) ON DELETE CASCADE,
                status VARCHAR(20) DEFAULT 'pending',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE(from_player_id, to_player_id)
            )
        """;

        String createPrivateMessagesTable = """
            CREATE TABLE IF NOT EXISTS private_messages (
                id SERIAL PRIMARY KEY,
                from_player_id UUID REFERENCES players(id) ON DELETE CASCADE,
                to_player_id UUID REFERENCES players(id) ON DELETE CASCADE,
                message TEXT NOT NULL,
                is_read BOOLEAN DEFAULT false,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        String createIndexes = """
            CREATE INDEX IF NOT EXISTS idx_map_chunks_coords ON map_chunks(chunk_x, chunk_y);
            CREATE INDEX IF NOT EXISTS idx_maps_active ON maps(is_active);
            CREATE INDEX IF NOT EXISTS idx_players_username ON players(username);
            CREATE INDEX IF NOT EXISTS idx_players_is_online ON players(is_online);
            CREATE INDEX IF NOT EXISTS idx_friend_requests_to_player ON friend_requests(to_player_id, status);
            CREATE INDEX IF NOT EXISTS idx_private_messages_to_player ON private_messages(to_player_id, is_read);
            CREATE INDEX IF NOT EXISTS idx_private_messages_conversation ON private_messages(from_player_id, to_player_id, created_at);
        """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayersTable);
            stmt.execute(createMapsTable);
            stmt.execute(createMapChunksTable);
            stmt.execute(createChatHistoryTable);
            stmt.execute(createFriendsTable);
            stmt.execute(createFriendRequestsTable);
            stmt.execute(createPrivateMessagesTable);
            stmt.execute(createIndexes);
            logger.info("Database tables initialized");
        } catch (SQLException e) {
            logger.error("Failed to initialize tables", e);
        }
    }

    // ==================== PLAYER MANAGEMENT ====================

    public boolean registerPlayer(String username, String email, String password) {
        String sql = "INSERT INTO players (id, username, email, password_hash, inventory) VALUES (?, ?, ?, ?, ?::jsonb)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
            UUID id = UUID.randomUUID();

            String emptyInventory = "{\"slots\": {}, \"equipped\": {}}";

            pstmt.setObject(1, id);
            pstmt.setString(2, username);
            pstmt.setString(3, email);
            pstmt.setString(4, hashedPassword);
            pstmt.setString(5, emptyInventory);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Player registered successfully: {}", username);
                return true;
            }
            return false;

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
                "level, experience, gold, attribute_points, skill_points, " +
                "current_hp, current_mana, current_stamina, " +
                // Bonus de atributos (upados pelo jogador)
                "bonus_max_hp, bonus_max_mana, bonus_max_stamina, " +
                "bonus_hp_regen, bonus_mana_regen, bonus_stamina_regen, " +
                "bonus_physical_defense, bonus_magic_defense, " +
                "bonus_physical_power, bonus_ranged_power, bonus_magic_power, " +
                "bonus_critical_chance, bonus_critical_damage, bonus_dodge_chance, " +
                "bonus_attack_speed, bonus_movement_speed, " +
                "bonus_cooldown_reduction, bonus_life_steal, bonus_mana_steal, bonus_tenacity, " +
                "bonus_luck, " +
                "bonus_fire_resistance, bonus_ice_resistance, bonus_lightning_resistance, " +
                "bonus_poison_resistance, bonus_holy_resistance, bonus_dark_resistance, " +
                "inventory " +
                "FROM players WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password_hash");

                if (BCrypt.checkpw(password, hashedPassword)) {
                    Player player = new Player();

                    // ==================== 1. DADOS BÁSICOS ====================
                    player.setId(rs.getObject("id").toString());
                    player.setUsername(rs.getString("username"));
                    player.setEmail(rs.getString("email"));
                    player.setX(rs.getFloat("x"));
                    player.setY(rs.getFloat("y"));
                    player.setDirection(rs.getString("direction") != null ? rs.getString("direction") : "DOWN");

                    // ==================== 2. PROGRESSÃO ====================
                    player.setLevel(rs.getInt("level"));
                    player.setExperience(rs.getInt("experience"));
                    player.setGold(rs.getInt("gold"));
                    player.setAttributePoints(rs.getInt("attribute_points"));
                    player.setSkillPoints(rs.getInt("skill_points"));

                    // ==================== 3. BÔNUS DE ATRIBUTOS (UPADOS PELO JOGADOR) ====================
                    // IMPORTANTE: Carregar TODOS os bônus ANTES dos status atuais
                    // para que getMaxHp() retorne o valor correto quando setCurrentHp for chamado

                    // Recursos
                    player.setBonusMaxHp(rs.getInt("bonus_max_hp"));
                    player.setBonusMaxMana(rs.getInt("bonus_max_mana"));
                    player.setBonusMaxStamina(rs.getInt("bonus_max_stamina"));

                    // Regeneração
                    player.setBonusHpRegen(rs.getInt("bonus_hp_regen"));
                    player.setBonusManaRegen(rs.getInt("bonus_mana_regen"));
                    player.setBonusStaminaRegen(rs.getInt("bonus_stamina_regen"));

                    // Defesas
                    player.setBonusPhysicalDefense(rs.getInt("bonus_physical_defense"));
                    player.setBonusMagicDefense(rs.getInt("bonus_magic_defense"));

                    // Poder de Dano
                    player.setBonusPhysicalPower(rs.getInt("bonus_physical_power"));
                    player.setBonusRangedPower(rs.getInt("bonus_ranged_power"));
                    player.setBonusMagicPower(rs.getInt("bonus_magic_power"));

                    // Chance e Multiplicadores
                    player.setBonusCriticalChance(rs.getFloat("bonus_critical_chance"));
                    player.setBonusCriticalDamage(rs.getFloat("bonus_critical_damage"));
                    player.setBonusDodgeChance(rs.getFloat("bonus_dodge_chance"));

                    // Velocidades
                    player.setBonusAttackSpeed(rs.getFloat("bonus_attack_speed"));
                    player.setBonusMovementSpeed(rs.getFloat("bonus_movement_speed"));

                    // Utilidades
                    player.setBonusCooldownReduction(rs.getFloat("bonus_cooldown_reduction"));
                    player.setBonusLifeSteal(rs.getFloat("bonus_life_steal"));
                    player.setBonusManaSteal(rs.getFloat("bonus_mana_steal"));
                    player.setBonusTenacity(rs.getFloat("bonus_tenacity"));

                    // Sorte
                    player.setBonusLuck(rs.getInt("bonus_luck"));

                    // Resistências Elementais
                    player.setBonusFireResistance(rs.getInt("bonus_fire_resistance"));
                    player.setBonusIceResistance(rs.getInt("bonus_ice_resistance"));
                    player.setBonusLightningResistance(rs.getInt("bonus_lightning_resistance"));
                    player.setBonusPoisonResistance(rs.getInt("bonus_poison_resistance"));
                    player.setBonusHolyResistance(rs.getInt("bonus_holy_resistance"));
                    player.setBonusDarkResistance(rs.getInt("bonus_dark_resistance"));

                    // ==================== 4. STATUS ATUAIS (DEPOIS dos bônus!) ====================
                    // Agora getMaxHp() retorna o valor correto porque os bônus já foram carregados
                    player.setCurrentHp(rs.getInt("current_hp"));
                    player.setCurrentMana(rs.getInt("current_mana"));
                    player.setCurrentStamina(rs.getInt("current_stamina"));

                    // ==================== 5. INVENTÁRIO ====================
                    String inventoryJson = rs.getString("inventory");
                    if (inventoryJson != null && !inventoryJson.isEmpty()) {
                        try {
                            Inventory inventory = objectMapper.readValue(inventoryJson, Inventory.class);
                            player.setInventory(inventory);
                            logger.debug("Loaded inventory for {}: {} slots, {} equipped",
                                    username, inventory.getSlots().size(), inventory.getEquipped().size());
                        } catch (Exception e) {
                            logger.error("Failed to parse inventory JSON for player {}", username, e);
                            player.setInventory(new Inventory());
                        }
                    } else {
                        player.setInventory(new Inventory());
                    }

                    player.setOnline(true);
                    updateLastLogin(player.getId());

                    // LOG COMPLETO PARA DEBUG
                    logger.info("========================================");
                    logger.info("PLAYER LOADED FROM DATABASE: {}", username);
                    logger.info("Level: {} | AP: {} | SP: {}", player.getLevel(), player.getAttributePoints(), player.getSkillPoints());
                    logger.info("HP: {}/{}", player.getCurrentHp(), player.getMaxHp());
                    logger.info("Mana: {}/{}", player.getCurrentMana(), player.getMaxMana());
                    logger.info("Stamina: {}/{}", player.getCurrentStamina(), player.getMaxStamina());
                    logger.info("Bonus MaxHP: {}", player.getBonusMaxHp());
                    logger.info("Physical Power: {} (Base:10 + Bonus:{})", player.getPhysicalPower(), player.getBonusPhysicalPower());
                    logger.info("Ranged Power: {} (Base:10 + Bonus:{})", player.getRangedPower(), player.getBonusRangedPower());
                    logger.info("Magic Power: {} (Base:10 + Bonus:{})", player.getMagicPower(), player.getBonusMagicPower());
                    logger.info("Physical Defense: {} (Base:0 + Bonus:{})", player.getPhysicalDefense(), player.getBonusPhysicalDefense());
                    logger.info("Magic Defense: {} (Base:0 + Bonus:{})", player.getMagicDefense(), player.getBonusMagicDefense());
                    logger.info("Critical Chance: {}% (Base:5% + Bonus:{})", player.getCriticalChance() * 100, player.getBonusCriticalChance() * 100);
                    logger.info("Movement Speed: {} (Base:400 + Bonus:{})", player.getMovementSpeed(), player.getBonusMovementSpeed());
                    logger.info("Equipped items: {}", player.getInventory().getEquipped());
                    logger.info("========================================");

                    return player;
                } else {
                    logger.warn("Invalid password for player: {}", username);
                }
            }
            return null;

        } catch (SQLException e) {
            logger.error("Error authenticating player", e);
            return null;
        }
    }

    public void savePlayer(Player player) {
        if (player == null) {
            logger.warn("Attempted to save null player");
            return;
        }

        String sql = """
        UPDATE players SET 
            x = ?, y = ?, direction = ?,
            level = ?, experience = ?, gold = ?,
            attribute_points = ?, skill_points = ?,
            current_hp = ?, current_mana = ?, current_stamina = ?,
            is_online = ?,
            inventory = ?::jsonb,
            
            bonus_max_hp = ?,
            bonus_max_mana = ?,
            bonus_max_stamina = ?,
            bonus_hp_regen = ?,
            bonus_mana_regen = ?,
            bonus_stamina_regen = ?,
            bonus_physical_defense = ?,
            bonus_magic_defense = ?,
            bonus_physical_power = ?,
            bonus_ranged_power = ?,
            bonus_magic_power = ?,
            bonus_critical_chance = ?,
            bonus_critical_damage = ?,
            bonus_dodge_chance = ?,
            bonus_attack_speed = ?,
            bonus_movement_speed = ?,
            bonus_cooldown_reduction = ?,
            bonus_life_steal = ?,
            bonus_mana_steal = ?,
            bonus_tenacity = ?,
            bonus_luck = ?,
            bonus_fire_resistance = ?,
            bonus_ice_resistance = ?,
            bonus_lightning_resistance = ?,
            bonus_poison_resistance = ?,
            bonus_holy_resistance = ?,
            bonus_dark_resistance = ?
        WHERE id = ?::uuid
        """;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String inventoryJson = objectMapper.writeValueAsString(player.getInventory());

            int index = 1;

            // Posicao
            pstmt.setFloat(index++, player.getX());
            pstmt.setFloat(index++, player.getY());
            pstmt.setString(index++, player.getDirection());

            // Progressao
            pstmt.setInt(index++, player.getLevel());
            pstmt.setInt(index++, player.getExperience());
            pstmt.setInt(index++, player.getGold());
            pstmt.setInt(index++, player.getAttributePoints());
            pstmt.setInt(index++, player.getSkillPoints());

            // Status atuais
            pstmt.setInt(index++, player.getCurrentHp());
            pstmt.setInt(index++, player.getCurrentMana());
            pstmt.setInt(index++, player.getCurrentStamina());

            // Online status
            pstmt.setBoolean(index++, player.isOnline());

            // Inventario
            pstmt.setString(index++, inventoryJson);

            // Bonus atributos
            pstmt.setInt(index++, player.getBonusMaxHp());
            pstmt.setInt(index++, player.getBonusMaxMana());
            pstmt.setInt(index++, player.getBonusMaxStamina());

            pstmt.setInt(index++, player.getBonusHpRegen());
            pstmt.setInt(index++, player.getBonusManaRegen());
            pstmt.setInt(index++, player.getBonusStaminaRegen());

            pstmt.setInt(index++, player.getBonusPhysicalDefense());
            pstmt.setInt(index++, player.getBonusMagicDefense());

            pstmt.setInt(index++, player.getBonusPhysicalPower());
            pstmt.setInt(index++, player.getBonusRangedPower());
            pstmt.setInt(index++, player.getBonusMagicPower());

            pstmt.setFloat(index++, player.getBonusCriticalChance());
            pstmt.setFloat(index++, player.getBonusCriticalDamage());
            pstmt.setFloat(index++, player.getBonusDodgeChance());

            pstmt.setFloat(index++, player.getBonusAttackSpeed());
            pstmt.setFloat(index++, player.getBonusMovementSpeed());

            pstmt.setFloat(index++, player.getBonusCooldownReduction());
            pstmt.setFloat(index++, player.getBonusLifeSteal());
            pstmt.setFloat(index++, player.getBonusManaSteal());
            pstmt.setFloat(index++, player.getBonusTenacity());

            pstmt.setInt(index++, player.getBonusLuck());

            pstmt.setInt(index++, player.getBonusFireResistance());
            pstmt.setInt(index++, player.getBonusIceResistance());
            pstmt.setInt(index++, player.getBonusLightningResistance());
            pstmt.setInt(index++, player.getBonusPoisonResistance());
            pstmt.setInt(index++, player.getBonusHolyResistance());
            pstmt.setInt(index++, player.getBonusDarkResistance());

            // WHERE clause
            pstmt.setObject(index++, UUID.fromString(player.getId()));

            int updated = pstmt.executeUpdate();

            logger.debug("Saved player {} - Level {} | HP {}/{} | AP: {} | Pos: ({},{})",
                    player.getUsername(),
                    player.getLevel(),
                    player.getCurrentHp(), player.getMaxHp(),
                    player.getAttributePoints(),
                    player.getX(), player.getY());

            if (updated == 0) {
                logger.warn("No rows updated for player {}", player.getUsername());
            }

        } catch (SQLException e) {
            logger.error("Error saving player {}", player.getUsername(), e);
        } catch (Exception e) {
            logger.error("Error serializing inventory for {}", player.getUsername(), e);
        }
    }

    /**
     * Método assíncrono para salvar
     */
    public void savePlayerAsync(Player player) {
        Thread.startVirtualThread(() -> {
            savePlayer(player);
        });
    }

    private void updateLastLogin(String playerId) {
        String sql = "UPDATE players SET last_login = CURRENT_TIMESTAMP WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating last login", e);
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
        String sql = "INSERT INTO chat_history (player_id, message) VALUES (?::uuid, ?)";
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
        String sql = "SELECT id, username, level, is_online FROM players WHERE username = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Player player = new Player();
                player.setId(rs.getString("id"));
                player.setUsername(rs.getString("username"));
                player.setLevel(rs.getInt("level"));
                player.setOnline(rs.getBoolean("is_online"));
                return player;
            }
            return null;
        }
    }

    public Player getPlayerById(String playerId) throws SQLException {
        String sql = "SELECT id, username, level, is_online FROM players WHERE id = ?::uuid";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, UUID.fromString(playerId));
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Player player = new Player();
                player.setId(rs.getString("id"));
                player.setUsername(rs.getString("username"));
                player.setLevel(rs.getInt("level"));
                player.setOnline(rs.getBoolean("is_online"));
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
        String deleteRequestSql = "DELETE FROM friend_requests WHERE id = ?";
        String insertFriendSql = "INSERT INTO friends (player_id, friend_id) VALUES (?::uuid, ?::uuid), (?::uuid, ?::uuid)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
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
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteRequestSql)) {
                deleteStmt.setInt(1, Integer.parseInt(requestId));
                deleteStmt.executeUpdate();
            }
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
        String sql = "DELETE FROM friend_requests WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, Integer.parseInt(requestId));
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean removeFriend(String playerId, String friendId) throws SQLException {
        String deleteFriendsSql = "DELETE FROM friends WHERE (player_id = ?::uuid AND friend_id = ?::uuid) " +
                "OR (player_id = ?::uuid AND friend_id = ?::uuid)";
        String deleteRequestsSql = "DELETE FROM friend_requests WHERE " +
                "(from_player_id = ?::uuid AND to_player_id = ?::uuid) OR " +
                "(from_player_id = ?::uuid AND to_player_id = ?::uuid)";
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(deleteFriendsSql)) {
                pstmt.setObject(1, UUID.fromString(playerId));
                pstmt.setObject(2, UUID.fromString(friendId));
                pstmt.setObject(3, UUID.fromString(friendId));
                pstmt.setObject(4, UUID.fromString(playerId));
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(deleteRequestsSql)) {
                pstmt.setObject(1, UUID.fromString(playerId));
                pstmt.setObject(2, UUID.fromString(friendId));
                pstmt.setObject(3, UUID.fromString(friendId));
                pstmt.setObject(4, UUID.fromString(playerId));
                pstmt.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            logger.error("Error removing friend", e);
            throw e;
        }
    }

    public FriendRequestDetails getFriendRequestDetails(String requestId) throws SQLException {
        String sql = "SELECT fr.id, fr.from_player_id, fr.to_player_id, p.username as from_username " +
                "FROM friend_requests fr JOIN players p ON fr.from_player_id = p.id " +
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

    // ==================== MENSAGENS PRIVADAS ====================

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
                "ORDER BY created_at ASC LIMIT ?";
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
        logger.info("DatabaseManager closed");
    }
}