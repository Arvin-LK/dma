package com.dma.core.infrastructure.repository;

import com.dma.common.enums.DatabaseType;
import com.dma.core.domain.model.connection.ConnectionId;
import com.dma.core.domain.model.connection.DatabaseConnection;
import com.dma.core.domain.repository.ConnectionRepository;
import com.dma.core.infrastructure.config.DatabaseInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite 持久化的数据库连接仓储。
 * 替换 InMemory 实现，连接配置在应用重启后保留。
 */
@Primary
@Repository
public class SqliteConnectionRepository implements ConnectionRepository {

    private static final Logger log = LoggerFactory.getLogger(SqliteConnectionRepository.class);
    private final String jdbcUrl;

    public SqliteConnectionRepository(DatabaseInitializer initializer) {
        this.jdbcUrl = "jdbc:sqlite:" + initializer.getDbPath();
        log.info("SqliteConnectionRepository initialized");
    }

    @Override
    public Optional<DatabaseConnection> findById(ConnectionId id) {
        String sql = "SELECT * FROM database_connection WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id.value());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findById failed: {}", id.value(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<DatabaseConnection> findAll(int page, int size) {
        List<DatabaseConnection> list = new ArrayList<>();
        String sql = "SELECT * FROM database_connection ORDER BY updated_at DESC LIMIT ? OFFSET ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, size);
            ps.setInt(2, (page - 1) * size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findAll failed", e);
        }
        return list;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM database_connection";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    @Override
    public ConnectionId save(DatabaseConnection connection) {
        String sql = """
            INSERT INTO database_connection (name, db_type, host, port, username, password_encrypted, database_name)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, connection.getName());
            ps.setString(2, connection.getDbType().name());
            ps.setString(3, connection.getHost());
            ps.setInt(4, connection.getPort());
            ps.setString(5, connection.getUsername());
            ps.setString(6, connection.getEncryptedPassword());
            ps.setString(7, connection.getDatabaseName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ConnectionId id = new ConnectionId(keys.getLong(1));
                    connection.setId(id);
                    log.info("Saved connection: id={}, name={}", id.value(), connection.getName());
                    return id;
                }
            }
        } catch (SQLException e) {
            log.error("save failed: {}", connection.getName(), e);
        }
        throw new RuntimeException("Failed to save connection: " + connection.getName());
    }

    @Override
    public void update(DatabaseConnection connection) {
        String sql = """
            UPDATE database_connection SET name=?, db_type=?, host=?, port=?,
            username=?, password_encrypted=?, database_name=?,
            updated_at=datetime('now','localtime')
            WHERE id=?
            """;
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, connection.getName());
            ps.setString(2, connection.getDbType().name());
            ps.setString(3, connection.getHost());
            ps.setInt(4, connection.getPort());
            ps.setString(5, connection.getUsername());
            ps.setString(6, connection.getEncryptedPassword());
            ps.setString(7, connection.getDatabaseName());
            ps.setLong(8, connection.getId().value());
            ps.executeUpdate();
            log.info("Updated connection: id={}", connection.getId().value());
        } catch (SQLException e) {
            log.error("update failed: {}", connection.getId(), e);
        }
    }

    @Override
    public void delete(ConnectionId id) {
        String sql = "DELETE FROM database_connection WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id.value());
            ps.executeUpdate();
            log.info("Deleted connection: id={}", id.value());
        } catch (SQLException e) {
            log.error("delete failed: {}", id.value(), e);
        }
    }

    @Override
    public boolean existsByName(String name) {
        String sql = "SELECT 1 FROM database_connection WHERE name = ?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private DatabaseConnection mapRow(ResultSet rs) throws SQLException {
        DatabaseConnection conn = new DatabaseConnection(
                rs.getString("name"),
                DatabaseType.valueOf(rs.getString("db_type")),
                rs.getString("host"),
                rs.getInt("port"),
                rs.getString("username"),
                rs.getString("password_encrypted"),
                rs.getString("database_name")
        );
        conn.setId(new ConnectionId(rs.getLong("id")));
        // Timestamps
        String created = rs.getString("created_at");
        String updated = rs.getString("updated_at");
        if (created != null) {
            try { conn.setCreatedAt(LocalDateTime.parse(created, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); }
            catch (Exception ignored) {}
        }
        if (updated != null) {
            try { conn.setUpdatedAt(LocalDateTime.parse(updated, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))); }
            catch (Exception ignored) {}
        }
        return conn;
    }
}
