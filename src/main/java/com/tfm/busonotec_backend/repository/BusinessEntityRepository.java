package com.tfm.busonotec_backend.repository;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BusinessEntityRepository {
  private static final Logger log = LoggerFactory.getLogger(BusinessEntityRepository.class);
  private final JdbcTemplate jdbc;

  public BusinessEntityRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  @PostConstruct
  public void ensureTable() {
    String sql = "CREATE TABLE IF NOT EXISTS business_entities (id UUID PRIMARY KEY, name TEXT UNIQUE NOT NULL, description TEXT)";
    jdbc.execute(sql);
    log.info("Ensured metadata table business_entities exists");
  }

  public void save(BusinessEntity e) {
    String sql = "INSERT INTO business_entities(id, name, description) VALUES (?, ?, ?)";
    jdbc.update(sql, e.getId(), e.getName(), e.getDescription());
  }

  public List<BusinessEntity> findAll() {
    String sql = "SELECT id, name, description FROM business_entities ORDER BY name";
    return jdbc.query(sql, new RowMapper<BusinessEntity>() {
      @Override
      public BusinessEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new BusinessEntity(UUID.fromString(rs.getString("id")), rs.getString("name"), rs.getString("description"));
      }
    });
  }

  public Optional<BusinessEntity> findById(UUID id) {
    String sql = "SELECT id, name, description FROM business_entities WHERE id = ?";
    List<BusinessEntity> res = jdbc.query(sql, (rs, rn) -> new BusinessEntity(UUID.fromString(rs.getString("id")), rs.getString("name"), rs.getString("description")), id);
    return res.isEmpty() ? Optional.empty() : Optional.of(res.get(0));
  }

  public Optional<BusinessEntity> findByName(String name) {
    String sql = "SELECT id, name, description FROM business_entities WHERE name = ?";
    List<BusinessEntity> res = jdbc.query(sql, (rs, rn) -> new BusinessEntity(UUID.fromString(rs.getString("id")), rs.getString("name"), rs.getString("description")), name);
    return res.isEmpty() ? Optional.empty() : Optional.of(res.get(0));
  }

  public boolean existsByName(String name) {
    return findByName(name).isPresent();
  }
}
