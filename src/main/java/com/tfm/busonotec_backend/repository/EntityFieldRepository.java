package com.tfm.busonotec_backend.repository;

import com.tfm.busonotec_backend.domain.EntityField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;

@Repository
public class EntityFieldRepository {
  private static final Logger log = LoggerFactory.getLogger(EntityFieldRepository.class);
  private final JdbcTemplate jdbc;

  public EntityFieldRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @PostConstruct
  public void ensureTable() {
    String sql = "CREATE TABLE IF NOT EXISTS entity_fields (id UUID PRIMARY KEY, business_entity_id UUID NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL)";
    jdbc.execute(sql);
    log.info("Ensured metadata table entity_fields exists");
  }

  public void save(EntityField f) {
    String sql = "INSERT INTO entity_fields(id, business_entity_id, name, type) VALUES (?, ?, ?, ?)";
    jdbc.update(sql, f.getId(), f.getBusinessEntityId(), f.getName(), f.getType());
  }

  public List<EntityField> findByBusinessEntityId(UUID businessEntityId) {
    String sql = "SELECT id, business_entity_id, name, type FROM entity_fields WHERE business_entity_id = ? ORDER BY name";
    return jdbc.query(sql, (rs, rn) ->
        new EntityField(
            UUID.fromString(rs.getString("id")),
            rs.getString("name"),
            rs.getString("type"),
            UUID.fromString(rs.getString("business_entity_id")),
            null
        ),
        businessEntityId
    );
  }

  public boolean existsByNameForEntity(UUID businessEntityId, String name) {
    String sql = "SELECT COUNT(1) FROM entity_fields WHERE business_entity_id = ? AND name = ?";
    Integer count = jdbc.queryForObject(sql, Integer.class, businessEntityId, name);
    return count != null && count > 0;
  }
}
