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
          type TEXT NOT NULL,
          is_required BOOLEAN NOT NULL DEFAULT FALSE,
          min_length INTEGER,
          max_length INTEGER,
          min_value NUMERIC,
          max_value NUMERIC,
          min_date DATE,
          max_date DATE
        )
        """);

    jdbc.execute("ALTER TABLE entity_fields ADD COLUMN IF NOT EXISTS is_required BOOLEAN NOT NULL DEFAULT FALSE");
    jdbc.execute("ALTER TABLE entity_fields ADD COLUMN IF NOT EXISTS min_length INTEGER");
    jdbc.execute("ALTER TABLE entity_fields ADD COLUMN IF NOT EXISTS max_length INTEGER");
    jdbc.execute("ALTER TABLE entity_fields ADD COLUMN IF NOT EXISTS min_value NUMERIC");
    jdbc.execute("ALTER TABLE entity_fields ADD COLUMN IF NOT EXISTS max_value NUMERIC");
    jdbc.execute("ALTER TABLE entity_fields ADD COLUMN IF NOT EXISTS min_date DATE");
    jdbc.execute("ALTER TABLE entity_fields ADD COLUMN IF NOT EXISTS max_date DATE");

    log.info("Ensured base metadata tables exist");
  }
}
