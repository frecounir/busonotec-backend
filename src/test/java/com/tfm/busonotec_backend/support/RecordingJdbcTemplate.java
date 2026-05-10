package com.tfm.busonotec_backend.support;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

public final class RecordingJdbcTemplate extends JdbcTemplate {
  private final List<String> executedSql = new ArrayList<>();
  private final List<String> queriedTableNames = new ArrayList<>();
  private int tableCount;

  @Override
  public void execute(String sql) {
    executedSql.add(sql);
  }

  @Override
  public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
    queriedTableNames.add((String) args[0]);
    return requiredType.cast(tableCount);
  }

  public void tableExists(boolean exists) {
    tableCount = exists ? 1 : 0;
  }

  public List<String> executedSql() {
    return List.copyOf(executedSql);
  }

  public String lastExecutedSql() {
    return executedSql.get(executedSql.size() - 1);
  }

  public List<String> queriedTableNames() {
    return List.copyOf(queriedTableNames);
  }

  public boolean hasNoExecutedSql() {
    return executedSql.isEmpty();
  }

  public boolean hasNoQueries() {
    return queriedTableNames.isEmpty();
  }
}
