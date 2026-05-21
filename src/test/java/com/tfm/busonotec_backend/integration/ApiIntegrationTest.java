package com.tfm.busonotec_backend.integration;

import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import com.tfm.busonotec_backend.dto.AiBusinessEntityDefinition;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;
import com.tfm.busonotec_backend.dto.AiEntityFieldDefinition;
import com.tfm.busonotec_backend.service.GenerativeAgentClient;
import com.tfm.busonotec_backend.support.TestHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.http.HttpResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tfm.busonotec_backend.support.H2TestDatabase.columnExists;
import static com.tfm.busonotec_backend.support.H2TestDatabase.tableExists;
import static com.tfm.busonotec_backend.support.TestFixtures.uniqueEntityName;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {
  private static final Pattern ENTITY_NAME_IN_PROMPT = Pattern.compile("entityName=([a-zA-Z][a-zA-Z0-9_]{0,62})");

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

    EntityFieldResponse field = createEntityField(
        entity,
        "score",
        "number",
        Map.of("required", true, "minValue", 0, "maxValue", 100)
    );
    EntityFieldResponse dateField = createEntityField(
        entity,
        "enrollmentDate",
        "date",
        Map.of("minDate", "2026-01-01", "maxDate", "2026-12-31")
    );

    assertThat(columnExists(jdbc, entityName.toLowerCase(Locale.ROOT), "score")).isTrue();
    assertThat(columnExists(jdbc, entityName.toLowerCase(Locale.ROOT), "enrollmentdate")).isTrue();
    assertThat(field.getBusinessEntityId()).isEqualTo(entity.getId());
    assertThat(field.getName()).isEqualTo("score");
    assertThat(field.getType()).isEqualTo("number");
    assertThat(field.isRequired()).isTrue();
    assertThat(field.getMinValue()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(field.getMaxValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
    assertThat(dateField.getBusinessEntityId()).isEqualTo(entity.getId());
    assertThat(dateField.getName()).isEqualTo("enrollmentDate");
    assertThat(dateField.getType()).isEqualTo("date");
    assertThat(dateField.getMinDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(dateField.getMaxDate()).isEqualTo(LocalDate.of(2026, 12, 31));
    assertThat(fieldsForEntity(entity)).containsExactly("enrollmentDate", "score");

    Map<String, Object> createdRecord = createRecord(entity, 95);

    assertThat(recordsForEntity(entity)).singleElement().satisfies(record -> {
      assertThat(record.get("id")).isEqualTo(createdRecord.get("id"));
      assertThat(((Number) record.get("score")).intValue()).isEqualTo(95);
      assertThat(record.get("enrollmentdate").toString()).isEqualTo("2026-05-19");
    });

    Map<String, Object> updatedRecord = updateRecord(entity, createdRecord.get("id"), 100);

    assertThat(recordsForEntity(entity)).singleElement().satisfies(record -> {
      assertThat(record.get("id")).isEqualTo(updatedRecord.get("id"));
      assertThat(((Number) record.get("score")).intValue()).isEqualTo(100);
    });

    deleteRecord(entity, updatedRecord.get("id"));

    assertThat(recordsForEntity(entity)).isEmpty();

    deleteEntityField(field);

    assertThat(columnExists(jdbc, entityName.toLowerCase(Locale.ROOT), "score")).isFalse();
    assertThat(fieldsForEntity(entity)).containsExactly("enrollmentDate");

    deleteBusinessEntity(entity);

    assertThat(tableExists(jdbc, entityName.toLowerCase(Locale.ROOT))).isFalse();
    assertBusinessEntityDoesNotAppearInList(entityName);
    assertThat(fieldsForEntity(entity)).isEmpty();
  }

  @Test
  void aiBusinessSchemaEndpointsCreatePlanAndExecuteApprovedPlan() throws Exception {
    String entityName = uniqueEntityName("EstudiantesAi");

    HttpResponse<String> planResponse = http.postJson("/api/ai/business-schema/plan", Map.of("prompt", "entityName=" + entityName));

    assertOk(planResponse);
    assertThat(tableExists(jdbc, entityName.toLowerCase(Locale.ROOT))).isFalse();
    assertThat(http.readTextValues(planResponse, "name")).contains(entityName, "puntaje", "activo");

    AiBusinessSchemaPlan plan = http.readBody(planResponse, AiBusinessSchemaPlan.class);
    HttpResponse<String> executeResponse = http.postJson("/api/ai/business-schema/execute", plan);

    assertOk(executeResponse);
    assertThat(tableExists(jdbc, entityName.toLowerCase(Locale.ROOT))).isTrue();
    assertThat(columnExists(jdbc, entityName.toLowerCase(Locale.ROOT), "puntaje")).isTrue();
    assertThat(columnExists(jdbc, entityName.toLowerCase(Locale.ROOT), "activo")).isTrue();
    assertThat(http.readTextValues(executeResponse, "name")).contains(entityName, "puntaje", "activo");
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
        .contains("/api/entity-fields/{id}")
        .contains("/api/business-entities/{businessEntityId}/records")
        .contains("/api/business-entities/{businessEntityId}/records/{recordId}")
        .contains("/api/ai/business-schema/plan")
        .contains("/api/ai/business-schema/execute")
        .doesNotContain("\"/api/entities")
        .doesNotContain("\"/api/fields");
  }

  private BusinessEntityResponse createBusinessEntity(String entityName) throws Exception {
    HttpResponse<String> response = http.postJson("/api/business-entities",
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

  private EntityFieldResponse createEntityField(BusinessEntityResponse entity, String name, String type) throws Exception {
    return createEntityField(entity, name, type, Map.of());
  }

  private EntityFieldResponse createEntityField(
      BusinessEntityResponse entity,
      String name,
      String type,
      Map<String, Object> validationRules
  ) throws Exception {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("businessEntityId", entity.getId());
    payload.put("name", name);
    payload.put("type", type);
    payload.putAll(validationRules);

    HttpResponse<String> response = http.postJson("/api/entity-fields", payload);

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
    HttpResponse<String> response = http.get("/api/business-entities/" + entity.getId() + "/records");

    assertOk(response);
    return http.readRows(response);
  }

  private Map<String, Object> createRecord(BusinessEntityResponse entity, int score) throws Exception {
    HttpResponse<String> response = http.postJson("/api/business-entities/" + entity.getId() + "/records",
        Map.of("score", score, "enrollmentDate", "2026-05-19"));

    assertOk(response);
    Map<String, Object> record = http.readRecord(response);
    assertThat(record.get("id")).isNotNull();
    assertThat(((Number) record.get("score")).intValue()).isEqualTo(score);
    assertThat(record.get("enrollmentdate").toString()).isEqualTo("2026-05-19");
    return record;
  }

  private Map<String, Object> updateRecord(BusinessEntityResponse entity, Object recordId, int score) throws Exception {
    HttpResponse<String> response = http.patchJson(
        "/api/business-entities/" + entity.getId() + "/records/" + recordId,
        Map.of("score", score)
    );

    assertOk(response);
    Map<String, Object> record = http.readRecord(response);
    assertThat(record.get("id")).isEqualTo(recordId);
    assertThat(((Number) record.get("score")).intValue()).isEqualTo(score);
    return record;
  }

  private void deleteRecord(BusinessEntityResponse entity, Object recordId) throws Exception {
    HttpResponse<String> response = http.delete("/api/business-entities/" + entity.getId() + "/records/" + recordId);

    assertThat(response.statusCode())
        .withFailMessage(response.body())
        .isEqualTo(204);
  }

  private void deleteEntityField(EntityFieldResponse field) throws Exception {
    HttpResponse<String> response = http.delete("/api/entity-fields/" + field.getId());

    assertThat(response.statusCode())
        .withFailMessage(response.body())
        .isEqualTo(204);
  }

  private void deleteBusinessEntity(BusinessEntityResponse entity) throws Exception {
    HttpResponse<String> response = http.delete("/api/business-entities/" + entity.getId());

    assertThat(response.statusCode())
        .withFailMessage(response.body())
        .isEqualTo(204);
  }

  private void assertOk(HttpResponse<String> response) {
    assertThat(response.statusCode())
        .withFailMessage(response.body())
        .isEqualTo(200);
  }

  @TestConfiguration
  static class FakeAiConfiguration {
    @Bean
    @Primary
    GenerativeAgentClient fakeGenerativeAgentClient() {
      return prompt -> {
        Matcher matcher = ENTITY_NAME_IN_PROMPT.matcher(prompt);
        String entityName = matcher.find() ? matcher.group(1) : uniqueEntityName("GeneradoAi");
        return new AiBusinessSchemaPlan(List.of(
            new AiBusinessEntityDefinition(entityName, "Generado por IA de prueba", List.of(
                new AiEntityFieldDefinition("puntaje", "number"),
                new AiEntityFieldDefinition("activo", "boolean")
            ))
        ));
      };
    }
  }
}
