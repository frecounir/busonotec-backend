package com.tfm.busonotec_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenAiGenerativeAgentClientTest {
  private ObjectMapper objectMapper;
  private StubOpenAiGateway gateway;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().findAndRegisterModules();
    gateway = new StubOpenAiGateway();
  }

  @Test
  void generateBusinessSchemaSendsStructuredOutputRequestAndParsesResponse() throws Exception {
    gateway.response = new OpenAiResponse(200, responseWithContent("""
        {
          "businessEntities": [
            {
              "name": "Estudiantes",
              "description": "Registros de estudiantes",
              "fields": [
                {
                  "name": "puntaje",
                  "type": "number",
                  "required": true,
                  "minLength": null,
                  "maxLength": null,
                  "minValue": 0,
                  "maxValue": 100,
                  "minDate": null,
                  "maxDate": null
                }
              ]
            }
          ]
        }
        """));
    OpenAiGenerativeAgentClient client = client("test-key", "gpt-test");

    AiBusinessSchemaPlan plan = client.generateBusinessSchema("Crea estudiantes");

    assertThat(plan.businessEntities()).singleElement().satisfies(entity -> {
      assertThat(entity.name()).isEqualTo("Estudiantes");
      assertThat(entity.fields()).singleElement().satisfies(field -> {
        assertThat(field.name()).isEqualTo("puntaje");
        assertThat(field.type()).isEqualTo("number");
        assertThat(field.required()).isTrue();
        assertThat(field.minValue()).isEqualByComparingTo("0");
        assertThat(field.maxValue()).isEqualByComparingTo("100");
      });
    });
    JsonNode requestBody = objectMapper.readTree(gateway.requestBody);
    assertThat(gateway.apiKey).isEqualTo("test-key");
    assertThat(gateway.timeout).isEqualTo(Duration.ofSeconds(30));
    assertThat(requestBody.path("model").asText()).isEqualTo("gpt-test");
    assertThat(requestBody.path("input").asText()).isEqualTo("Crea estudiantes");
    assertThat(requestBody.path("instructions").asText()).contains("Siempre responde en espanol");
    assertThat(requestBody.path("instructions").asText()).contains("No traduzcas conceptos del usuario al ingles");
    assertThat(requestBody.path("instructions").asText()).contains("Para cada campo decide si required debe ser true o false");
    assertThat(requestBody.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
    assertThat(requestBody.path("text").path("format").path("strict").asBoolean()).isTrue();
    JsonNode fieldRequiredProperties = requestBody.path("text").path("format").path("schema")
        .path("properties").path("businessEntities")
        .path("items").path("properties").path("fields")
        .path("items").path("required");
    List<String> requiredProperties = new ArrayList<>();
    fieldRequiredProperties.forEach(property -> requiredProperties.add(property.asText()));
    assertThat(requiredProperties)
        .contains("required", "minLength", "maxLength", "minValue", "maxValue", "minDate", "maxDate");
  }

  @Test
  void generateBusinessSchemaParsesTopLevelOutputText() {
    gateway.response = new OpenAiResponse(200, """
        {
          "output_text": "{\\"businessEntities\\":[{\\"name\\":\\"Actividades\\",\\"description\\":\\"Registros de actividades\\",\\"fields\\":[]}]}"
        }
        """);
    OpenAiGenerativeAgentClient client = client("test-key", "gpt-test");

    AiBusinessSchemaPlan plan = client.generateBusinessSchema("Crea actividades");

    assertThat(plan.businessEntities()).singleElement()
        .satisfies(entity -> assertThat(entity.name()).isEqualTo("Actividades"));
  }

  @Test
  void generateBusinessSchemaRejectsMissingConfiguration() {
    OpenAiGenerativeAgentClient missingApiKeyClient = client(" ", "gpt-test");
    OpenAiGenerativeAgentClient missingModelClient = client("test-key", " ");

    assertThat(assertThrows(IllegalStateException.class,
        () -> missingApiKeyClient.generateBusinessSchema("Crea esquema")))
        .hasMessage("OPENAI_API_KEY must be configured to use the AI schema assistant");
    assertThat(assertThrows(IllegalStateException.class,
        () -> missingModelClient.generateBusinessSchema("Crea esquema")))
        .hasMessage("OpenAI model must be configured to use the AI schema assistant");
    assertThat(gateway.requestBody).isNull();
  }

  @Test
  void generateBusinessSchemaPropagatesProviderErrors() {
    gateway.response = new OpenAiResponse(401, "{\"error\":{\"message\":\"bad key\"}}");
    OpenAiGenerativeAgentClient client = client("test-key", "gpt-test");

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> client.generateBusinessSchema("Crea esquema"));

    assertThat(exception).hasMessage("OpenAI request failed with status 401: {\"error\":{\"message\":\"bad key\"}}");
  }

  @Test
  void generateBusinessSchemaWrapsTransportFailures() {
    gateway.failure = new IOException("network unavailable");
    OpenAiGenerativeAgentClient client = client("test-key", "gpt-test");

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> client.generateBusinessSchema("Crea esquema"));

    assertThat(exception).hasMessage("Unable to generate business schema with OpenAI")
        .hasCause(gateway.failure);
  }

  @Test
  void generateBusinessSchemaRejectsResponseWithoutOutputText() {
    gateway.response = new OpenAiResponse(200, "{\"output\":[]}");
    OpenAiGenerativeAgentClient client = client("test-key", "gpt-test");

    IllegalStateException exception = assertThrows(IllegalStateException.class,
        () -> client.generateBusinessSchema("Crea esquema"));

    assertThat(exception).hasMessage("OpenAI response did not include structured output text");
  }

  private OpenAiGenerativeAgentClient client(String apiKey, String model) {
    return new OpenAiGenerativeAgentClient(objectMapper, gateway, apiKey, model, 30);
  }

  private String responseWithContent(String structuredOutput) {
    return """
        {
          "output": [
            {
              "content": [
                {
                  "type": "output_text",
                  "text": %s
                }
              ]
            }
          ]
        }
        """.formatted(toJsonString(structuredOutput));
  }

  private String toJsonString(String value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static final class StubOpenAiGateway implements OpenAiResponsesGateway {
    private String apiKey;
    private Duration timeout;
    private String requestBody;
    private OpenAiResponse response;
    private IOException failure;

    @Override
    public OpenAiResponse createResponse(String apiKey, Duration timeout, String requestBody) throws IOException {
      if (failure != null) {
        throw failure;
      }
      this.apiKey = apiKey;
      this.timeout = timeout;
      this.requestBody = requestBody;
      return response;
    }
  }
}
