package com.tfm.busonotec_backend.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/*
 Execute DDL safely. Keeps a registry of created entities.
*/
@Service
public class DynamicSchemaService {
  private final JdbcTemplate jdbc;
  private final Set<String> createdEntities = ConcurrentHashMap.newKeySet();

  public DynamicSchemaService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public void executeStatements(List<String> sqlStatements) {
    for (String sql : sqlStatements) {
      ensureSafeSql(sql);
      jdbc.execute(sql);
      // naive detection of table name from CREATE TABLE "name"
      String table = extractTableName(sql);
      if (table != null) createdEntities.add(table.toLowerCase());
    }
  }

  public boolean entityExists(String entityName) {
    return createdEntities.contains(entityName.toLowerCase());
  }

  private void ensureSafeSql(String sql) {
    String s = sql.trim().toUpperCase(Locale.ROOT);
    if (!s.startsWith("CREATE TABLE") && !s.startsWith("ALTER TABLE")) {
      throw new IllegalArgumentException("Only CREATE TABLE / ALTER TABLE allowed");
    }
    if (s.contains(";") && s.indexOf(";") != s.length() - 1) {
      throw new IllegalArgumentException("Multiple statements or trailing content not allowed");
    }
    if (s.contains("DROP ") || s.contains("TRUNCATE ") || s.contains("DELETE FROM")) {
      throw new IllegalArgumentException("Dangerous SQL keywords detected");
    }
  }

  private String extractTableName(String sql) {
    String up = sql.toUpperCase(Locale.ROOT);
    int idx = up.indexOf("CREATE TABLE");
    if (idx >= 0) {
      String rest = sql.substring(idx + "CREATE TABLE".length()).trim();
      // rest starts with "table" possibly quoted
      String[] parts = rest.split("\\s+", 2);
      String table = parts[0].replace("\"", "");
      return table;
    }
    return null;
  }
}
