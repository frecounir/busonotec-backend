package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.dto.ColumnResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for exposing database schema metadata.
 * Uses information_schema to read table and column information.
 */
@Service
public class SchemaMetadataService {

    private final JdbcTemplate jdbc;

    public SchemaMetadataService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * List all tables in the public schema.
     */
    public List<String> listTables() {
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        return jdbc.queryForList(sql, String.class);
    }

    /**
     * Get columns (name + data type) for a specific table.
     * Validates table name to prevent SQL injection.
     */
    public List<ColumnResponse> getColumns(String tableName) {
        validateTableName(tableName);
        String sql = "SELECT column_name, data_type FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? ORDER BY ordinal_position";
        return jdbc.query(sql, new Object[]{tableName}, (rs, rowNum) ->
                new ColumnResponse(rs.getString("column_name"), rs.getString("data_type"))
        );
    }

    private void validateTableName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Table name must be provided");
        }
        // Allow only alphanumeric and underscore to prevent injection and unusual names
        if (!name.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name");
        }
        // TODO: consider limiting length or checking against known tables for stricter validation
    }
}
