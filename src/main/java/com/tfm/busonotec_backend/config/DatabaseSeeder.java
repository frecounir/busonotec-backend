package com.tfm.busonotec_backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder {
  private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

  private final JdbcTemplate jdbc;

  public DatabaseSeeder(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @PostConstruct
  public void seed() {
    jdbc.execute("""
        CREATE TABLE IF NOT EXISTS business_entities (
          id UUID PRIMARY KEY,
          name TEXT UNIQUE NOT NULL,
          description TEXT
        )
        """);

    jdbc.execute("""
        CREATE TABLE IF NOT EXISTS entity_fields (
          id UUID PRIMARY KEY,
          business_entity_id UUID NOT NULL,
          name TEXT NOT NULL,
          type TEXT NOT NULL
        )
        """);

    log.info("Ensured base metadata tables exist");
  }
}
