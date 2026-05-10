package com.tfm.busonotec_backend.repository;

import com.tfm.busonotec_backend.config.DatabaseSeeder;
import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static com.tfm.busonotec_backend.support.H2TestDatabase.newJdbcTemplate;
import static com.tfm.busonotec_backend.support.H2TestDatabase.tableExists;
import static com.tfm.busonotec_backend.support.TestFixtures.businessEntity;
import static com.tfm.busonotec_backend.support.TestFixtures.entityField;
import static org.assertj.core.api.Assertions.assertThat;

class RepositoryIntegrationTest {
  private JdbcTemplate jdbc;
  private BusinessEntityRepository businessEntityRepository;
  private EntityFieldRepository entityFieldRepository;

  @BeforeEach
  void setUp() {
    jdbc = newJdbcTemplate("repository");
    new DatabaseSeeder(jdbc).seed();
    businessEntityRepository = new BusinessEntityRepository(jdbc);
    entityFieldRepository = new EntityFieldRepository(jdbc);
  }

  @Test
  void seederCreatesRequiredMetadataTables() {
    assertThat(tableExists(jdbc, "business_entities")).isTrue();
    assertThat(tableExists(jdbc, "entity_fields")).isTrue();
  }

  @Test
  void businessEntityRepositorySavesFindsAndSortsEntities() {
    BusinessEntity beta = businessEntity(UUID.randomUUID(), "Beta", "Second");
    BusinessEntity alpha = businessEntity(UUID.randomUUID(), "Alpha", "First");

    businessEntityRepository.save(beta);
    businessEntityRepository.save(alpha);

    assertThat(businessEntityRepository.findAll())
        .extracting(BusinessEntity::getName)
        .containsExactly("Alpha", "Beta");
    assertThat(businessEntityRepository.findById(beta.getId()))
        .hasValueSatisfying(entity -> assertThat(entity.getName()).isEqualTo("Beta"));
    assertThat(businessEntityRepository.findByName("Alpha"))
        .hasValueSatisfying(entity -> assertThat(entity.getId()).isEqualTo(alpha.getId()));
    assertThat(businessEntityRepository.existsByName("Alpha")).isTrue();
    assertThat(businessEntityRepository.findById(UUID.randomUUID())).isEmpty();
    assertThat(businessEntityRepository.findByName("Missing")).isEmpty();
    assertThat(businessEntityRepository.existsByName("Missing")).isFalse();
  }

  @Test
  void entityFieldRepositorySavesFindsAndSortsFieldsForEntity() {
    UUID studentsId = UUID.randomUUID();
    UUID activitiesId = UUID.randomUUID();
    businessEntityRepository.save(businessEntity(studentsId, "Students", "Student records"));
    businessEntityRepository.save(businessEntity(activitiesId, "Activities", "Activity records"));

    EntityField beta = entityField(UUID.randomUUID(), studentsId, "beta", "boolean");
    EntityField alpha = entityField(UUID.randomUUID(), studentsId, "alpha", "string");
    EntityField unrelated = entityField(UUID.randomUUID(), activitiesId, "zeta", "number");
    saveFields(beta, alpha, unrelated);

    List<EntityField> fields = entityFieldRepository.findByBusinessEntityId(studentsId);

    assertThat(fields).extracting(EntityField::getName).containsExactly("alpha", "beta");
    assertThat(fields).first().satisfies(field -> {
      assertThat(field.getId()).isEqualTo(alpha.getId());
      assertThat(field.getBusinessEntityId()).isEqualTo(studentsId);
    });
    assertThat(entityFieldRepository.existsByNameForEntity(studentsId, "alpha")).isTrue();
    assertThat(entityFieldRepository.existsByNameForEntity(studentsId, "missing")).isFalse();
    assertThat(entityFieldRepository.existsByNameForEntity(UUID.randomUUID(), "alpha")).isFalse();
  }

  private void saveFields(EntityField... fields) {
    for (EntityField field : fields) {
      entityFieldRepository.save(field);
    }
  }
}
