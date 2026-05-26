package com.tfm.busonotec_backend.repository;

import com.tfm.busonotec_backend.domain.EntityField;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EntityFieldRepository {
  private final JdbcTemplate jdbc;

  public EntityFieldRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public void save(EntityField f) {
    String sql = """
        INSERT INTO entity_fields(
          id, business_entity_id, name, type, is_required,
          min_length, max_length, min_value, max_value, min_date, max_date,
          relationship_type, referenced_business_entity_id
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    jdbc.update(
        sql,
        f.getId(),
        f.getBusinessEntityId(),
        f.getName(),
        f.getType(),
        f.isRequired(),
        f.getMinLength(),
        f.getMaxLength(),
        f.getMinValue(),
        f.getMaxValue(),
        f.getMinDate(),
        f.getMaxDate(),
        f.getRelationshipType(),
        f.getReferencedBusinessEntityId()
    );
  }

  public List<EntityField> findByBusinessEntityId(UUID businessEntityId) {
    String sql = """
        SELECT id, business_entity_id, name, type, is_required,
               min_length, max_length, min_value, max_value, min_date, max_date,
               relationship_type, referenced_business_entity_id
        FROM entity_fields
        WHERE business_entity_id = ?
        ORDER BY name
        """;
    return jdbc.query(sql, mapper(), businessEntityId);
  }

  public Optional<EntityField> findById(UUID id) {
    String sql = """
        SELECT id, business_entity_id, name, type, is_required,
               min_length, max_length, min_value, max_value, min_date, max_date,
               relationship_type, referenced_business_entity_id
        FROM entity_fields
        WHERE id = ?
        """;
    List<EntityField> fields = jdbc.query(sql, mapper(), id);
    return fields.isEmpty() ? Optional.empty() : Optional.of(fields.get(0));
  }

  public boolean existsByNameForEntity(UUID businessEntityId, String name) {
    String sql = "SELECT COUNT(1) FROM entity_fields WHERE business_entity_id = ? AND name = ?";
    Integer count = jdbc.queryForObject(sql, Integer.class, businessEntityId, name);
    return count != null && count > 0;
  }

  public boolean existsByReferencedBusinessEntityId(UUID referencedBusinessEntityId) {
    String sql = "SELECT COUNT(1) FROM entity_fields WHERE referenced_business_entity_id = ?";
    Integer count = jdbc.queryForObject(sql, Integer.class, referencedBusinessEntityId);
    return count != null && count > 0;
  }

  public List<EntityField> findByReferencedBusinessEntityId(UUID referencedBusinessEntityId) {
    String sql = """
        SELECT id, business_entity_id, name, type, is_required,
               min_length, max_length, min_value, max_value, min_date, max_date,
               relationship_type, referenced_business_entity_id
        FROM entity_fields
        WHERE referenced_business_entity_id = ?
        ORDER BY business_entity_id, name
        """;
    return jdbc.query(sql, mapper(), referencedBusinessEntityId);
  }

  public int deleteByBusinessEntityId(UUID businessEntityId) {
    String sql = "DELETE FROM entity_fields WHERE business_entity_id = ?";
    return jdbc.update(sql, businessEntityId);
  }

  public boolean deleteById(UUID id) {
    String sql = "DELETE FROM entity_fields WHERE id = ?";
    return jdbc.update(sql, id) > 0;
  }

  private RowMapper<EntityField> mapper() {
    return new RowMapper<>() {
      @Override
      public EntityField mapRow(ResultSet rs, int rowNum) throws SQLException {
        Date minDate = rs.getDate("min_date");
        Date maxDate = rs.getDate("max_date");
        String referencedBusinessEntityId = rs.getString("referenced_business_entity_id");
        return new EntityField(
            UUID.fromString(rs.getString("id")),
            rs.getString("name"),
            rs.getString("type"),
            UUID.fromString(rs.getString("business_entity_id")),
            null,
            rs.getBoolean("is_required"),
            (Integer) rs.getObject("min_length"),
            (Integer) rs.getObject("max_length"),
            rs.getBigDecimal("min_value"),
            rs.getBigDecimal("max_value"),
            minDate == null ? null : minDate.toLocalDate(),
            maxDate == null ? null : maxDate.toLocalDate(),
            rs.getString("relationship_type"),
            referencedBusinessEntityId == null ? null : UUID.fromString(referencedBusinessEntityId)
        );
      }
    };
  }
}
