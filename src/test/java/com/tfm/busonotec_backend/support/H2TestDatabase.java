package com.tfm.busonotec_backend.support;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

public final class H2TestDatabase {
  private static final String POSTGRESQL_COMPATIBILITY =
      ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1";

  private H2TestDatabase() {
  }

  public static JdbcTemplate newJdbcTemplate(String databasePrefix) {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL("jdbc:h2:mem:" + databasePrefix + "-" + UUID.randomUUID() + POSTGRESQL_COMPATIBILITY);
    dataSource.setUser("sa");
    dataSource.setPassword("");
    return new JdbcTemplate(dataSource);
  }

  public static boolean tableExists(JdbcTemplate jdbc, String tableName) {
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
        Integer.class,
        tableName
    );
    return count != null && count > 0;
  }

  public static boolean columnExists(JdbcTemplate jdbc, String tableName, String columnName) {
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'public' AND table_name = ? AND column_name = ?",
        Integer.class,
        tableName,
        columnName
    );
    return count != null && count > 0;
  }
}
