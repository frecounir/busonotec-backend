package com.tfm.busonotec_backend.repository;

import com.tfm.busonotec_backend.domain.EntityField;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class EntityFieldRepository {
  private final JdbcTemplate jdbc;

  public EntityFieldRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

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

  public int deleteByBusinessEntityId(UUID businessEntityId) {
    String sql = "DELETE FROM entity_fields WHERE business_entity_id = ?";
    return jdbc.update(sql, businessEntityId);
  }
}
