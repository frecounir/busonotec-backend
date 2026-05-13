package com.tfm.busonotec_backend.integration;

import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import com.tfm.busonotec_backend.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.tfm.busonotec_backend.support.H2TestDatabase.columnExists;
import static com.tfm.busonotec_backend.support.H2TestDatabase.tableExists;
import static com.tfm.busonotec_backend.support.TestFixtures.uniqueEntityName;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {
  @LocalServerPort
  private int port;

  @Autowired
  private JdbcTemplate jdbc;

  private TestHttpClient http;

  @BeforeEach
  void setUp() {
    http = new TestHttpClient(port);
  }

  @Test
  void entityAndFieldEndpointsCreateMetadataAndPhysicalColumn() throws Exception {
    String entityName = uniqueEntityName("Students");

    BusinessEntityResponse entity = createBusinessEntity(entityName);

    assertThat(tableExists(jdbc, entityName.toLowerCase(Locale.ROOT))).isTrue();
    assertBusinessEntityCanBeFetched(entity);
    assertBusinessEntityAppearsInList(entityName);

    EntityFieldResponse field = createEntityField(entity);

    assertThat(columnExists(jdbc, entityName.toLowerCase(Locale.ROOT), "score")).isTrue();
    assertThat(field.getBusinessEntityId()).isEqualTo(entity.getId());
    assertThat(field.getName()).isEqualTo("score");
    assertThat(field.getType()).isEqualTo("number");
    assertThat(fieldsForEntity(entity)).containsExactly("score");

    Map<String, Object> createdRecord = createRecord(entity, 95);

    assertThat(recordsForEntity(entity)).singleElement().satisfies(record -> {
      assertThat(record.get("id")).isEqualTo(createdRecord.get("id"));
      assertThat(((Number) record.get("score")).intValue()).isEqualTo(95);
    });

    Map<String, Object> updatedRecord = updateRecord(entity, createdRecord.get("id"), 100);

    assertThat(recordsForEntity(entity)).singleElement().satisfies(record -> {
      assertThat(record.get("id")).isEqualTo(updatedRecord.get("id"));
      assertThat(((Number) record.get("score")).intValue()).isEqualTo(100);
    });

    deleteRecord(entity, updatedRecord.get("id"));

    assertThat(recordsForEntity(entity)).isEmpty();

    deleteBusinessEntity(entity);

    assertThat(tableExists(jdbc, entityName.toLowerCase(Locale.ROOT))).isFalse();
    assertBusinessEntityDoesNotAppearInList(entityName);
    assertThat(fieldsForEntity(entity)).isEmpty();
  }

  @Test
  void corsPreflightAllowsReactOrigins() throws Exception {
    HttpResponse<String> response = http.options("/api/business-entities", Map.of(
        "Origin", "http://localhost:5173",
        "Access-Control-Request-Method", "GET"
    ));

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("access-control-allow-origin"))
        .contains("http://localhost:5173");
    assertThat(response.headers().firstValue("access-control-allow-credentials"))
        .contains("true");
  }

  @Test
  void openApiDocumentationIncludesBusinessEndpoints() throws Exception {
    HttpResponse<String> response = http.get("/v3/api-docs");

    assertOk(response);
    assertThat(response.body())
        .contains("/api/business-entities")
        .contains("/api/business-entities/{id}")
        .contains("/api/entity-fields/{businessEntityId}")
        .contains("/api/business-entities/{businessEntityId}/records")
        .contains("/api/business-entities/{businessEntityId}/records/{recordId}");
  }

  private BusinessEntityResponse createBusinessEntity(String entityName) throws Exception {
    HttpResponse<String> response = http.postJson("/api/entities",
        Map.of("name", entityName, "description", "Student records"));

    assertOk(response);
    BusinessEntityResponse entity = http.readBody(response, BusinessEntityResponse.class);
    assertThat(entity.getId()).isNotNull();
    assertThat(entity.getName()).isEqualTo(entityName);
    assertThat(entity.getDescription()).isEqualTo("Student records");
    return entity;
  }

  private void assertBusinessEntityCanBeFetched(BusinessEntityResponse entity) throws Exception {
    HttpResponse<String> response = http.get("/api/business-entities/" + entity.getId());

    assertOk(response);
    BusinessEntityResponse fetchedEntity = http.readBody(response, BusinessEntityResponse.class);
    assertThat(fetchedEntity.getId()).isEqualTo(entity.getId());
    assertThat(fetchedEntity.getName()).isEqualTo(entity.getName());
  }

  private void assertBusinessEntityAppearsInList(String entityName) throws Exception {
    HttpResponse<String> response = http.get("/api/business-entities");

    assertOk(response);
    assertThat(http.readTextValues(response, "name")).contains(entityName);
  }

  private void assertBusinessEntityDoesNotAppearInList(String entityName) throws Exception {
    HttpResponse<String> response = http.get("/api/business-entities");

    assertOk(response);
    assertThat(http.readTextValues(response, "name")).doesNotContain(entityName);
  }

  private EntityFieldResponse createEntityField(BusinessEntityResponse entity) throws Exception {
    HttpResponse<String> response = http.postJson("/api/fields",
        Map.of("businessEntityId", entity.getId(), "name", "score", "type", "number"));

    assertOk(response);
    EntityFieldResponse field = http.readBody(response, EntityFieldResponse.class);
    assertThat(field.getId()).isNotNull();
    return field;
  }

  private List<String> fieldsForEntity(BusinessEntityResponse entity) throws Exception {
    HttpResponse<String> response = http.get("/api/entity-fields/" + entity.getId());

    assertOk(response);
    return http.readTextValues(response, "name");
  }

  private List<Map<String, Object>> recordsForEntity(BusinessEntityResponse entity) throws Exception {
    HttpResponse<String> response = http.get("/api/entities/" + entity.getId() + "/records");

    assertOk(response);
    return http.readRows(response);
  }

  private Map<String, Object> createRecord(BusinessEntityResponse entity, int score) throws Exception {
    HttpResponse<String> response = http.postJson("/api/entities/" + entity.getId() + "/records", Map.of("score", score));

    assertOk(response);
    Map<String, Object> record = http.readRecord(response);
    assertThat(record.get("id")).isNotNull();
    assertThat(((Number) record.get("score")).intValue()).isEqualTo(score);
    return record;
  }

  private Map<String, Object> updateRecord(BusinessEntityResponse entity, Object recordId, int score) throws Exception {
    HttpResponse<String> response = http.patchJson(
        "/api/entities/" + entity.getId() + "/records/" + recordId,
        Map.of("score", score)
    );

    assertOk(response);
    Map<String, Object> record = http.readRecord(response);
    assertThat(record.get("id")).isEqualTo(recordId);
    assertThat(((Number) record.get("score")).intValue()).isEqualTo(score);
    return record;
  }

  private void deleteRecord(BusinessEntityResponse entity, Object recordId) throws Exception {
    HttpResponse<String> response = http.delete("/api/entities/" + entity.getId() + "/records/" + recordId);

    assertThat(response.statusCode())
        .withFailMessage(response.body())
        .isEqualTo(204);
  }

  private void deleteBusinessEntity(BusinessEntityResponse entity) throws Exception {
    HttpResponse<String> response = http.delete("/api/entities/" + entity.getId());

    assertThat(response.statusCode())
        .withFailMessage(response.body())
        .isEqualTo(204);
  }

  private void assertOk(HttpResponse<String> response) {
    assertThat(response.statusCode())
        .withFailMessage(response.body())
        .isEqualTo(200);
  }
}
