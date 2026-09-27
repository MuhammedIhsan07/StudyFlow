package com.studyflow.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

/** A single embedded SQL database; transactions serialize changes and preserve invariants. */
final class Database implements AutoCloseable {
    private final Connection connection;
    @FunctionalInterface interface Work<T> { T run() throws Exception; }

    Database(Path directory) throws Exception {
        Files.createDirectories(directory);
        String path = directory.resolve("studyflow").toAbsolutePath().normalize().toString().replace('\\', '/');
        if (path.contains(";")) throw new IllegalArgumentException("The data directory cannot contain a semicolon.");
        connection = DriverManager.getConnection("jdbc:h2:file:" + path + ";DB_CLOSE_ON_EXIT=FALSE", "sa", "");
        // H2 acquires an exclusive file lock; another server cannot silently share this file.
        for (String sql : SCHEMA) try (Statement statement = connection.createStatement()) { statement.execute(sql); }
        connection.setAutoCommit(false);
    }
    synchronized <T> T transaction(Work<T> work) throws Exception {
        try { T result = work.run(); connection.commit(); return result; }
        catch (Exception exception) { connection.rollback(); throw exception; }
    }
    List<Map<String, Object>> query(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(sql, parameters); ResultSet rows = statement.executeQuery()) {
            List<Map<String, Object>> result = new ArrayList<>();
            ResultSetMetaData metadata = rows.getMetaData();
            while (rows.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= metadata.getColumnCount(); i++) {
                    Object value = rows.getObject(i);
                    row.put(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT), value);
                }
                result.add(row);
            }
            return result;
        }
    }
    Map<String, Object> one(String sql, Object... parameters) throws SQLException {
        List<Map<String, Object>> rows = query(sql, parameters);
        return rows.isEmpty() ? null : rows.get(0);
    }
    int execute(String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = prepare(sql, parameters)) { return statement.executeUpdate(); }
    }
    private PreparedStatement prepare(String sql, Object... parameters) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        for (int i = 0; i < parameters.length; i++) statement.setObject(i + 1, parameters[i]);
        return statement;
    }
    @Override public synchronized void close() throws SQLException { connection.close(); }

    private static final String[] SCHEMA = {
        """
        CREATE TABLE IF NOT EXISTS users (
          id VARCHAR(36) PRIMARY KEY, name VARCHAR(100) NOT NULL,
          email VARCHAR(254) NOT NULL UNIQUE, password_hash VARCHAR(256) NOT NULL,
          role VARCHAR(10) NOT NULL CHECK(role IN ('ADMIN','MENTOR','STUDENT')),
          active BOOLEAN NOT NULL DEFAULT TRUE, must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
          program VARCHAR(120) NOT NULL DEFAULT '', daily_goal INT NOT NULL DEFAULT 120 CHECK(daily_goal BETWEEN 15 AND 720),
          preferred_time VARCHAR(10) NOT NULL DEFAULT 'EVENING',
          mentor_id VARCHAR(36) REFERENCES users(id), created_at VARCHAR(30) NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS sessions (
          token_hash VARCHAR(64) PRIMARY KEY, user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
          csrf_token VARCHAR(64) NOT NULL, expires_at BIGINT NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS subjects (
          id VARCHAR(36) PRIMARY KEY, student_id VARCHAR(36) NOT NULL REFERENCES users(id),
          name VARCHAR(100) NOT NULL, code VARCHAR(20) NOT NULL, color VARCHAR(7) NOT NULL,
          weekly_goal INT NOT NULL CHECK(weekly_goal BETWEEN 15 AND 3000),
          UNIQUE(student_id, code), UNIQUE(id, student_id)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS tasks (
          id VARCHAR(36) PRIMARY KEY, student_id VARCHAR(36) NOT NULL REFERENCES users(id),
          subject_id VARCHAR(36) NOT NULL, title VARCHAR(180) NOT NULL,
          task_type VARCHAR(12) NOT NULL CHECK(task_type IN ('STUDY','REVISION','ASSIGNMENT','EXAM')),
          priority VARCHAR(6) NOT NULL CHECK(priority IN ('LOW','MEDIUM','HIGH')),
          scheduled_date VARCHAR(10) NOT NULL, start_time VARCHAR(5) NOT NULL,
          duration INT NOT NULL CHECK(duration BETWEEN 15 AND 480), deadline VARCHAR(10),
          adaptive BOOLEAN NOT NULL, status VARCHAR(12) NOT NULL CHECK(status IN ('SCHEDULED','COMPLETED')),
          completed_date VARCHAR(10), created_by VARCHAR(36) NOT NULL REFERENCES users(id),
          created_at VARCHAR(30) NOT NULL, version INT NOT NULL DEFAULT 1,
          FOREIGN KEY(subject_id, student_id) REFERENCES subjects(id, student_id),
          CHECK((status='COMPLETED' AND completed_date IS NOT NULL) OR (status='SCHEDULED' AND completed_date IS NULL))
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS notes (
          id VARCHAR(36) PRIMARY KEY, student_id VARCHAR(36) NOT NULL REFERENCES users(id),
          author_id VARCHAR(36) NOT NULL REFERENCES users(id), body VARCHAR(3000) NOT NULL,
          shared BOOLEAN NOT NULL, created_at VARCHAR(30) NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS audit_events (
          id VARCHAR(36) PRIMARY KEY, actor_id VARCHAR(36) REFERENCES users(id),
          action VARCHAR(80) NOT NULL, target_id VARCHAR(36), created_at VARCHAR(30) NOT NULL
        )
        """,
        "CREATE INDEX IF NOT EXISTS tasks_student_date ON tasks(student_id, scheduled_date)",
        "CREATE INDEX IF NOT EXISTS users_mentor ON users(mentor_id)",
        "CREATE INDEX IF NOT EXISTS sessions_user ON sessions(user_id)",
        "CREATE INDEX IF NOT EXISTS notes_student ON notes(student_id)"
    };
}
