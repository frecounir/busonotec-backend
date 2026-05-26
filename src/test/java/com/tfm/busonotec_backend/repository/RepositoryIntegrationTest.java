package com.tfm.busonotec_backend.repository;

import com.tfm.busonotec_backend.config.DatabaseSeeder;
import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.tfm.busonotec_backend.support.H2TestDatabase.columnExists;
import static com.tfm.busonotec_backend.support.H2TestDatabase.newJdbcTemplate;
import static com.tfm.busonotec_backend.support.H2TestDatabase.tableExists;
import static com.tfm.busonotec_backend.support.TestFixtures.businessEntity;
import static com.tfm.busonotec_backend.support.TestFixtures.entityField;
import static com.tfm.busonotec_backend.support.TestFixtures.relationshipField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RepositoryIntegrationTest {
  private JdbcTemplate jdbc;
  private BusinessEntityRepository businessEntityRepository;
  private EntityFieldRepository entityFieldRepository;
  private BusinessRecordRepository businessRecordRepository;

  @BeforeEach
  void setUp() {
    jdbc = newJdbcTemplate("repository");
    new DatabaseSeeder(jdbc).seed();
    businessEntityRepository = new BusinessEntityRepository(jdbc);
    entityFieldRepository = new EntityFieldRepository(jdbc);
    businessRecordRepository = new BusinessRecordRepository(jdbc);
  }

  @Test
  void seederCreatesRequiredMetadataTables() {
    assertThat(tableExists(jdbc, "business_entities")).isTrue();
    assertThat(tableExists(jdbc, "entity_fields")).isTrue();
    assertThat(columnExists(jdbc, "entity_fields", "relationship_type")).isTrue();
    assertThat(columnExists(jdbc, "entity_fields", "referenced_business_entity_id")).isTrue();
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

    assertThat(businessEntityRepository.deleteById(beta.getId())).isTrue();
    assertThat(businessEntityRepository.findById(beta.getId())).isEmpty();
    assertThat(businessEntityRepository.existsByName("Beta")).isFalse();
    assertThat(businessEntityRepository.deleteById(UUID.randomUUID())).isFalse();
  }

  @Test
  void entityFieldRepositorySavesFindsAndSortsFieldsForEntity() {
    UUID studentsId = UUID.randomUUID();
    UUID activitiesId = UUID.randomUUID();
    businessEntityRepository.save(businessEntity(studentsId, "Students", "Student records"));
    businessEntityRepository.save(businessEntity(activitiesId, "Activities", "Activity records"));

    EntityField beta = entityField(UUID.randomUUID(), studentsId, "beta", "boolean");
    EntityField alpha = entityField(
        UUID.randomUUID(),
        studentsId,
        "alpha",
        "string",
        true,
        3,
        120,
        null,
        null,
        null,
        null
    );
    EntityField unrelated = entityField(UUID.randomUUID(), activitiesId, "zeta", "number");
    saveFields(beta, alpha, unrelated);

    List<EntityField> fields = entityFieldRepository.findByBusinessEntityId(studentsId);

    assertThat(fields).extracting(EntityField::getName).containsExactly("alpha", "beta");
    assertThat(fields).first().satisfies(field -> {
      assertThat(field.getId()).isEqualTo(alpha.getId());
      assertThat(field.getBusinessEntityId()).isEqualTo(studentsId);
      assertThat(field.isRequired()).isTrue();
      assertThat(field.getMinLength()).isEqualTo(3);
      assertThat(field.getMaxLength()).isEqualTo(120);
    });
    assertThat(entityFieldRepository.existsByNameForEntity(studentsId, "alpha")).isTrue();
    assertThat(entityFieldRepository.existsByNameForEntity(studentsId, "missing")).isFalse();
    assertThat(entityFieldRepository.existsByNameForEntity(UUID.randomUUID(), "alpha")).isFalse();
    assertThat(entityFieldRepository.findById(alpha.getId()))
        .hasValueSatisfying(field -> assertThat(field.getName()).isEqualTo("alpha"));
    assertThat(entityFieldRepository.findById(UUID.randomUUID())).isEmpty();
    assertThat(entityFieldRepository.deleteById(alpha.getId())).isTrue();
    assertThat(entityFieldRepository.findById(alpha.getId())).isEmpty();
    assertThat(entityFieldRepository.existsByNameForEntity(studentsId, "alpha")).isFalse();
    assertThat(entityFieldRepository.deleteById(UUID.randomUUID())).isFalse();

    assertThat(entityFieldRepository.deleteByBusinessEntityId(studentsId)).isEqualTo(1);
    assertThat(entityFieldRepository.findByBusinessEntityId(studentsId)).isEmpty();
    assertThat(entityFieldRepository.findByBusinessEntityId(activitiesId)).singleElement()
        .satisfies(field -> assertThat(field.getName()).isEqualTo("zeta"));
  }

  @Test
  void entityFieldRepositoryPersistsRelationshipMetadata() {
    UUID studentsId = UUID.randomUUID();
    UUID activitiesId = UUID.randomUUID();
    businessEntityRepository.save(businessEntity(studentsId, "Students", "Student records"));
    businessEntityRepository.save(businessEntity(activitiesId, "Activities", "Activity records"));

    EntityField relationship = relationshipField(
        UUID.randomUUID(),
        studentsId,
        "activityId",
        "many_to_one",
        activitiesId
    );
    entityFieldRepository.save(relationship);

    assertThat(entityFieldRepository.findById(relationship.getId()))
        .hasValueSatisfying(field -> {
          assertThat(field.getType()).isEqualTo("relationship");
          assertThat(field.getRelationshipType()).isEqualTo("many_to_one");
          assertThat(field.getReferencedBusinessEntityId()).isEqualTo(activitiesId);
        });
    assertThat(entityFieldRepository.findByReferencedBusinessEntityId(activitiesId))
        .extracting(EntityField::getId)
        .containsExactly(relationship.getId());
    assertThat(entityFieldRepository.existsByReferencedBusinessEntityId(activitiesId)).isTrue();
    assertThat(entityFieldRepository.existsByReferencedBusinessEntityId(UUID.randomUUID())).isFalse();
  }

  @Test
  void businessRecordRepositoryReadsRowsFromPhysicalEntityTable() {
    UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    jdbc.execute("CREATE TABLE \"students\" (id UUID PRIMARY KEY, name VARCHAR(255), score NUMERIC)");
    jdbc.update("INSERT INTO \"students\" (id, name, score) VALUES (?, ?, ?)", secondId, "Beta", 80);
    jdbc.update("INSERT INTO \"students\" (id, name, score) VALUES (?, ?, ?)", firstId, "Alpha", 95);

    List<Map<String, Object>> records = businessRecordRepository.findAllByEntityName("Students");

    assertThat(records).hasSize(2);
    assertThat(records).extracting(record -> record.get("id")).containsExactly(firstId, secondId);
    assertThat(records).extracting(record -> record.get("name")).containsExactly("Alpha", "Beta");
    assertThat(records).extracting(record -> ((Number) record.get("score")).intValue()).containsExactly(95, 80);
  }

  @Test
  void businessRecordRepositoryCreatesRowsInPhysicalEntityTable() {
    UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000010");
    jdbc.execute("CREATE TABLE \"students\" (id UUID PRIMARY KEY, name VARCHAR(255), score NUMERIC)");

    Map<String, Object> created = businessRecordRepository.create(
        "Students",
        recordId,
        Map.of("name", "Ana", "score", 95)
    );

    assertThat(created).containsEntry("id", recordId)
        .containsEntry("name", "Ana")
        .containsEntry("score", 95);
    assertThat(businessRecordRepository.findAllByEntityName("Students")).singleElement().satisfies(record -> {
      assertThat(record.get("id")).isEqualTo(recordId);
      assertThat(record.get("name")).isEqualTo("Ana");
      assertThat(((Number) record.get("score")).intValue()).isEqualTo(95);
    });
  }

  @Test
  void businessRecordRepositoryCreatesRowsWithoutDynamicValues() {
    UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000011");
    jdbc.execute("CREATE TABLE \"students\" (id UUID PRIMARY KEY)");

    Map<String, Object> created = businessRecordRepository.create("Students", recordId, null);

    assertThat(created).containsOnlyKeys("id");
    assertThat(businessRecordRepository.findAllByEntityName("Students")).singleElement()
        .satisfies(record -> assertThat(record.get("id")).isEqualTo(recordId));
  }

  @Test
  void businessRecordRepositoryUpdatesAndFindsRowsInPhysicalEntityTable() {
    UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000020");
    UUID missingRecordId = UUID.fromString("00000000-0000-0000-0000-000000000021");
    jdbc.execute("CREATE TABLE \"students\" (id UUID PRIMARY KEY, name VARCHAR(255), score NUMERIC)");
    businessRecordRepository.create("Students", recordId, Map.of("name", "Ana", "score", 95));

    boolean updated = businessRecordRepository.update("Students", recordId, Map.of("score", 100));

    assertThat(updated).isTrue();
    assertThat(businessRecordRepository.findByEntityNameAndId("Students", recordId)).satisfies(record -> {
      assertThat(record.get("id")).isEqualTo(recordId);
      assertThat(record.get("name")).isEqualTo("Ana");
      assertThat(((Number) record.get("score")).intValue()).isEqualTo(100);
    });
    assertThat(businessRecordRepository.update("Students", missingRecordId, Map.of("score", 70))).isFalse();
  }

  @Test
  void businessRecordRepositoryDeletesRowsFromPhysicalEntityTable() {
    UUID recordId = UUID.fromString("00000000-0000-0000-0000-000000000030");
    UUID missingRecordId = UUID.fromString("00000000-0000-0000-0000-000000000031");
    jdbc.execute("CREATE TABLE \"students\" (id UUID PRIMARY KEY, name VARCHAR(255))");
    businessRecordRepository.create("Students", recordId, Map.of("name", "Ana"));

    boolean deleted = businessRecordRepository.delete("Students", recordId);

    assertThat(deleted).isTrue();
    assertThat(businessRecordRepository.findAllByEntityName("Students")).isEmpty();
    assertThat(businessRecordRepository.delete("Students", missingRecordId)).isFalse();
  }

  @Test
  void businessRecordRepositoryRejectsInvalidEntityNames() {
    assertThatThrownBy(() -> businessRecordRepository.findAllByEntityName(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Entity name must be provided");
    assertThatThrownBy(() -> businessRecordRepository.findAllByEntityName(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Entity name must be provided");
    assertThatThrownBy(() -> businessRecordRepository.findAllByEntityName("student-name"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid entity name: student-name");
    assertThatThrownBy(() -> businessRecordRepository.create("Students", UUID.randomUUID(), Map.of("bad-name", 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid column name: bad-name");
    assertThatThrownBy(() -> businessRecordRepository.create("Students", UUID.randomUUID(), Map.of("", 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Column name must be provided");
    assertThatThrownBy(() -> businessRecordRepository.update("Students", UUID.randomUUID(), Map.of("bad-name", 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid column name: bad-name");
    assertThatThrownBy(() -> businessRecordRepository.update("Students", UUID.randomUUID(), Map.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Record values must be provided");
  }

  private void saveFields(EntityField... fields) {
    for (EntityField field : fields) {
      entityFieldRepository.save(field);
    }
  }
}
