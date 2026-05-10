package com.tfm.busonotec_backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Repository
public class BusinessRecordRepository {
  private static final Pattern NAME = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,62}$");

  private final JdbcTemplate jdbc;

  public BusinessRecordRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Map<String, Object>> findAllByEntityName(String entityName) {
    validateEntityName(entityName);
    return jdbc.queryForList("SELECT * FROM " + quote(entityName) + " ORDER BY id");
  }

  public Map<String, Object> create(String entityName, UUID recordId, Map<String, Object> valuesByColumn) {
    validateEntityName(entityName);
    Objects.requireNonNull(recordId, "Record id must not be null");

    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", recordId);
    if (valuesByColumn != null) {
      valuesByColumn.forEach((columnName, value) -> {
        validateColumnName(columnName);
        row.put(columnName.toLowerCase(Locale.ROOT), value);
      });
    }

    String columns = row.keySet().stream()
        .map(this::quote)
        .collect(Collectors.joining(", "));
    String placeholders = row.keySet().stream()
        .map(column -> "?")
        .collect(Collectors.joining(", "));
    String sql = "INSERT INTO " + quote(entityName) + " (" + columns + ") VALUES (" + placeholders + ")";

    jdbc.update(sql, row.values().toArray());
    return row;
  }

  private void validateEntityName(String entityName) {
    if (entityName == null || entityName.isBlank()) {
      throw new IllegalArgumentException("Entity name must be provided");
    }
    if (!NAME.matcher(entityName).matches()) {
      throw new IllegalArgumentException("Invalid entity name: " + entityName);
    }
  }

  private void validateColumnName(String columnName) {
    if (columnName == null || columnName.isBlank()) {
      throw new IllegalArgumentException("Column name must be provided");
    }
    if (!NAME.matcher(columnName).matches()) {
      throw new IllegalArgumentException("Invalid column name: " + columnName);
    }
  }

  private String quote(String identifier) {
    return "\"" + identifier.toLowerCase(Locale.ROOT) + "\"";
  }
}
