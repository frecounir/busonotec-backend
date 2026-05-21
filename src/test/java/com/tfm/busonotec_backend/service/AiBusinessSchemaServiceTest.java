package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.dto.AiBusinessEntityDefinition;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaRequest;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaResponse;
import com.tfm.busonotec_backend.dto.AiEntityFieldDefinition;
import com.tfm.busonotec_backend.support.InMemoryBusinessEntityRepository;
import com.tfm.busonotec_backend.support.InMemoryEntityFieldRepository;
import com.tfm.busonotec_backend.support.RecordingDynamicSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiBusinessSchemaServiceTest {
  private StubGenerativeAgentClient agentClient;
  private InMemoryBusinessEntityRepository entityRepository;
  private InMemoryEntityFieldRepository fieldRepository;
  private RecordingDynamicSchemaService schemaService;
  private AiBusinessSchemaService service;

  @BeforeEach
  void setUp() {
    agentClient = new StubGenerativeAgentClient(validPlan());
    entityRepository = new InMemoryBusinessEntityRepository();
    fieldRepository = new InMemoryEntityFieldRepository();
    schemaService = new RecordingDynamicSchemaService();

    BusinessEntityService businessEntityService = new BusinessEntityService(entityRepository, fieldRepository, schemaService);
    EntityFieldService entityFieldService = new EntityFieldService(fieldRepository, entityRepository, schemaService);
    service = new AiBusinessSchemaService(agentClient, businessEntityService, entityFieldService);
  }

  @Test
  void createPlanFromPromptReturnsGeneratedPlanWithoutCreatingAnything() {
    AiBusinessSchemaPlan plan = service.createPlanFromPrompt(new AiBusinessSchemaRequest("Crea esquema de estudiantes"));

    assertThat(agentClient.prompts()).containsExactly("Crea esquema de estudiantes");
    assertThat(plan.businessEntities()).singleElement().satisfies(entity -> {
      assertThat(entity.name()).isEqualTo("Estudiantes");
      assertThat(entity.fields()).extracting("name").containsExactly("puntaje", "activo");
    });
    assertThat(entityRepository.hasNoSavedEntities()).isTrue();
    assertThat(fieldRepository.hasNoSavedFields()).isTrue();
    assertThat(schemaService.hasNoStatements()).isTrue();
    assertThat(schemaService.hasNoAddedColumns()).isTrue();
  }

  @Test
  void createPlanFromPromptRemovesValidationRulesThatDoNotApplyToFieldType() {
    agentClient.plan = new AiBusinessSchemaPlan(List.of(new AiBusinessEntityDefinition(
        "Estudiantes",
        "Registros de estudiantes",
        List.of(
            new AiEntityFieldDefinition("puntaje", "number", true, 3, 120, BigDecimal.ZERO, BigDecimal.valueOf(100), null, null),
            new AiEntityFieldDefinition("activo", "boolean", false, 1, null, BigDecimal.ZERO, null, null, null),
            new AiEntityFieldDefinition("nombre", "string", true, 2, 80, BigDecimal.ZERO, null, null, null)
        )
    )));

    AiBusinessSchemaPlan plan = service.createPlanFromPrompt(new AiBusinessSchemaRequest("Crea esquema de estudiantes"));

    assertThat(plan.businessEntities()).singleElement().satisfies(entity -> {
      assertThat(entity.fields()).element(0).satisfies(field -> {
        assertThat(field.type()).isEqualTo("number");
        assertThat(field.minLength()).isNull();
        assertThat(field.maxLength()).isNull();
        assertThat(field.minValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(field.maxValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
      });
      assertThat(entity.fields()).element(1).satisfies(field -> {
        assertThat(field.type()).isEqualTo("boolean");
        assertThat(field.minLength()).isNull();
        assertThat(field.minValue()).isNull();
      });
      assertThat(entity.fields()).element(2).satisfies(field -> {
        assertThat(field.type()).isEqualTo("string");
        assertThat(field.minLength()).isEqualTo(2);
        assertThat(field.maxLength()).isEqualTo(80);
        assertThat(field.minValue()).isNull();
      });
    });
    assertThat(entityRepository.hasNoSavedEntities()).isTrue();
  }

  @Test
  void executePlanCreatesEntitiesAndFieldsFromApprovedPlan() {
    AiBusinessSchemaResponse response = service.executePlan(validPlan());

    assertThat(agentClient.prompts()).isEmpty();
    assertThat(response.createdBusinessEntities()).singleElement().satisfies(created -> {
      assertThat(created.businessEntity().getName()).isEqualTo("Estudiantes");
      assertThat(created.fields()).extracting("name").containsExactly("puntaje", "activo");
      assertThat(created.fields()).first().satisfies(field -> {
        assertThat(field.isRequired()).isTrue();
        assertThat(field.getMinValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(field.getMaxValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
      });
    });
    assertThat(entityRepository.savedEntities()).singleElement()
        .satisfies(entity -> assertThat(entity.getName()).isEqualTo("Estudiantes"));
    assertThat(fieldRepository.savedFields())
        .extracting("name")
        .containsExactly("puntaje", "activo");
    assertThat(fieldRepository.savedFields()).first().satisfies(field -> {
      assertThat(field.isRequired()).isTrue();
      assertThat(field.getMinValue()).isEqualByComparingTo(BigDecimal.ZERO);
      assertThat(field.getMaxValue()).isEqualByComparingTo(BigDecimal.valueOf(100));
    });
    assertThat(schemaService.statementFor("Estudiantes")).contains("CREATE TABLE IF NOT EXISTS \"estudiantes\" (id UUID PRIMARY KEY)");
    assertThat(schemaService.addedColumns())
        .extracting(RecordingDynamicSchemaService.AddedColumn::fieldName)
        .containsExactly("puntaje", "activo");
  }

  @Test
  void createPlanFromPromptRejectsBlankPromptBeforeCallingAgent() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.createPlanFromPrompt(new AiBusinessSchemaRequest(" ")));

    assertThat(exception).hasMessage("Prompt must be provided");
    assertThat(agentClient.prompts()).isEmpty();
    assertThat(entityRepository.hasNoSavedEntities()).isTrue();
  }

  @Test
  void createPlanFromPromptRejectsMissingEntitiesInAiPlan() {
    agentClient.plan = new AiBusinessSchemaPlan(List.of());

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.createPlanFromPrompt(new AiBusinessSchemaRequest("Crea esquema")));

    assertThat(exception).hasMessage("AI response must include at least one business entity");
    assertThat(entityRepository.hasNoSavedEntities()).isTrue();
  }

  @Test
  void executePlanRejectsInvalidEntityOrFieldDefinitionsBeforeCreatingAnything() {
    assertInvalidPlan(
        new AiBusinessSchemaPlan(List.of(new AiBusinessEntityDefinition("1Estudiantes", "Invalido", List.of()))),
        "Invalid AI response entity name: 1Estudiantes"
    );
    assertInvalidPlan(
        new AiBusinessSchemaPlan(List.of(new AiBusinessEntityDefinition("Estudiantes", "Registros de estudiantes", List.of(
            new AiEntityFieldDefinition("id", "string")
        )))),
        "AI response field name 'id' is reserved"
    );
    assertInvalidPlan(
        new AiBusinessSchemaPlan(List.of(new AiBusinessEntityDefinition("Estudiantes", "Registros de estudiantes", List.of(
            new AiEntityFieldDefinition("puntaje", "currency")
        )))),
        "AI response contains unsupported field type: currency"
    );
    assertInvalidPlan(
        new AiBusinessSchemaPlan(List.of(new AiBusinessEntityDefinition("Estudiantes", "Registros de estudiantes", List.of(
            new AiEntityFieldDefinition("puntaje", "number", true, null, null, BigDecimal.TEN, BigDecimal.ONE, null, null)
        )))),
        "AI response minimum value must be less than or equal to maximum value"
    );
  }

  @Test
  void executePlanRejectsDuplicatedGeneratedNamesBeforeCreatingAnything() {
    assertInvalidPlan(
        new AiBusinessSchemaPlan(List.of(
            new AiBusinessEntityDefinition("Estudiantes", "Registros de estudiantes", List.of()),
            new AiBusinessEntityDefinition("estudiantes", "Registros duplicados", List.of())
        )),
        "AI response contains duplicated entity: estudiantes"
    );
    assertInvalidPlan(
        new AiBusinessSchemaPlan(List.of(new AiBusinessEntityDefinition("Estudiantes", "Registros de estudiantes", List.of(
            new AiEntityFieldDefinition("puntaje", "number"),
            new AiEntityFieldDefinition("PUNTAJE", "number")
        )))),
        "AI response contains duplicated field for entity Estudiantes: PUNTAJE"
    );
  }

  private void assertInvalidPlan(AiBusinessSchemaPlan plan, String expectedMessage) {
    agentClient.plan = plan;

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.executePlan(plan));

    assertThat(exception).hasMessage(expectedMessage);
    assertThat(entityRepository.hasNoSavedEntities()).isTrue();
    assertThat(fieldRepository.hasNoSavedFields()).isTrue();
  }

  private AiBusinessSchemaPlan validPlan() {
    return new AiBusinessSchemaPlan(List.of(
        new AiBusinessEntityDefinition("Estudiantes", "Registros de estudiantes", List.of(
            new AiEntityFieldDefinition("puntaje", "number", true, null, null, BigDecimal.ZERO, BigDecimal.valueOf(100), null, null),
            new AiEntityFieldDefinition("activo", "boolean")
        ))
    ));
  }

  private static final class StubGenerativeAgentClient implements GenerativeAgentClient {
    private final List<String> prompts = new ArrayList<>();
    private AiBusinessSchemaPlan plan;

    private StubGenerativeAgentClient(AiBusinessSchemaPlan plan) {
      this.plan = plan;
    }

    @Override
    public AiBusinessSchemaPlan generateBusinessSchema(String prompt) {
      prompts.add(prompt);
      return plan;
    }

    private List<String> prompts() {
      return List.copyOf(prompts);
    }
  }
}
